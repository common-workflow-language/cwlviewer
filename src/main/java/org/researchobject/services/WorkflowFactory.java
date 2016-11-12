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

import org.researchobject.domain.CWLCollection;
import org.researchobject.domain.GithubDetails;
import org.researchobject.domain.WorkflowRO;
import org.researchobject.domain.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class WorkflowFactory {

    /**
     * Github API service
     */
    private final GitHubUtil githubUtil;
    private final int singleFileSizeLimit;
    private final int totalFileSizeLimit;

    @Autowired
    public WorkflowFactory(GitHubUtil githubUtil,
                           @Value("${singleFileSizeLimit}") int singleFileSizeLimit,
                           @Value("${totalFileSizeLimit}") int totalFileSizeLimit) {
        this.githubUtil = githubUtil;
        this.singleFileSizeLimit = singleFileSizeLimit;
        this.totalFileSizeLimit = totalFileSizeLimit;
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

            // Store details from URL
            GithubDetails githubInfo = new GithubDetails(directoryDetails.get(0),
                    directoryDetails.get(1), directoryDetails.get(2));
            String githubBasePath = directoryDetails.get(3);

            try {
                // Set up CWL utility to collect the documents
                CWLCollection cwlFiles = new CWLCollection(githubUtil, githubInfo, githubBasePath);

                // Create a new research object bundle from Github details
                WorkflowRO ROBundle = new WorkflowRO(githubUtil, githubInfo, githubBasePath);

                // Save the Research Object Bundle
                ROBundle.saveToTempFile();

                // Return the model of the workflow
                return cwlFiles.getWorkflow();

            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        } else {
            System.out.println("Error should never happen, already passed validation");
        }

        return null;
    }

    /*
        // Check total file size
        int totalFileSize = 0;
        for (RepositoryContents repoContent : repoContents) {
            totalFileSize += repoContent.getSize();
        }
        if (totalFileSize > totalFileSizeLimit) {
            throw new IOException("Files within the Github directory can not be above "
                                  + totalFileSizeLimit + "B in size");
        }
    */


    /*
        // Check file size before downloading
        if (file.getSize() > singleFileSizeLimit) {
            throw new IOException("Files within the Github directory can not be above " + singleFileSizeLimit + "B in size");
        }
    */
}
