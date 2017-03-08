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

package org.commonwl.viewer.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.commonwl.viewer.services.DockerService;
import org.commonwl.viewer.services.GitHubService;
import org.commonwl.viewer.web.PathTraversalException;
import org.eclipse.egit.github.core.RepositoryContents;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Provides CWL parsing for workflows to gather an overview
 * for display and visualisation
 */
public class CWLCollection {

    private GitHubService githubService;
    private GithubDetails githubInfo;
    private String commitSha;

    private int totalFileSize;
    private int totalFileSizeLimit;
    private int singleFileSizeLimit;

    // Maps of ID to associated JSON
    private Map<String, JsonNode> cwlDocs = new HashMap<>();

    // The main workflow
    private String mainWorkflowKey;

    // Extension of CWL files
    private final String CWL_EXTENSION = "cwl";

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
    private final String REQUIREMENTS = "requirements";
    private final String HINTS = "hints";
    private final String DOCKER_REQUIREMENT = "DockerRequirement";
    private final String DOCKER_PULL = "dockerPull";

    /**
     * Creates a new collection of CWL files from a Github repository
     * @param githubService Service to provide the Github API functionality
     * @param githubInfo The information necessary to access the Github directory associated with the RO
     * @param commitSha The sha hash of the latest commit used for caching
     * @throws IOException Any API errors which may have occurred
     */
    public CWLCollection(GitHubService githubService, GithubDetails githubInfo,
                         String commitSha, int singleFileSizeLimit, int totalFileSizeLimit) throws IOException {
        this.githubInfo = githubInfo;
        this.githubService = githubService;
        this.commitSha = commitSha;
        this.singleFileSizeLimit = singleFileSizeLimit;
        this.totalFileSizeLimit = totalFileSizeLimit;
        this.totalFileSize = 0;

        // Add any CWL files from the Github repo to this collection
        List<RepositoryContents> repoContents = githubService.getContents(githubInfo);
        addDocs(repoContents);
    }

    /**
     * Add the CWL documents from a Github repository
     * @param repoContents The contents of the Github base directory
     */
    private void addDocs(List<RepositoryContents> repoContents) throws IOException {
        // Loop through repo contents and add them
        for (RepositoryContents repoContent : repoContents) {

            // Parse subdirectories if they exist
            if (repoContent.getType().equals(GitHubService.TYPE_DIR)) {

                // Get the contents of the subdirectory
                GithubDetails githubSubdir = new GithubDetails(githubInfo.getOwner(),
                        githubInfo.getRepoName(), githubInfo.getBranch(), repoContent.getPath());
                List<RepositoryContents> subdirectory = githubService.getContents(githubSubdir);

                // Add the files in the subdirectory to this new folder
                addDocs(subdirectory);

            } else if (repoContent.getType().equals(GitHubService.TYPE_FILE)) {

                // Keep track of total file size for limit - only track files which
                // will be added to the RO bundle due to being small enough
                if (repoContent.getSize() <= singleFileSizeLimit) {
                    totalFileSize += repoContent.getSize();
                }

                if (totalFileSize <= totalFileSizeLimit) {
                    // Get the file extension
                    int eIndex = repoContent.getName().lastIndexOf('.') + 1;
                    if (eIndex > 0) {
                        String extension = repoContent.getName().substring(eIndex);

                        // If this is a cwl file which needs to be parsed
                        if (extension.equals(CWL_EXTENSION)) {
                            if (repoContent.getSize() <= singleFileSizeLimit) {
                                // Get the content of this file from Github
                                GithubDetails githubFile = new GithubDetails(githubInfo.getOwner(),
                                        githubInfo.getRepoName(), githubInfo.getBranch(), repoContent.getPath());
                                String fileContent = githubService.downloadFile(githubFile, commitSha);

                                // Parse yaml to JsonNode
                                Yaml reader = new Yaml();
                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode cwlFile = mapper.valueToTree(reader.load(fileContent));

                                // Add document to those being considered
                                addDoc(cwlFile, repoContent.getPath());
                            } else {
                                throw new IOException("File '" + repoContent.getName() +  "' is over singleFileSizeLimit - " +
                                        FileUtils.byteCountToDisplaySize(repoContent.getSize()) + "/" +
                                        FileUtils.byteCountToDisplaySize(singleFileSizeLimit));
                            }
                        }
                    }
                } else {
                    throw new IOException("Contents of the repository are over totalFileSizeLimit of " +
                            FileUtils.byteCountToDisplaySize(totalFileSizeLimit));
                }
            }
        }
    }

