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
import org.apache.commons.codec.binary.Base64;
import org.eclipse.egit.github.core.RepositoryContents;
import org.researchobject.domain.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowFactory {

    /**
     * Github API service
     */
    private final GitHubUtil githubUtil;

    @Autowired
    public WorkflowFactory(GitHubUtil githubUtil) {
        this.githubUtil = githubUtil;
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

                // Filter the repository contents into a new list - only files with .cwl extension
                List<RepositoryContents> workflowFiles = new ArrayList<>();
                for (RepositoryContents repoContent : repoContents) {
                    if (repoContent.getType().equals("file")) {
                        int eIndex = repoContent.getName().lastIndexOf('.') + 1;
                        if (eIndex > 0) {
                            String extension = repoContent.getName().substring(eIndex);
                            if (extension.equals("cwl")) {
                                workflowFiles.add(repoContent);
                            }
                        }
                    }
                }

                // Set up CWL utility to collect the documents
                CWLUtil cwlUtil = new CWLUtil();

                // Loop through the cwl files
                for (RepositoryContents workflowFile : workflowFiles) {

                    // Get the content of specific file from the listing
                    List<RepositoryContents> currentWorkflow = githubUtil.getContents(owner, repoName, branch, workflowFile.getPath());
                    String fileContentBase64 = currentWorkflow.get(0).getContent();
                    String fileContent = new String(Base64.decodeBase64(fileContentBase64.getBytes()));

                    // Parse yaml to JsonNode
                    Yaml reader = new Yaml();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode cwlFile = mapper.valueToTree(reader.load(fileContent));

                    // Add document to those being considered
                    cwlUtil.addDoc(cwlFile);

                }

                return cwlUtil.getWorkflow();

            } catch (IOException ex) {
                System.out.println("API Error");
            }
        } else {
            System.out.println("Error should never happen, already passed validation");
        }

        return null;
    }
}
