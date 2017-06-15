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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.commonwl.view.docker.DockerService;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowOverview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides CWL parsing for workflows to gather an overview
 * for display and visualisation
 */
@Service
public class CWLService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Autowired properties/services
    private final GitHubService githubService;
    private final RDFService rdfService;
    private final CWLTool cwlTool;
    private final int singleFileSizeLimit;

    // CWL specific strings
    private final String DOC_GRAPH = "$graph";
    private final String CLASS = "class";
    private final String WORKFLOW = "Workflow";
    private final String COMMANDLINETOOL = "CommandLineTool";
    private final String EXPRESSIONTOOL = "ExpressionTool";
    private final String LABEL = "label";
    private final String DOC = "doc";
    private final String DESCRIPTION = "description";

    /**
     * Constructor for the Common Workflow Language service
     * @param githubService A service for accessing Github functionality
     * @param cwlTool Handles cwltool integration
     * @param singleFileSizeLimit The file size limit for single files
     */
    @Autowired
    public CWLService(GitHubService githubService,
                      RDFService rdfService,
                      CWLTool cwlTool,
                      @Value("${singleFileSizeLimit}") int singleFileSizeLimit) {
        this.githubService = githubService;
        this.rdfService = rdfService;
        this.cwlTool = cwlTool;
        this.singleFileSizeLimit = singleFileSizeLimit;
    }

    /**
     * Gets the Workflow object from a Github file
     * @param githubInfo The Github repository information
     * @param latestCommit The latest commit ID
     * @return The constructed workflow object
     */
    public Workflow parseWorkflow(GithubDetails githubInfo, String latestCommit) throws IOException {

        // Get rdf representation from cwltool
        String url = String.format("https://cdn.rawgit.com/%s/%s/%s/%s", githubInfo.getOwner(),
                githubInfo.getRepoName(), latestCommit, githubInfo.getPath());
        String rdf = cwlTool.getRDF(url);

        // Create a workflow model from RDF representation
        final Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(rdf.getBytes()), null, "TURTLE");

        // Base workflow details
        Resource workflow = model.getResource(url);
        String label = rdfService.getLabel(workflow);
        String doc = rdfService.getDoc(workflow);

        // Inputs
        Map<String, CWLElement> wfInputs = new HashMap<>();
        ResultSet inputs = rdfService.getInputs(model, url);
        while (inputs.hasNext()) {
            QuerySolution input = inputs.nextSolution();
            CWLElement wfInput = new CWLElement();
            wfInput.setType(typeURIToString(input.get("type").toString()));
            if (input.contains("label")) {
                wfInput.setLabel(input.get("label").toString());
            }
            if (input.contains("doc")) {
                wfInput.setDoc(input.get("doc").toString());
            }
            wfInputs.put(stepFromURI(
                    input.get("input").toString()), wfInput);
        }

        // Outputs
        Map<String, CWLElement> wfOutputs = new HashMap<>();
        ResultSet outputs = rdfService.getOutputs(model, url);
        while (outputs.hasNext()) {
            QuerySolution output = outputs.nextSolution();
            CWLElement wfOutput = new CWLElement();
            wfOutput.setType(typeURIToString(output.get("type").toString()));
            if (output.contains("src")) {
                wfOutput.addSourceID(stepFromURI(
                        output.get("src").toString()));
            }
            if (output.contains("label")) {
                wfOutput.setLabel(output.get("label").toString());
            }
            if (output.contains("doc")) {
                wfOutput.setDoc(output.get("doc").toString());
            }
            wfOutputs.put(stepFromURI(
                    output.get("output").toString()), wfOutput);
        }


        // Steps
        Map<String, CWLStep> wfSteps = new HashMap<>();
        ResultSet steps = rdfService.getSteps(model, url);
        while (steps.hasNext()) {
            QuerySolution step = steps.nextSolution();
            String uri = stepFromURI(step.get("step").toString());
            if (wfSteps.containsKey(uri)) {
                // Already got step details, add extra source ID
                if (step.contains("src")) {
                    CWLElement src = new CWLElement();
                    src.addSourceID(stepFromURI(step.get("src").toString()));
                    wfSteps.get(uri).getSources().put(
                            step.get("stepinput").toString(), src);
                } else if (step.contains("default")) {
                    CWLElement src = new CWLElement();
                    src.setDefaultVal(formatDefault(step.get("default").toString()));
                    wfSteps.get(uri).getSources().put(
                            step.get("stepinput").toString(), src);
                }
            } else {
                // Add new step
                CWLStep wfStep = new CWLStep();

                Path workflowPath = Paths.get(FilenameUtils.getPath(url));
                Path runPath = Paths.get(step.get("run").toString());
                wfStep.setRun(workflowPath.relativize(runPath).toString());
                wfStep.setRunType(strToRuntype(step.get("runtype").toString()));

                if (step.contains("src")) {
                    CWLElement src = new CWLElement();
                    src.addSourceID(stepFromURI(step.get("src").toString()));
                    Map<String, CWLElement> srcList = new HashMap<>();
                    srcList.put(stepFromURI(
                            step.get("stepinput").toString()), src);
                    wfStep.setSources(srcList);
                } else if (step.contains("default")) {
                    CWLElement src = new CWLElement();
                    src.setDefaultVal(formatDefault(step.get("default").toString()));
                    Map<String, CWLElement> srcList = new HashMap<>();
                    srcList.put(stepFromURI(
                            step.get("stepinput").toString()), src);
                    wfStep.setSources(srcList);
                }
                if (step.contains("label")) {
                    wfStep.setLabel(step.get("label").toString());
                }
                if (step.contains("doc")) {
                    wfStep.setDoc(step.get("doc").toString());
                }
                wfSteps.put(uri, wfStep);
            }
        }

        // Docker link
        ResultSet dockerResult = rdfService.getDockerLink(model, url);
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
        Workflow workflowModel = new Workflow(label, doc,
                wfInputs, wfOutputs, wfSteps, dockerLink);
        workflowModel.generateDOT();

        return workflowModel;

    }

    /**
     * Get an overview of a workflow
     * @param githubInfo The details to access the workflow
     * @return A constructed WorkflowOverview of the workflow
     * @throws IOException Any API errors which may have occurred
     */
    public WorkflowOverview getWorkflowOverview(GithubDetails githubInfo) throws IOException {

        // Get the content of this file from Github
        String fileContent = githubService.downloadFile(githubInfo);
        int fileSizeBytes = fileContent.getBytes("UTF-8").length;

        // Check file size limit before parsing
        if (fileSizeBytes <= singleFileSizeLimit) {

            // Parse file as yaml
            JsonNode cwlFile = yamlStringToJson(fileContent);

            // If the CWL file is packed there can be multiple workflows in a file
            if (cwlFile.has(DOC_GRAPH)) {
                // Packed CWL, find the first subelement which is a workflow and take it
                for (JsonNode jsonNode : cwlFile.get(DOC_GRAPH)) {
                    if (extractProcess(jsonNode) == CWLProcess.WORKFLOW) {
                        cwlFile = jsonNode;
                    }
                }
            }

            // Can only make an overview if this is a workflow
            if (extractProcess(cwlFile) == CWLProcess.WORKFLOW) {
                // Use filename for label if there is no defined one
                String path = FilenameUtils.getName(githubInfo.getPath());
                String label = extractLabel(cwlFile);
                if (label == null) {
                    label = path;
                }

                // Return the constructed overview
                return new WorkflowOverview(path, label, extractDoc(cwlFile));

            } else {
                return null;
            }
        } else {
            throw new IOException("File '" + githubInfo.getPath() +  "' is over singleFileSizeLimit - " +
                    FileUtils.byteCountToDisplaySize(fileSizeBytes) + "/" +
                    FileUtils.byteCountToDisplaySize(singleFileSizeLimit));
        }

    }

    /**
     * Gets the step ID from a full URI
     * @param uri The URI
     * @return The step ID
     */
    private String stepFromURI(String uri) {
        int lastHash = uri.lastIndexOf('#');
        if (lastHash != -1) {
            uri = uri.substring(lastHash + 1);
            int lastSlash = uri.lastIndexOf('/');
            if (lastSlash != -1) {
                uri = uri.substring(0, lastSlash);
            }
        }
        return uri;
    }

    /**
     * Format a default value
     * @param defaultVal The default value
     * @return Default value suitable for a node label
     */
    private String formatDefault(String defaultVal) {
        int lastCaret = defaultVal.indexOf("^^");
        if (lastCaret != -1) {
            return defaultVal.substring(0, lastCaret);
        }
        return defaultVal;
    }

    /**
     * Convert an RDF type to cwl process
     * @param runtype The string from the RDF
     * @return CWL process the string refers to
     */
    private CWLProcess strToRuntype(String runtype) {
        switch (runtype) {
            case "https://w3id.org/cwl/cwl#Workflow":
                return CWLProcess.WORKFLOW;
            case "https://w3id.org/cwl/cwl#CommandLineTool":
                return CWLProcess.COMMANDLINETOOL;
            case "https://w3id.org/cwl/cwl#ExpressionTool":
                return CWLProcess.EXPRESSIONTOOL;
            default:
                return null;
        }
    }

    /**
     * Convert RDF URI for a type to a name
     * @param uri The URI for the type
     * @return The human readable name for that type
     */
    private String typeURIToString(String uri) {
        switch (uri) {
            case "http://www.w3.org/2001/XMLSchema#string":
                return "String";
            case "https://w3id.org/cwl/cwl#File":
                return "File";
            case "http://www.w3.org/2001/XMLSchema#int":
                return "Integer";
            case "http://www.w3.org/2001/XMLSchema#double":
                return "Double";
            case "http://www.w3.org/2001/XMLSchema#float":
                return "Float";
            case "http://www.w3.org/2001/XMLSchema#long":
                return "Long";
            default:
                return uri;
        }
    }

    /**
     * Converts a yaml String to JsonNode
     * @param yaml A String containing the yaml content
     * @return A JsonNode with the content of the document
     */
    private JsonNode yamlStringToJson(String yaml) {
        Yaml reader = new Yaml();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(reader.load(yaml));
    }

    /**
     * Extract the label from a node
     * @param node The node to have the label extracted from
     * @return The string for the label of the node
     */
    private String extractLabel(JsonNode node) {
        if (node != null && node.has(LABEL)) {
            return node.get(LABEL).asText();
        }
        return null;
    }

    /**
     * Extract the doc or description from a node
     * @param node The node to have the doc/description extracted from
     * @return The string for the doc/description of the node
     */
    private String extractDoc(JsonNode node) {
        if (node != null) {
            if (node.has(DOC)) {
                return node.get(DOC).asText();
            } else if (node.has(DESCRIPTION)) {
                // This is to support older standards of cwl which use description instead of doc
                return node.get(DESCRIPTION).asText();
            }
        }
        return null;
    }

    /**
     * Extract the class parameter from a node representing a document
     * @param rootNode The root node of a cwl document
     * @return Which process this document represents
     */
    private CWLProcess extractProcess(JsonNode rootNode) {
        if (rootNode != null) {
            if (rootNode.has(CLASS)) {
                switch(rootNode.get(CLASS).asText()) {
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
}