    /**
     * Adds a document to the group of those being parsed
     * @param newDoc The document to be added
     * @param fileName The name of the file this document has come from
     */
    private void addDoc(JsonNode newDoc, String fileName) {
        // Make sure that this document is only one object and not multiple under a $graph directive
        if (newDoc.has(DOC_GRAPH)) {
            // Add each of the sub documents
            for (JsonNode jsonNode : newDoc.get(DOC_GRAPH)) {
                cwlDocs.put(extractID(jsonNode), jsonNode);
            }
        } else {
            // Otherwise just add the document itself with ID of document name
            cwlDocs.put(fileName, newDoc);
        }
    }

    /**
     * Find the main workflow object in the group of files being considered
     * by finding the minimal inDegree in a graph of run: parameters within steps
     * @return The file name/key of the workflow
     */
    private String findMainWorkflow() {
        // TODO: make this path dependant so it doesn't get messed up by duplicate filenames or graphs
        // Currently this strategy fails gracefully and returns the first workflow in the case of a graph

        // Store the in degree of each workflow
        Map<String, Integer> inDegrees = new HashMap<String, Integer>();
        for (Map.Entry<String, JsonNode> doc : cwlDocs.entrySet()) {
            if (doc.getValue().get(CLASS).asText().equals(WORKFLOW)) {
                inDegrees.put(doc.getKey(), 0);
            }
        }

        // Loop through documents and calculate in degrees
        for (Map.Entry<String, JsonNode> doc : cwlDocs.entrySet()) {
            JsonNode content = doc.getValue();
            if (inDegrees.containsKey(doc.getKey())) {
                // Parse workflow steps and see whether other workflows are run
                JsonNode steps = content.get(STEPS);
                if (steps.getClass() == ArrayNode.class) {
                    // Explicit ID and other fields within each input list
                    for (JsonNode step : steps) {
                        String run = FilenameUtils.getName(extractRun(step));
                        if (run != null && inDegrees.containsKey(run)) {
                            inDegrees.put(run, inDegrees.get(run) + 1);
                        }
                    }
                } else if (steps.getClass() == ObjectNode.class) {
                    // ID is the key of each object
                    Iterator<Map.Entry<String, JsonNode>> iterator = steps.fields();
                    while (iterator.hasNext()) {
                        Map.Entry<String, JsonNode> stepNode = iterator.next();
                        JsonNode stepJson = stepNode.getValue();
                        String run = FilenameUtils.getName(extractRun(stepJson));
                        if (run != null && inDegrees.containsKey(run)) {
                            inDegrees.put(run, inDegrees.get(run) + 1);
                        }
                    }
                }
            }
        }

        // Find a workflow with minimum inDegree and return
        int minVal = Integer.MAX_VALUE;
        String minKey = null;
        for (Map.Entry<String, Integer> inDegree : inDegrees.entrySet()) {
            if (inDegree.getValue() < minVal) {
                minKey = inDegree.getKey();
                minVal = inDegree.getValue();
            }

            // Early escape if minVal is already minimal
            if (minVal == 0) {
                return minKey;
            }
        }

        return minKey;
    }

    /**
     * Gets the Workflow object for this collection of documents
     * @return A Workflow object representing the main workflow amongst the files added
     * @throws PathTraversalException If run path is outside the root Github directory
     */
    public Workflow getWorkflow() throws PathTraversalException {
        // Get the main workflow
        if (mainWorkflowKey == null) {
            mainWorkflowKey = findMainWorkflow();

            // If it is still null there is no workflow to be found
            if (mainWorkflowKey == null) {
                return null;
            }
        }
        JsonNode mainWorkflow = cwlDocs.get(mainWorkflowKey);

        // Use ID/filename for label if there is no defined one
        String label = extractLabel(mainWorkflow);
        if (label == null) {
            label = mainWorkflowKey;
        }

        Workflow workflowModel = new Workflow(label, extractDoc(mainWorkflow), getInputs(mainWorkflow),
                getOutputs(mainWorkflow), getSteps(mainWorkflow), getDockerLink(mainWorkflow));
        fillStepRunTypes(workflowModel);
        return workflowModel;
    }

