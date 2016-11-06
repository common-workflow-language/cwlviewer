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

package org.researchobject.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Agent;
import org.apache.taverna.robundle.manifest.Manifest;
import org.eclipse.egit.github.core.RepositoryContents;
import org.researchobject.domain.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowFactory {

    /**
     * Github API service
     */
    private final GitHubUtil githubUtil;
    private final int singleFileSizeLimit;
    private final int totalFileSizeLimit;
    private final String applicationName;
    private final String applicationURL;

    @Autowired
    public WorkflowFactory(GitHubUtil githubUtil,
                           @Value("${singleFileSizeLimit}") int singleFileSizeLimit,
                           @Value("${totalFileSizeLimit}") int totalFileSizeLimit,
                           @Value("${applicationName}") String applicationName,
                           @Value("${applicationURL}") String applicationURL) {
        this.githubUtil = githubUtil;
        this.singleFileSizeLimit = singleFileSizeLimit;
        this.totalFileSizeLimit = totalFileSizeLimit;
        this.applicationName = applicationName;
        this.applicationURL = applicationURL;
    }

    /**
     * Builds a new workflow from cwl files fetched from Github
     * @param githubURL Github directory URL to get the files from
     * @return The constructed model for the Workflow
     */
    public Workflow workflowFromGithub(String githubURL) {

        List<String> directoryDetails = githubUtil.detailsFromDirURL(githubURL);

        // If the URL is valid and details could be extracted
        if (directoryDetails.size() > 0) {

            // Store returned details
            final String owner = directoryDetails.get(0);
            final String repoName = directoryDetails.get(1);
            final String branch = directoryDetails.get(2);
            final String path = directoryDetails.get(3);

            // Get contents of the repo
            try {
                List<RepositoryContents> repoContents = githubUtil.getContents(owner, repoName, branch, path);

                // Check total file size
                int totalFileSize = 0;
                for (RepositoryContents repoContent : repoContents) {
                    totalFileSize += repoContent.getSize();
                }
                if (totalFileSize > totalFileSizeLimit) {
                    throw new IOException("Files within the Github directory can not be above " + totalFileSizeLimit + "B in size");
                }

                // Set up CWL utility to collect the documents
                CWLUtil cwlUtil = new CWLUtil();

                // Create a new RO bundle
                Bundle bundle = Bundles.createBundle();
                Manifest manifest = bundle.getManifest();

                // Simplified attribution for RO bundle
                try {

                    // Attribution for this tool
                    Agent githubCreator = new Agent(applicationName);
                    githubCreator.setUri(new URI(applicationURL));
                    manifest.setCreatedBy(githubCreator);

                    // ID is the github URL
                    manifest.setId(new URI(githubURL));

                    // Github author attribution
                    // TODO: way to add all the contributors somehow
                    // TODO: set the aggregates details according to the github information
                    List<Agent> authorList = new ArrayList<>(1);
                    Agent author = new Agent(owner);
                    author.setUri(new URI("https://github.com/" + owner));
                    authorList.add(author);
                    manifest.setAuthoredBy(authorList);

                } catch (URISyntaxException ex) {
                    System.out.println("Incorrect URI Syntax");
                }

                // Make a directory in the RO bundle to store the files
                Path bundleFiles = bundle.getRoot().resolve("workflow");
                Files.createDirectory(bundleFiles);

                // Loop through repo contents
                for (RepositoryContents repoContent : repoContents) {

                    // TODO: Go into subdirectories
                    if (repoContent.getType().equals("file")) {

                        // Check file size before downloading
                        if (repoContent.getSize() > singleFileSizeLimit) {
                            throw new IOException("Files within the Github directory can not be above " + singleFileSizeLimit + "B in size");
                        }

                        // Get the content of this file from Github
                        String fileContent = githubUtil.downloadFile(owner, repoName, branch, repoContent.getPath());

                        // Save file to research object bundle
                        Path newFilePort = bundleFiles.resolve(repoContent.getName());
                        Bundles.setStringValue(newFilePort, fileContent);

                        // Get the file extension
                        int eIndex = repoContent.getName().lastIndexOf('.') + 1;
                        if (eIndex > 0) {
                            String extension = repoContent.getName().substring(eIndex);

                            // If this is a cwl file which needs to be parsed
                            if (extension.equals("cwl")) {
                                // Parse yaml to JsonNode
                                Yaml reader = new Yaml();
                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode cwlFile = mapper.valueToTree(reader.load(fileContent));

                                // Add document to those being considered
                                cwlUtil.addDoc(cwlFile);
                            }
                        }
                    }
                }

                // Save the Research Object Bundle
                Path zip = Files.createTempFile("bundle", ".zip");
                Bundles.closeAndSaveBundle(bundle, zip);

                return cwlUtil.getWorkflow();

            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        } else {
            System.out.println("Error should never happen, already passed validation");
        }

        return null;
    }
}
