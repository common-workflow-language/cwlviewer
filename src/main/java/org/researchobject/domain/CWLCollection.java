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

package org.researchobject.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.eclipse.egit.github.core.RepositoryContents;
import org.researchobject.services.GitHubUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.*;

/**
 * Provides CWL parsing for workflows to gather an overview
 * for display and visualisation
 */
public class CWLCollection {

    private GitHubUtil githubUtil;
    private GithubDetails githubInfo;

    private List<JsonNode> cwlDocs = new ArrayList<>();
    private int mainWorkflowIndex = -1;

    /**
     * Creates a new collection of CWL files from a Github repository
     * @param githubInfo The information necessary to access the Github directory associated with the RO
     * @throws IOException Any API errors which may have occurred
     */
    public CWLCollection(GitHubUtil githubUtil, GithubDetails githubInfo, String githubBasePath) throws IOException {
        this.githubInfo = githubInfo;
        this.githubUtil = githubUtil;

        // Add any CWL files from the Github repo to this collection
        List<RepositoryContents> repoContents = githubUtil.getContents(githubInfo, githubBasePath);
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
            if (repoContent.getType().equals("dir")) {

                // Get the contents of the subdirectory
                List<RepositoryContents> subdirectory = githubUtil.getContents(githubInfo, repoContent.getPath());

                // Add the files in the subdirectory to this new folder
                addDocs(subdirectory);

                // Otherwise this is a file so add to the bundle
            } else if (repoContent.getType().equals("file")) {

                // Get the file extension
                int eIndex = repoContent.getName().lastIndexOf('.') + 1;
                if (eIndex > 0) {
                    String extension = repoContent.getName().substring(eIndex);

                    // If this is a cwl file which needs to be parsed
                    if (extension.equals("cwl")) {

                        // Get the content of this file from Github
                        String fileContent = githubUtil.downloadFile(githubInfo, repoContent.getPath());

                        // Parse yaml to JsonNode
                        Yaml reader = new Yaml();
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode cwlFile = mapper.valueToTree(reader.load(fileContent));

                        // Add document to those being considered
                        addDoc(cwlFile);
                    }
                }

            }
        }
    }

    /**
     * Adds a document to the group of those being parsed
     * @param newDoc The document to be added
     */
    private void addDoc(JsonNode newDoc) {
        // Make sure that this document is only one object and not multiple under a $graph directive
        if (newDoc.has("$graph")) {
            // Add each of the sub documents
            for (JsonNode jsonNode : newDoc.get("$graph")) {
                cwlDocs.add(jsonNode);
            }
        } else {
            // Otherwise just add the document itself
            cwlDocs.add(newDoc);
        }
    }

    /**
     * Find the main workflow object in the group of files being considered
     */
    private void findMainWorkflow() {
        // Find the first workflow we come across
        // TODO: Consider relationship between run: parameters to better discover this
        for (int i=0; i < cwlDocs.size(); i++) {
            if (cwlDocs.get(i).get("class").asText().equals("Workflow")) {
                mainWorkflowIndex = i;
            }
        }
    }

    /**
     * Gets the Workflow object for this collection of documents
     * @return A Workflow object representing the main workflow amongst the files added
     */
    public Workflow getWorkflow() {
        if (mainWorkflowIndex < 0) {
            findMainWorkflow();
        }
        JsonNode mainWorkflow = cwlDocs.get(mainWorkflowIndex);
        return new Workflow(extractLabel(mainWorkflow), extractDoc(mainWorkflow),
                            getInputs(mainWorkflow), getOutputs(mainWorkflow));
    }

    /**
     * Get a list of the inputs for a particular document
     * @param cwlDoc The document to get inputs for
     * @return A map of input IDs and details related to them
     */
    private Map<String, InputOutput> getInputs(JsonNode cwlDoc) {
        if (cwlDoc != null) {
            if (cwlDoc.has("inputs")) {
                return getInputsOutputs(cwlDoc.get("inputs"));
            }
        }
        return null;
    }

    /**
     * Get a list of the outputs for a particular document
     * @param cwlDoc The document to get outputs for
     * @return A map of output IDs and details related to them
     */
    private Map<String, InputOutput> getOutputs(JsonNode cwlDoc) {
        if (cwlDoc != null) {
            if (cwlDoc.has("outputs")) {
                return getInputsOutputs(cwlDoc.get("outputs"));
            }
        }
        return null;
    }

    /**
     * Get a list of inputs or outputs from an inputs or outputs node
     * @param inputsOutputs The inputs or outputs node
     * @return A map of input IDs and details related to them
     */
    private Map<String, InputOutput> getInputsOutputs(JsonNode inputsOutputs) {
        Map<String, InputOutput> returnMap = new HashMap<>();

        if (inputsOutputs.getClass() == ArrayNode.class) {
            // Explicit ID and other fields within each input list
            for (JsonNode inputOutput : inputsOutputs) {
                String id = inputOutput.get("id").asText();
                returnMap.put(id, getDetails(inputOutput));
            }
        } else if (inputsOutputs.getClass() == ObjectNode.class) {
            // ID is the key of each object
            Iterator<Map.Entry<String, JsonNode>> iterator = inputsOutputs.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> inputOutputNode = iterator.next();
                String outputID = inputOutputNode.getKey();
                returnMap.put(outputID, getDetails(inputOutputNode.getValue()));
            }
        }

        return returnMap;
    }

    /**
     * Gets the details of an input or output
     * @param inputOutput The node of the particular input or output
     * @return An InputOutput object with the label, doc and type extracted
     */
    private InputOutput getDetails(JsonNode inputOutput) {
        if (inputOutput != null) {
            InputOutput details = new InputOutput();

            // Shorthand notation "id: type" - no label/doc/other params
            if (inputOutput.getClass() == TextNode.class) {
                details.setType(inputOutput.asText());
            } else {
                if (extractLabel(inputOutput) != null) {
                    details.setLabel(extractLabel(inputOutput));
                }

                if (extractDoc(inputOutput) != null) {
                    details.setDoc(extractDoc(inputOutput));
                }

                // Type is only for inputs
                if (inputOutput.has("type")) {
                    details.setType(extractTypes(inputOutput.get("type")));
                }
            }

            return details;
        }
        return null;
    }

    /**
     * Extract the label from a node
     * @param node The node to have the label extracted from
     * @return The string for the label of the node
     */
    private String extractLabel(JsonNode node) {
        if (node != null) {
            if (node.has("label")) {
                return node.get("label").asText();
            }
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
            if (node.has("doc")) {
                return node.get("doc").asText();
            } else if (node.has("description")) {
                // This is to support older standards of cwl which use description instead of doc
                return node.get("description").asText();
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
                        if (type.get("type").asText().equals("array")) {
                            typeDetails.append(type.get("items").asText());
                            typeDetails.append("[], ");
                        } else {
                            typeDetails.append(type.get("type").asText());
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
            }
        }
        return null;
    }

}
