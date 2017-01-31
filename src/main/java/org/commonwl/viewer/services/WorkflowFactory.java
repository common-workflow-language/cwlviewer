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

package org.commonwl.viewer.services;

import org.commonwl.viewer.domain.CWLCollection;
import org.commonwl.viewer.domain.GithubDetails;
import org.commonwl.viewer.domain.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class WorkflowFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GitHubService githubService;
    private final ROBundleFactory ROBundleFactory;

    @Autowired
    public WorkflowFactory(GitHubService githubService,
                           ROBundleFactory ROBundleFactory) {
        this.githubService = githubService;
        this.ROBundleFactory = ROBundleFactory;
    }

    /**
     * Builds a new workflow from cwl files fetched from Github
     * @param githubInfo Github information for the workflow
     * @return The constructed model for the Workflow
     */
    public Workflow workflowFromGithub(GithubDetails githubInfo) {

        try {
            // Set up CWL utility to collect the documents
            CWLCollection cwlFiles = new CWLCollection(githubService, githubInfo);

            // Get the workflow model
            Workflow workflowModel = cwlFiles.getWorkflow();
            if (workflowModel != null) {
                // Set origin details
                workflowModel.setRetrievedOn(new Date());
                workflowModel.setRetrievedFrom(githubInfo);

                // Create a new research object bundle from Github details
                // This is Async so cannot just call constructor, needs intermediate as per Spring framework
                ROBundleFactory.workflowROFromGithub(githubService, githubInfo);

                // Return this model to be displayed
                return workflowModel;

            } else {
                logger.error("No workflow could be found");
            }
        } catch (Exception ex) {
            logger.error("Error creating workflow: " + ex.getMessage());
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