    /**
     * Fill the step runtypes based on types of other documents
     * @param workflow The workflow model to set runtypes for
     * @throws PathTraversalException If run path is outside the root Github directory
     */
    private void fillStepRunTypes(Workflow workflow) throws PathTraversalException {
        Map<String, CWLStep> steps = workflow.getSteps();
        for (CWLStep step : steps.values()) {
            String runParam = step.getRun();
            if (runParam != null) {
                JsonNode runDoc;
                if (runParam.startsWith("#")) {
                    runDoc = cwlDocs.get(runParam.substring(1));
                } else {
                    Path basePath = Paths.get(githubInfo.getPath());
                    Path filePath = basePath.resolve(runParam);
                    if (!filePath.startsWith(basePath)) {
                        throw new PathTraversalException("Step run parameter path '" + filePath +
                                "' is outside base path '" + basePath + "'");
                    }
                    runDoc = cwlDocs.get(filePath.toString());
                    step.setRunType(extractProcess(runDoc));
                }
                step.setRunType(extractProcess(runDoc));
            }
        }
    }

    /**
     * Get the steps for a particular document
     * @param cwlDoc The document to get steps for
     * @return A map of step IDs and details related to them
     */
    private Map<String, CWLStep> getSteps(JsonNode cwlDoc) {
        if (cwlDoc != null && cwlDoc.has(STEPS)) {
            Map<String, CWLStep> returnMap = new HashMap<>();

            JsonNode steps = cwlDoc.get(STEPS);
            if (steps.getClass() == ArrayNode.class) {
                // Explicit ID and other fields within each input list
                for (JsonNode step : steps) {
                    CWLStep stepObject = new CWLStep(extractLabel(step), extractDoc(step),
                            extractTypes(step), extractRun(step), getInputs(step), getOutputs(step));
                    returnMap.put(extractID(step), stepObject);
                }
            } else if (steps.getClass() == ObjectNode.class) {
                // ID is the key of each object
                Iterator<Map.Entry<String, JsonNode>> iterator = steps.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> stepNode = iterator.next();
                    JsonNode stepJson = stepNode.getValue();
                    CWLStep stepObject = new CWLStep(extractLabel(stepJson), extractDoc(stepJson),
                            extractTypes(stepJson), extractRun(stepJson), getInputs(stepJson),
                            getOutputs(stepJson));
                    returnMap.put(stepNode.getKey(), stepObject);
                }
            }

            return returnMap;
        }
        return null;
    }

    /**
     * Get a the inputs for a particular document
     * @param cwlDoc The document to get inputs for
     * @return A map of input IDs and details related to them
     */
    private Map<String, CWLElement> getInputs(JsonNode cwlDoc) {
        if (cwlDoc != null) {
            if (cwlDoc.has(INPUTS)) {
                // For workflow/draft steps
                return getInputsOutputs(cwlDoc.get(INPUTS));
            } else if (cwlDoc.has(IN)) {
                // For V1.0 steps
                return getStepInputsOutputs(cwlDoc.get(IN));
            }
        }
        return null;
    }

    /**
     * Get the outputs for a particular document
     * @param cwlDoc The document to get outputs for
     * @return A map of output IDs and details related to them
     */
    private Map<String, CWLElement> getOutputs(JsonNode cwlDoc) {
        if (cwlDoc != null) {
            if (cwlDoc.has(OUTPUTS)) {
                // For workflow/draft steps
                return getInputsOutputs(cwlDoc.get(OUTPUTS));
            } else if (cwlDoc.has(OUT)) {
                // For V1.0 steps
                return getStepInputsOutputs(cwlDoc.get(OUT));
            }
        }
        return null;
    }

    /**
     * Get inputs or outputs from an in or out node
     * @param inOut The in or out node
     * @return A map of input IDs and details related to them
     */
    private Map<String, CWLElement> getStepInputsOutputs(JsonNode inOut) {
        Map<String, CWLElement> returnMap = new HashMap<>();

        if (inOut.getClass() == ArrayNode.class) {
            // array<WorkflowStepInput>
            for (JsonNode inOutNode : inOut) {
                if (inOutNode.getClass() == ObjectNode.class) {
                    CWLElement inputOutput = new CWLElement();
                    List<String> sources = extractSource(inOutNode);
                    if (sources.size() > 0) {
                        for (String source : sources) {
                            inputOutput.addSourceID(source);
                        }
                    } else {
                        inputOutput.setDefaultVal(extractDefault(inOutNode));
                    }
                    returnMap.put(extractID(inOutNode), inputOutput);
                }
            }
        } else if (inOut.getClass() == ObjectNode.class) {
            // map<WorkflowStepInput.id, WorkflowStepInput.source>
            Iterator<Map.Entry<String, JsonNode>> iterator = inOut.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> inOutNode = iterator.next();
                CWLElement inputOutput = new CWLElement();
                if (inOutNode.getValue().getClass() == ObjectNode.class) {
                    inputOutput.setDefaultVal(extractDefault(inOutNode.getValue()));
                } else if (inOutNode.getValue().getClass() == ArrayNode.class) {
                    for (JsonNode key : inOutNode.getValue()) {
                        inputOutput.addSourceID(stepIDFromSource(key.asText()));
                    }
                } else {
                    inputOutput.addSourceID(stepIDFromSource(inOutNode.getValue().asText()));
                }
                returnMap.put(inOutNode.getKey(), inputOutput);
            }
        }

        return returnMap;
    }

    /**
     * Get inputs or outputs from an inputs or outputs node
     * @param inputsOutputs The inputs or outputs node
     * @return A map of input IDs and details related to them
     */
    private Map<String, CWLElement> getInputsOutputs(JsonNode inputsOutputs) {
        Map<String, CWLElement> returnMap = new HashMap<>();

        if (inputsOutputs.getClass() == ArrayNode.class) {
            // Explicit ID and other fields within each list
            for (JsonNode inputOutput : inputsOutputs) {
                String id = inputOutput.get(ID).asText();
                returnMap.put(id, getDetails(inputOutput));
            }
        } else if (inputsOutputs.getClass() == ObjectNode.class) {
            // ID is the key of each object
            Iterator<Map.Entry<String, JsonNode>> iterator = inputsOutputs.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> inputOutputNode = iterator.next();
                returnMap.put(inputOutputNode.getKey(), getDetails(inputOutputNode.getValue()));
            }
        }

        return returnMap;
    }

    /**
     * Gets the details of an input or output
     * @param inputOutput The node of the particular input or output
     * @return An CWLElement object with the label, doc and type extracted
     */
    private CWLElement getDetails(JsonNode inputOutput) {
        if (inputOutput != null) {
            CWLElement details = new CWLElement();

            // Shorthand notation "id: type" - no label/doc/other params
            if (inputOutput.getClass() == TextNode.class) {
                details.setType(inputOutput.asText());
            } else {
                details.setLabel(extractLabel(inputOutput));
                details.setDoc(extractDoc(inputOutput));
                extractSource(inputOutput).forEach(details::addSourceID);
                details.setDefaultVal(extractDefault(inputOutput));

                // Type is only for inputs
                if (inputOutput.has(TYPE)) {
                    details.setType(extractTypes(inputOutput.get(TYPE)));
                }
            }

            return details;
        }
        return null;
    }

    /**
     * Get the docker link from a workflow node
     * @param node The overall workflow node
     * @return A string with a dockerhub or image link
     */
    private String getDockerLink(JsonNode node) {
        if (node != null) {
            if (node.has(REQUIREMENTS)) {
                String dockerLink = extractDockerLink(node.get(REQUIREMENTS));
                if (dockerLink != null) {
                    return extractDockerLink(node.get(REQUIREMENTS));
                }
            }
            if (node.has(HINTS)) {
                String dockerLink = extractDockerLink(node.get(HINTS));
                if (dockerLink != null) {
                    return extractDockerLink(node.get(HINTS));
                }
            }
        }
        return null;
    }

    /**
     * Get the docker link from a workflow hints or requriements node
     * @param hintReq A hints or requirements node
     * @return A string with a dockerhub or image link
     */
    private String extractDockerLink(JsonNode hintReq) {
        // ArrayNode syntax using yaml lists
        if (hintReq.getClass() == ArrayNode.class) {
            for (JsonNode requirement : hintReq) {
                if (requirement.has(CLASS)
                        && requirement.get(CLASS).asText().equals(DOCKER_REQUIREMENT)) {
                    if (requirement.has(DOCKER_PULL)) {
                        // Format dockerPull string as a dockerhub URL
                        return DockerService.getDockerHubURL(requirement.get(DOCKER_PULL).asText());
                    } else {
                        // Indicate that the docker requirement exists, but we cannot
                        // provide a link to dockerhub as docker pull is not used
                        return "true";
                    }
                }
            }
            // v1.0 object syntax with requirement class as key
        } else if (hintReq.getClass() == ObjectNode.class) {
            // Look for DockerRequirement
            if (hintReq.has(DOCKER_REQUIREMENT)) {
                JsonNode dockerReq = hintReq.get(DOCKER_REQUIREMENT);
                if (dockerReq.has(DOCKER_PULL)) {
                    // Format dockerPull string as a dockerhub URL
                    String dockerPull = dockerReq.get(DOCKER_PULL).asText();
                    return DockerService.getDockerHubURL(dockerPull);
                } else {
                    // Indicate that the docker requirement exists, but we cannot
                    // provide a link to dockerhub as docker pull is not used
                    return "true";
                }
            }
        }
        return null;
    }

    /**
     * Extract the id from a node
     * @param node The node to have the id extracted from
     * @return The string for the id of the node
     */
    private String extractID(JsonNode node) {
        if (node != null && node.has(ID)) {
            String id = node.get(ID).asText();
            if (id.startsWith("#")) {
                return id.substring(1);
            }
            return id;
        }
        return null;
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
     * Extract the default value from a node
     * @param node The node to have the label extracted from
     * @return The string for the default value of the node
     */
    private String extractDefault(JsonNode node) {
        if (node != null && node.has(DEFAULT)) {
            if (node.get(DEFAULT).has(LOCATION)) {
                return node.get(DEFAULT).get(LOCATION).asText();
            } else {
                return "\\\"" + node.get(DEFAULT).asText() + "\\\"";
            }
        }
        return null;
    }

    /**
     * Extract the source or outputSource from a node
     * @param node The node to have the sources extracted from
     * @return A list of strings for the sources
     */
    private List<String> extractSource(JsonNode node) {
        if (node != null) {
            List<String> sources = new ArrayList<String>();
            JsonNode sourceNode = null;

            // outputSource and source treated the same
            if (node.has(OUTPUT_SOURCE)) {
                sourceNode = node.get(OUTPUT_SOURCE);
            } else if (node.has(SOURCE)) {
                sourceNode = node.get(SOURCE);
            }

            if (sourceNode != null) {
                // Single source
                if (sourceNode.getClass() == TextNode.class) {
                    sources.add(stepIDFromSource(sourceNode.asText()));
                }
                // Can be an array of multiple sources
                if (sourceNode.getClass() == ArrayNode.class) {
                    for (JsonNode source : sourceNode) {
                        sources.add(stepIDFromSource(source.asText()));
                    }
                }
            }

            return sources;
        }
        return null;
    }

    /**
     * Gets just the step ID from source of format 'stepID</ or .>outputID'
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
     * Extract the types from a node representing inputs or outputs
     * @param typeNode The root node representing an input or output
     * @return A string with the types listed
     */
    private String extractTypes(JsonNode typeNode) {
        if (typeNode != null) {
            if (typeNode.getClass() == TextNode.class) {
                // Single type
                return typeNode.asText();
            } else if (typeNode.getClass() == ArrayNode.class) {
                // Multiple types, build a string to represent them
                StringBuilder typeDetails = new StringBuilder();
                boolean optional = false;
                for (JsonNode type : typeNode) {
                    if (type.getClass() == TextNode.class) {
                        // This is a simple type
                        if (type.asText().equals("null")) {
                            // null as a type means this field is optional
                            optional = true;
                        } else {
                            // Add a simple type to the string
                            typeDetails.append(type.asText());
                            typeDetails.append(", ");
                        }
                    } else if (typeNode.getClass() == ArrayNode.class) {
                        // This is a verbose type with sub-fields broken down into type: and other params
                        if (type.get(TYPE).asText().equals(ARRAY)) {
                            typeDetails.append(type.get(ARRAY_ITEMS).asText());
                            typeDetails.append("[], ");
                        } else {
                            typeDetails.append(type.get(TYPE).asText());
                        }
                    }
                }

                // Trim off excessive separators
                if (typeDetails.length() > 1) {
                    typeDetails.setLength(typeDetails.length() - 2);
                }

                // Add optional if null was included in the multiple types
                if (optional) typeDetails.append(" (Optional)");

                // Set the type to the constructed string
                return typeDetails.toString();

            } else if (typeNode.getClass() == ObjectNode.class) {
                // Type: array and items:
                if (typeNode.has(ARRAY_ITEMS)) {
                    return typeNode.get(ARRAY_ITEMS).asText() + "[]";
                }
            }
        }
        return null;
    }

    /**
     * Extract the run parameter from a node representing a step
     * @param stepNode The root node of a step
     * @return A string with the run parameter if it exists
     */
    private String extractRun(JsonNode stepNode) {
        if (stepNode != null) {
            if (stepNode.has(RUN)) {
                return stepNode.get(RUN).asText();
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
