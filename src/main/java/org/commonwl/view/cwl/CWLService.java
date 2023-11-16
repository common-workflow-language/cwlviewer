/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.commonwl.view.cwl;

import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotException;
import org.commonwl.view.docker.DockerService;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.git.GitLicenseException;
import org.commonwl.view.graphviz.ModelDotWriter;
import org.commonwl.view.graphviz.RDFDotWriter;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowNotFoundException;
import org.commonwl.view.workflow.WorkflowOverview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Provides CWL parsing for workflows to gather an overview for display and visualisation */
@Service
public class CWLService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final IRIFactory iriFactory = IRIFactory.iriImplementation();

  // Autowired properties/services
  private final RDFService rdfService;
  private final CWLTool cwlTool;
  private final Map<String, String> licenseVocab;
  private final int singleFileSizeLimit;

  // CWL specific strings
  private final String DOC_GRAPH = "$graph";
  private final String CLASS = "class";
  private final String WORKFLOW = "Workflow";
  private final String COMMANDLINETOOL = "CommandLineTool";
  private final String EXPRESSIONTOOL = "ExpressionTool";
  private final String STEPS = "steps";
  private final String INPUTS = "inputs";
  private final String IN = "in";
  private final String OUTPUTS = "outputs";
  private final String OUT = "out";
  private final String ID = "id";
  private final String TYPE = "type";
  private final String LABEL = "label";
  private final String DEFAULT = "default";
  private final String OUTPUT_SOURCE = "outputSource";
  private final String SOURCE = "source";
  private final String DOC = "doc";
  private final String DESCRIPTION = "description";
  private final String ARRAY = "array";
  private final String ARRAY_ITEMS = "items";
  private final String LOCATION = "location";
  private final String RUN = "run";

  /**
   * Constructor for the Common Workflow Language service
   *
   * @param rdfService A service for handling RDF queries
   * @param cwlTool Handles cwltool integration
   * @param singleFileSizeLimit The file size limit for single files
   */
  @Autowired
  public CWLService(
      RDFService rdfService,
      CWLTool cwlTool,
      Map<String, String> licenseVocab,
      @Value("${singleFileSizeLimit}") int singleFileSizeLimit) {
    this.rdfService = rdfService;
    this.cwlTool = cwlTool;
    this.licenseVocab = licenseVocab;
    this.singleFileSizeLimit = singleFileSizeLimit;
  }

  /**
   * Gets whether a file is packed using schema salad
   *
   * @param workflowFile The file to be parsed
   * @return Whether the file is packed
   */
  public boolean isPacked(File workflowFile) throws IOException {
    if (workflowFile.length() > singleFileSizeLimit) {
      return false;
    }
    String fileContent = readFileToString(workflowFile, StandardCharsets.UTF_8);
    return fileContent.contains("$graph");
  }

  /**
   * Gets a list of workflows from a packed CWL file
   *
   * @param packedFile The packed CWL file
   * @return The list of workflow overviews
   */
  public List<WorkflowOverview> getWorkflowOverviewsFromPacked(File packedFile) throws IOException {
    if (packedFile.length() <= singleFileSizeLimit) {
      List<WorkflowOverview> overviews = new ArrayList<>();

      Map<String, Object> packedJson = yamlPathToJson(packedFile.toPath());

      if (packedJson.containsKey(DOC_GRAPH)) {
        for (Map<String, Object> node : (Iterable<Map<String, Object>>) packedJson.get(DOC_GRAPH)) {
          if (extractProcess(node) == CWLProcess.WORKFLOW) {
            WorkflowOverview overview =
                new WorkflowOverview((String) node.get(ID), extractLabel(node), extractDoc(node));
            overviews.add(overview);
          }
        }
      } else {
        throw new IOException("The file given was not recognised as a packed CWL file");
      }

      return overviews;

    } else {
      throw new IOException(
          "File '"
              + packedFile.getName()
              + "' is over singleFileSizeLimit - "
              + FileUtils.byteCountToDisplaySize(packedFile.length())
              + "/"
              + FileUtils.byteCountToDisplaySize(singleFileSizeLimit));
    }
  }

  /**
   * Gets the Workflow object from internal parsing. Note, the length of the stream is not checked.
   *
   * @param workflowStream The workflow stream to be parsed
   * @param packedWorkflowId The ID of the workflow object if the file is packed. <code>null</code>
   *     means the workflow is not expected to be packed, while "" means the first workflow found is
   *     used, packed or non-packed.
   * @param defaultLabel Label to give workflow if not set
   * @return The constructed workflow object
   */
  public Workflow parseWorkflowNative(
      InputStream workflowStream, String packedWorkflowId, String defaultLabel) throws IOException {
    // Parse file as yaml
    Map<String, Object> cwlFile = yamlStreamToJson(workflowStream);

    // Check packed workflow occurs
    boolean found = false;
    if (packedWorkflowId != null) {
      if (cwlFile.containsKey(DOC_GRAPH)) {
        for (Map<String, Object> node : (Iterable<Map<String, Object>>) cwlFile.get(DOC_GRAPH)) {
          if (extractProcess(node) == CWLProcess.WORKFLOW) {
            String currentId = (String) node.get(ID);
            if (currentId.startsWith("#")) {
              currentId = currentId.substring(1);
            }
            if (packedWorkflowId.isEmpty() || currentId.equals(packedWorkflowId)) {
              cwlFile = node;
              found = true;
              break;
            }
          }
        }
      }
      if (!found && !packedWorkflowId.isEmpty()) throw new WorkflowNotFoundException();
    }
    if (!found && extractProcess(cwlFile) == CWLProcess.WORKFLOW) {
      // Check the current json node is a workflow
      found = true;
    }
    if (!found) {
      throw new WorkflowNotFoundException();
    }

    // Use filename for label if there is no defined one
    String label = extractLabel(cwlFile);
    if (label == null) {
      label = defaultLabel;
    }

    // Construct the rest of the workflow model
    Workflow workflowModel =
        new Workflow(
            label, extractDoc(cwlFile), getInputs(cwlFile), getOutputs(cwlFile), getSteps(cwlFile));

    workflowModel.setCwltoolVersion(cwlTool.getVersion());

    // Generate DOT graph
    StringWriter graphWriter = new StringWriter();
    ModelDotWriter dotWriter = new ModelDotWriter(graphWriter);
    try {
      dotWriter.writeGraph(workflowModel);
      workflowModel.setVisualisationDot(graphWriter.toString());
    } catch (IOException ex) {
      logger.error("Failed to create DOT graph for workflow: " + ex.getMessage());
    }

    return workflowModel;
  }

  /**
   * Gets the Workflow object from internal parsing. The size of the workflow file must be below the
   * configured singleFileSizeLimit in the constructor/spring config.
   *
   * @param workflowFile The workflow file to be parsed
   * @param packedWorkflowId The ID of the workflow object if the file is packed
   * @return The constructed workflow object
   */
  public Workflow parseWorkflowNative(Path workflowFile, String packedWorkflowId)
      throws IOException {

    // Check file size limit before parsing
    long fileSizeBytes = Files.size(workflowFile);
    if (fileSizeBytes <= singleFileSizeLimit) {
      try (InputStream in = Files.newInputStream(workflowFile)) {
        return parseWorkflowNative(
            in, packedWorkflowId, workflowFile.getName(workflowFile.getNameCount() - 1).toString());
      }
    } else {
      throw new IOException(
          "File '"
              + workflowFile.getFileName()
              + "' is over singleFileSizeLimit - "
              + FileUtils.byteCountToDisplaySize(fileSizeBytes)
              + "/"
              + FileUtils.byteCountToDisplaySize(singleFileSizeLimit));
    }
  }

  /**
   * Create a workflow model using cwltool rdf output
   *
   * @param basicModel The basic workflow object created thus far
   * @param workflowFile The workflow file to run cwltool on
   * @return The constructed workflow object
   */
  public Workflow parseWorkflowWithCwltool(Workflow basicModel, Path workflowFile, Path workTree)
      throws CWLValidationException, GitLicenseException {
    GitDetails gitDetails = basicModel.getRetrievedFrom();
    String latestCommit = basicModel.getLastCommit();
    String packedWorkflowID = gitDetails.getPackedId();

    // Get paths to workflow
    String url = basicModel.getIdentifier();
    String workflowFileURI = workflowFile.toAbsolutePath().toUri().toString();
    URI workTreeUri = workTree.toAbsolutePath().toUri();
    String localPath = workflowFileURI;
    String gitPath = gitDetails.getPath();
    if (packedWorkflowID != null) {
      if (packedWorkflowID.charAt(0) != '#') {
        localPath += "#";
        gitPath += "#";
      }
      localPath += packedWorkflowID;
      gitPath += packedWorkflowID;
    }

    // Get RDF representation from cwltool
    if (!rdfService.graphExists(url)) {
      String rdf = cwlTool.getRDF(localPath);
      // Replace /tmp/123123 with permalink base
      // NOTE: We do not just replace workflowFileURI, all referenced files will also
      // get rewritten
      rdf =
          rdf.replace(
              workTreeUri.toString(), "https://w3id.org/cwl/view/git/" + latestCommit + "/");
      // Workaround for common-workflow-language/cwltool#427
      rdf = rdf.replace("<rdfs:>", "<http://www.w3.org/2000/01/rdf-schema#>");

      // Create a workflow model from RDF representation
      Model model = ModelFactory.createDefaultModel();
      model.read(new ByteArrayInputStream(rdf.getBytes()), null, "TURTLE");

      // Store the model
      rdfService.storeModel(url, model);
    }

    // Base workflow details
    String label = FilenameUtils.getName(url);
    String doc = null;
    ResultSet labelAndDoc = rdfService.getLabelAndDoc(url);
    if (labelAndDoc.hasNext()) {
      QuerySolution labelAndDocSoln = labelAndDoc.nextSolution();
      if (labelAndDocSoln.contains("label")) {
        label = labelAndDocSoln.get("label").toString();
      }
      if (labelAndDocSoln.contains("doc")) {
        doc = labelAndDocSoln.get("doc").toString();
      }
    }

    // Inputs
    Map<String, CWLElement> wfInputs = new HashMap<>();
    ResultSet inputs = rdfService.getInputs(url);
    while (inputs.hasNext()) {
      QuerySolution input = inputs.nextSolution();
      String inputName = rdfService.stepNameFromURI(gitPath, input.get("name").toString());

      CWLElement wfInput = new CWLElement();
      if (input.contains("type")) {
        String type;
        if (input.get("type").toString().equals("https://w3id.org/cwl/salad#array")) {
          type = typeURIToString(input.get("items").toString()) + "[]";
        } else {
          type = typeURIToString(input.get("type").toString());
        }
        if (input.contains("null")) {
          type += " (Optional)";
        }
        wfInput.setType(type);
      }
      if (input.contains("format")) {
        String format = input.get("format").toString();
        setFormat(wfInput, format);
      }
      if (input.contains("label")) {
        wfInput.setLabel(input.get("label").toString());
      }
      if (input.contains("doc")) {
        wfInput.setDoc(input.get("doc").toString());
      }
      wfInputs.put(rdfService.labelFromName(inputName), wfInput);
    }

    // Outputs
    Map<String, CWLElement> wfOutputs = new HashMap<>();
    ResultSet outputs = rdfService.getOutputs(url);
    while (outputs.hasNext()) {
      QuerySolution output = outputs.nextSolution();
      CWLElement wfOutput = new CWLElement();

      String outputName = rdfService.stepNameFromURI(gitPath, output.get("name").toString());
      if (output.contains("type")) {
        String type;
        if (output.get("type").toString().equals("https://w3id.org/cwl/salad#array")) {
          type = typeURIToString(output.get("items").toString()) + "[]";
        } else {
          type = typeURIToString(output.get("type").toString());
        }
        if (output.contains("null")) {
          type += " (Optional)";
        }
        wfOutput.setType(type);
      }

      if (output.contains("src")) {
        wfOutput.addSourceID(rdfService.stepNameFromURI(gitPath, output.get("src").toString()));
      }
      if (output.contains("format")) {
        String format = output.get("format").toString();
        setFormat(wfOutput, format);
      }
      if (output.contains("label")) {
        wfOutput.setLabel(output.get("label").toString());
      }
      if (output.contains("doc")) {
        wfOutput.setDoc(output.get("doc").toString());
      }
      wfOutputs.put(rdfService.labelFromName(outputName), wfOutput);
    }

    // Steps
    Map<String, CWLStep> wfSteps = new HashMap<>();
    ResultSet steps = rdfService.getSteps(url);
    while (steps.hasNext()) {
      QuerySolution step = steps.nextSolution();
      String uri = rdfService.stepNameFromURI(gitPath, step.get("step").toString());
      if (wfSteps.containsKey(uri)) {
        // Already got step details, add extra source ID
        if (step.contains("src")) {
          CWLElement src = new CWLElement();
          src.addSourceID(rdfService.stepNameFromURI(gitPath, step.get("src").toString()));
          wfSteps.get(uri).getSources().put(step.get("stepinput").toString(), src);
        } else if (step.contains("default")) {
          CWLElement src = new CWLElement();
          src.setDefaultVal(rdfService.formatDefault(step.get("default").toString()));
          wfSteps.get(uri).getSources().put(step.get("stepinput").toString(), src);
        }
      } else {
        // Add new step
        CWLStep wfStep = new CWLStep();

        IRI workflowPath = iriFactory.construct(url).resolve("./");
        Object runValue = step.get("run").asResource().toString();
        if (String.class.isAssignableFrom(runValue.getClass())) {
          String runPath = (String) runValue;
          wfStep.setRun(workflowPath.relativize(runPath).toString());
          wfStep.setRunType(rdfService.strToRuntype(step.get("runtype").toString()));
        }

        if (step.contains("src")) {
          CWLElement src = new CWLElement();
          src.addSourceID(rdfService.stepNameFromURI(gitPath, step.get("src").toString()));
          Map<String, CWLElement> srcList = new HashMap<>();
          srcList.put(rdfService.stepNameFromURI(gitPath, step.get("stepinput").toString()), src);
          wfStep.setSources(srcList);
        } else if (step.contains("default")) {
          CWLElement src = new CWLElement();
          src.setDefaultVal(rdfService.formatDefault(step.get("default").toString()));
          Map<String, CWLElement> srcList = new HashMap<>();
          srcList.put(rdfService.stepNameFromURI(gitPath, step.get("stepinput").toString()), src);
          wfStep.setSources(srcList);
        }
        if (step.contains("label")) {
          wfStep.setLabel(step.get("label").toString());
        }
        if (step.contains("doc")) {
          wfStep.setDoc(step.get("doc").toString());
        }
        wfSteps.put(rdfService.labelFromName(uri), wfStep);
      }
    }
    // Try to determine license
    ResultSet licenseResult = rdfService.getLicense(url);
    String licenseLink;
    if (licenseResult.hasNext()) {
      licenseLink = normaliseLicenseLink(licenseResult.next().get("license").toString());
    } else {
      // Check for "LICENSE"-like files in root of git repo
      licenseLink = basicModel.getRetrievedFrom().getLicense(workTree);
    }

    // Docker link
    ResultSet dockerResult = rdfService.getDockerLink(url);
    String dockerLink = null;
    if (dockerResult.hasNext()) {
      QuerySolution docker = dockerResult.nextSolution();
      if (docker.contains("pull")) {
        dockerLink = DockerService.getDockerHubURL(docker.get("pull").toString());
      } else {
        dockerLink = "true";
      }
    }

    // Create workflow model
    Workflow workflowModel =
        new Workflow(label, doc, wfInputs, wfOutputs, wfSteps, dockerLink, licenseLink);

    // Generate DOT graph
    StringWriter graphWriter = new StringWriter();
    RDFDotWriter RDFDotWriter = new RDFDotWriter(graphWriter, rdfService, gitPath);
    try {
      RDFDotWriter.writeGraph(url);
      workflowModel.setVisualisationDot(graphWriter.toString());
    } catch (IOException ex) {
      logger.error("Failed to create DOT graph for workflow: " + ex.getMessage());
    }

    return workflowModel;
  }

  /**
   * Get an overview of a workflow
   *
   * @param file A file, potentially a workflow
   * @return A constructed WorkflowOverview of the workflow
   * @throws IOException Any API errors which may have occurred
   */
  public WorkflowOverview getWorkflowOverview(File file) throws IOException {

    // Get the content of this file from Github
    long fileSizeBytes = file.length();

    // Check file size limit before parsing
    if (fileSizeBytes <= singleFileSizeLimit) {

      // Parse file as yaml
      Map<String, Object> cwlFile = yamlPathToJson(file.toPath());

      // If the CWL file is packed there can be multiple workflows in a file
      int packedCount = 0;
      if (cwlFile.containsKey(DOC_GRAPH)) {
        // Packed CWL, find the first subelement which is a workflow and take it
        for (Map<String, Object> node : (Iterable<Map<String, Object>>) cwlFile.get(DOC_GRAPH)) {
          if (extractProcess(node) == CWLProcess.WORKFLOW) {
            cwlFile = node;
            packedCount++;
          }
        }
        if (packedCount > 1) {
          return new WorkflowOverview(
              "/" + file.getName(), "Packed file", "contains " + packedCount + " workflows");
        }
      }

      // Can only make an overview if this is a workflow
      if (extractProcess(cwlFile) == CWLProcess.WORKFLOW) {
        // Use filename for label if there is no defined one
        String label = extractLabel(cwlFile);
        if (label == null) {
          label = file.getName();
        }

        // Return the constructed overview
        return new WorkflowOverview("/" + file.getName(), label, extractDoc(cwlFile));
      } else {
        // Return null if not a workflow file
        return null;
      }
    } else {
      throw new IOException(
          "File '"
              + file.getName()
              + "' is over singleFileSizeLimit - "
              + FileUtils.byteCountToDisplaySize(fileSizeBytes)
              + "/"
              + FileUtils.byteCountToDisplaySize(singleFileSizeLimit));
    }
  }

  /**
   * Set the format for an input or output, handling ontologies
   *
   * @param inputOutput The input or output CWL Element
   * @param format The format URI
   */
  private void setFormat(CWLElement inputOutput, String format) {
    inputOutput.setFormat(format);
    try {
      if (!rdfService.ontPropertyExists(format)) {
        Model ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        ontModel.read(format, null, "RDF/XML");
        rdfService.addToOntologies(ontModel);
      }
      String formatLabel = rdfService.getOntLabel(format);
      inputOutput.setType(inputOutput.getType() + " [" + formatLabel + "]");
    } catch (RiotException ex) {
      inputOutput.setType(inputOutput.getType() + " [format]");
    }
  }

  /**
   * Convert RDF URI for a type to a name
   *
   * @param uri The URI for the type
   * @return The human readable name for that type
   */
  private String typeURIToString(String uri) {
    switch (uri) {
      case "http://www.w3.org/2001/XMLSchema#string":
        return "String";
      case "https://w3id.org/cwl/cwl#File":
        return "File";
      case "http://www.w3.org/2001/XMLSchema#boolean":
        return "Boolean";
      case "http://www.w3.org/2001/XMLSchema#int":
        return "Integer";
      case "http://www.w3.org/2001/XMLSchema#double":
        return "Double";
      case "http://www.w3.org/2001/XMLSchema#float":
        return "Float";
      case "http://www.w3.org/2001/XMLSchema#long":
        return "Long";
      case "https://w3id.org/cwl/cwl#Directory":
        return "Directory";
      default:
        return uri;
    }
  }

  /**
   * Converts a yaml String to JsonNode
   *
   * @param path A Path to a file containing the yaml content
   * @return A JsonNode with the content of the document
   * @throws IOException
   */
  private Map<String, Object> yamlPathToJson(Path path) throws IOException {
    LoadSettings settings = LoadSettings.builder().build();
    Load load = new Load(settings);
    try (InputStream in = Files.newInputStream(path)) {
      return (Map<String, Object>) load.loadFromInputStream(in);
    }
  }

  /**
   * Converts a yaml String to JsonNode
   *
   * @param yamlStream An InputStream containing the yaml content
   * @return A JsonNode with the content of the document
   */
  private Map<String, Object> yamlStreamToJson(InputStream yamlStream) {
    LoadSettings settings = LoadSettings.builder().build();
    Load load = new Load(settings);
    return (Map<String, Object>) load.loadFromInputStream(yamlStream);
  }

  /**
   * Extract the label from a node
   *
   * @param node The node to have the label extracted from
   * @return The string for the label of the node
   */
  private String extractLabel(Map<String, Object> node) {
    if (node != null && node.containsKey(LABEL)) {
      return (String) node.get(LABEL);
    }
    return null;
  }

  /**
   * Extract the class parameter from a node representing a document
   *
   * @param node The root node of a cwl document
   * @return Which process this document represents
   */
  private CWLProcess extractProcess(Map<String, Object> node) {
    if (node != null) {
      if (node.containsKey(CLASS)) {
        switch ((String) node.get(CLASS)) {
          case WORKFLOW:
            return CWLProcess.WORKFLOW;
          case COMMANDLINETOOL:
            return CWLProcess.COMMANDLINETOOL;
          case EXPRESSIONTOOL:
            return CWLProcess.EXPRESSIONTOOL;
        }
      }
    }
    return null;
  }

  /**
   * Get the steps for a particular document
   *
   * @param cwlDoc The document to get steps for
   * @return A map of step IDs and details related to them
   */
  private Map<String, CWLStep> getSteps(Map<String, Object> cwlDoc) {
    if (cwlDoc != null && cwlDoc.containsKey(STEPS)) {
      Map<String, CWLStep> returnMap = new HashMap<>();

      Object steps = cwlDoc.get(STEPS);
      if (List.class.isAssignableFrom(steps.getClass())) {
        // Explicit ID and other fields within each input list
        for (Map<String, Object> step : (List<Map<String, Object>>) steps) {
          CWLStep stepObject =
              new CWLStep(extractLabel(step), extractDoc(step), extractRun(step), getInputs(step));
          returnMap.put(extractID(step), stepObject);
        }
      } else if (Map.class.isAssignableFrom(steps.getClass())) {
        // ID is the key of each object
        for (Entry<String, Map<String, Object>> stepEntry :
            ((Map<String, Map<String, Object>>) steps).entrySet()) {
          Map<String, Object> step = stepEntry.getValue();
          CWLStep stepObject =
              new CWLStep(extractLabel(step), extractDoc(step), extractRun(step), getInputs(step));
          returnMap.put(stepEntry.getKey(), stepObject);
        }
      }

      return returnMap;
    }
    return null;
  }

  /**
   * Get a the inputs for a particular document
   *
   * @param cwlDoc The document to get inputs for
   * @return A map of input IDs and details related to them
   */
  private Map<String, CWLElement> getInputs(Map<String, Object> cwlDoc) {
    if (cwlDoc != null) {
      if (cwlDoc.containsKey(INPUTS)) {
        // For all version workflow inputs/outputs and draft steps
        return getInputsOutputs(cwlDoc.get(INPUTS));
      } else if (cwlDoc.containsKey(IN)) {
        // For V1.0 steps
        return getStepInputsOutputs(cwlDoc.get(IN));
      }
    }
    return null;
  }

  /**
   * Get the outputs for a particular document
   *
   * @param cwlDoc The document to get outputs for
   * @return A map of output IDs and details related to them
   */
  private Map<String, CWLElement> getOutputs(Map<String, Object> cwlDoc) {
    if (cwlDoc != null) {
      // For all version workflow inputs/outputs and draft steps
      if (cwlDoc.containsKey(OUTPUTS)) {
        return getInputsOutputs(cwlDoc.get(OUTPUTS));
      }
      // Outputs are not gathered for v1 steps
    }
    return null;
  }

  /**
   * Get inputs or outputs from an in or out node
   *
   * @param inOut The in or out node
   * @return A map of input IDs and details related to them
   */
  private Map<String, CWLElement> getStepInputsOutputs(Object inOut) {
    Map<String, CWLElement> returnMap = new HashMap<>();

    if (List.class.isAssignableFrom(inOut.getClass())) {
      // array<WorkflowStepInput>
      for (Map<String, Object> inOutNode : (List<Map<String, Object>>) inOut) {
        CWLElement inputOutput = new CWLElement();
        List<String> sources = extractSource(inOutNode);
        if (sources.size() > 0) {
          for (String source : sources) {
            inputOutput.addSourceID(stepIDFromSource(source));
          }
        } else {
          inputOutput.setDefaultVal(extractDefault(inOutNode));
        }
        returnMap.put(extractID(inOutNode), inputOutput);
      }
    } else if (Map.class.isAssignableFrom(inOut.getClass())) {
      // map<WorkflowStepInput.id, WorkflowStepInput.source>
      Set<Entry<String, Object>> iterator = ((Map<String, Object>) inOut).entrySet();
      for (Entry<String, Object> entry : iterator) {
        Object inOutNode = entry.getValue();
        CWLElement inputOutput = new CWLElement();
        if (Map.class.isAssignableFrom(inOutNode.getClass())) {
          Map<String, Object> properties = (Map<String, Object>) inOutNode;
          if (properties.containsKey(SOURCE)) {
            Object source = properties.get(SOURCE);
            if (List.class.isAssignableFrom(source.getClass())) {
              for (String sourceEntry : (List<String>) source) {
                inputOutput.addSourceID(stepIDFromSource(sourceEntry));
              }
            } else {
              inputOutput.addSourceID(stepIDFromSource((String) source));
            }
          } else {
            inputOutput.setDefaultVal(extractDefault(properties));
          }
        } else if (List.class.isAssignableFrom(inOutNode.getClass())) {
          for (String key : (List<String>) inOutNode) {
            inputOutput.addSourceID(stepIDFromSource(key));
          }
        } else {
          inputOutput.addSourceID(stepIDFromSource((String) inOutNode));
        }
        returnMap.put(entry.getKey(), inputOutput);
      }
    }

    return returnMap;
  }

  /**
   * Get inputs or outputs from an inputs or outputs node
   *
   * @param object The inputs or outputs node
   * @return A map of input IDs and details related to them
   */
  private Map<String, CWLElement> getInputsOutputs(Object object) {
    Map<String, CWLElement> returnMap = new HashMap<>();

    if (List.class.isAssignableFrom(object.getClass())) {
      // Explicit ID and other fields within each list
      for (Map<String, Object> inputOutput : (List<Map<String, Object>>) object) {
        String id = (String) inputOutput.get(ID);
        if (id.charAt(0) == '#') {
          id = id.substring(1);
        }
        returnMap.put(id, getDetails(inputOutput));
      }
    } else if (Map.class.isAssignableFrom(object.getClass())) {
      // ID is the key of each object
      Set<Entry<String, Object>> iterator = ((Map<String, Object>) object).entrySet();
      for (Entry<String, Object> inputOutputNode : iterator) {
        returnMap.put(inputOutputNode.getKey(), getDetails(inputOutputNode.getValue()));
      }
    }

    return returnMap;
  }

  /**
   * Gets the details of an input or output
   *
   * @param inputOutput The node of the particular input or output
   * @return An CWLElement object with the label, doc and type extracted
   */
  private CWLElement getDetails(Object inputOutput) {
    if (inputOutput != null) {
      CWLElement details = new CWLElement();

      // Shorthand notation "id: type" - no label/doc/other params
      if (inputOutput.getClass() == String.class) {
        details.setType((String) inputOutput);
      } else if (List.class.isAssignableFrom(inputOutput.getClass())) {
        details.setType(this.extractTypes(inputOutput));
      } else if (Map.class.isAssignableFrom(inputOutput.getClass())) {
        Map<String, Object> iOMap = (Map<String, Object>) inputOutput;
        details.setLabel(extractLabel(iOMap));
        details.setDoc(extractDoc(iOMap));
        extractSource(iOMap).forEach(details::addSourceID);
        details.setDefaultVal(extractDefault(iOMap));

        // Type is only for inputs
        if (iOMap.containsKey(TYPE)) {
          details.setType(extractTypes(iOMap.get(TYPE)));
        }
      }

      return details;
    }
    return null;
  }

  /**
   * Extract the id from a node
   *
   * @param step The node to have the id extracted from
   * @return The string for the id of the node
   */
  private String extractID(Map<String, Object> step) {
    if (step != null && step.containsKey(ID)) {
      String id = (String) step.get(ID);
      if (id.startsWith("#")) {
        return id.substring(1);
      }
      return id;
    }
    return null;
  }

  /**
   * Extract the default value from a node
   *
   * @param inputOutput The node to have the label extracted from
   * @return The string for the default value of the node
   */
  private String extractDefault(Map<String, Object> inputOutput) {
    if (inputOutput != null && inputOutput.containsKey(DEFAULT)) {
      Object default_value = ((Map<String, Object>) inputOutput).get(DEFAULT);
      if (default_value == null) {
        return null;
      }
      if (Map.class.isAssignableFrom(default_value.getClass())
          && ((Map<String, Object>) default_value).containsKey(LOCATION)) {
        return (String) ((Map<String, Object>) default_value).get(LOCATION);
      } else {
        return "\\\"" + default_value + "\\\"";
      }
    }
    return null;
  }

  /**
   * Extract the source or outputSource from a node
   *
   * @param inputOutput The node to have the sources extracted from
   * @return A list of strings for the sources
   */
  private List<String> extractSource(Map<String, Object> inputOutput) {
    if (inputOutput != null) {
      List<String> sources = new ArrayList<String>();
      Object sourceNode = null;

      // outputSource and source treated the same
      if (inputOutput.containsKey(OUTPUT_SOURCE)) {
        sourceNode = inputOutput.get(OUTPUT_SOURCE);
      } else if (inputOutput.containsKey(SOURCE)) {
        sourceNode = inputOutput.get(SOURCE);
      }

      if (sourceNode != null) {
        // Single source
        if (String.class.isAssignableFrom(sourceNode.getClass())) {
          sources.add(stepIDFromSource((String) sourceNode));
        }
        // Can be an array of multiple sources
        if (List.class.isAssignableFrom(sourceNode.getClass())) {
          for (String source : (List<String>) sourceNode) {
            sources.add(stepIDFromSource(source));
          }
        }
      }

      return sources;
    }
    return null;
  }

  /**
   * Gets just the step ID from source of format 'stepID</ or .>outputID'
   *
   * @param source The source
   * @return The step ID
   */
  private String stepIDFromSource(String source) {
    if (source != null && source.length() > 0) {
      // Strip leading # if it exists
      if (source.charAt(0) == '#') {
        source = source.substring(1);
      }

      // Draft 3/V1 notation is 'stepID/outputID'
      int slashSplit = source.indexOf("/");
      if (slashSplit != -1) {
        source = source.substring(0, slashSplit);
      } else {
        // Draft 2 notation was 'stepID.outputID'
        int dotSplit = source.indexOf(".");
        if (dotSplit != -1) {
          source = source.substring(0, dotSplit);
        }
      }
    }
    return source;
  }

  /**
   * Extract the doc or description from a node
   *
   * @param cwlFile The node to have the doc/description extracted from
   * @return The string for the doc/description of the node
   */
  private String extractDoc(Map<String, Object> cwlFile) {
    if (cwlFile != null) {
      if (cwlFile.containsKey(DOC)) {
        Object doc = cwlFile.get(DOC);
        if (doc == null) {
          return null;
        }
        if (doc.getClass().isAssignableFrom(String.class)) {
          return (String) doc;
        }
        if (doc instanceof List<?>) {
          List<String> docList = (List<String>) doc;
          return String.join("", docList);
        }
        return (String) cwlFile.get(DOC);
      } else if (cwlFile.containsKey(DESCRIPTION)) {
        // This is to support older standards of cwl which use description instead of
        // doc
        return (String) cwlFile.get(DESCRIPTION);
      }
    }
    return null;
  }

  /**
   * Extract the types from a node representing inputs or outputs
   *
   * @param typeNode The root node representing an input or output
   * @return A string with the types listed
   */
  private String extractTypes(Object typeNode) {
    if (typeNode != null) {
      if (typeNode.getClass() == String.class) {
        // Single type
        return (String) typeNode;
      } else if (List.class.isAssignableFrom(typeNode.getClass())) {
        // Multiple types, build a string to represent them
        StringBuilder typeDetails = new StringBuilder();
        boolean optional = false;
        for (Object type : (List<Object>) typeNode) {
          if (type.getClass() == String.class) {
            // This is a simple type
            if (((String) type).equals("null")) {
              // null as a type means this field is optional
              optional = true;
            } else {
              // Add a simple type to the string
              typeDetails.append((String) type);
              typeDetails.append(", ");
            }
          } else if (Map.class.isAssignableFrom(type.getClass())) {
            // This is a verbose type with sub-fields broken down into type: and other
            // params
            if (((Map<String, Object>) type).get(TYPE).equals(ARRAY)) {
              Object items = ((Map<String, Object>) type).get(ARRAY_ITEMS);
              if (items.getClass() == String.class) {
                typeDetails.append(items);
                typeDetails.append("[], ");
              } else {
                typeDetails.append(type.toString() + ", ");
              }
            } else {
              typeDetails.append((String) ((Map<String, Object>) type).get(TYPE));
            }
          }
        }

        // Trim off excessive separators
        if (typeDetails.length() > 1) {
          typeDetails.setLength(typeDetails.length() - 2);
        }

        // Add optional if null was included in the multiple types
        if (optional) typeDetails.append("?");

        // Set the type to the constructed string
        return typeDetails.toString();

      } else if (Map.class.isAssignableFrom(typeNode.getClass())) {
        // Type: array and items:
        if (((Map<String, Object>) typeNode).containsKey(ARRAY_ITEMS)) {
          return extractTypes(((Map<String, String>) typeNode).get(ARRAY_ITEMS)) + "[]";
        }
      }
    }
    return null;
  }

  /**
   * Extract the run parameter from a node representing a step
   *
   * @param step The root node of a step
   * @return A string with the run parameter if it exists
   */
  private Object extractRun(Map<String, Object> step) {
    if (step != null) {
      if (step.containsKey(RUN)) {
        return step.get(RUN);
      }
    }
    return null;
  }

  public String normaliseLicenseLink(String licenseLink) {
    if (licenseLink == null) {
      return null;
    }
    String httpsLicenseLink = StringUtils.stripEnd(licenseLink.replace("http://", "https://"), "/");
    return licenseVocab.getOrDefault(httpsLicenseLink, licenseLink);
  }
}
