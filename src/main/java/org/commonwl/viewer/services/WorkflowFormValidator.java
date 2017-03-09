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

import org.commonwl.viewer.domain.GithubDetails;
import org.commonwl.viewer.domain.WorkflowForm;
import org.eclipse.egit.github.core.RepositoryContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.io.IOException;
import java.util.List;

/**
 * Runs validation on the workflow form from the main page
 */
@Component
public class WorkflowFormValidator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Github API service
     */
    private final GitHubService githubService;

    @Autowired
    public WorkflowFormValidator(GitHubService githubService) {
        this.githubService = githubService;
    }

    /**
     * Validates a WorkflowForm to ensure the URL is not empty and directory contains cwl files
     * @param form The given WorkflowForm
     * @param e Any errors from validation
     */
    public GithubDetails validateAndParse(WorkflowForm form, Errors e) {
        ValidationUtils.rejectIfEmptyOrWhitespace(e, "githubURL", "githubURL.emptyOrWhitespace");

        // Only continue if not null and isn't just whitespace
        if (!e.hasErrors()) {
            GithubDetails githubInfo = githubService.detailsFromDirURL(form.getGithubURL());

            // If the URL is valid and details could be extracted
            if (githubInfo != null) {

                // Check the repository exists and get content to ensure that branch/path exist
                try {
                    // Return the Github information if it is valid
                    List<RepositoryContents> repoContents = githubService.getContents(githubInfo);
                    if (containsCWL(repoContents, githubInfo)) {
                        return githubInfo;
                    } else {
                        // The URL does not contain any .cwl files
                        logger.error("No .cwl files found at Github URL");
                        e.rejectValue("githubURL", "githubURL.missingWorkflow", "No .cwl files were found in the specified Github directory");
                    }
                } catch (IOException ex) {
                    // Given repository/branch/path does not exist or API error occured
                    logger.error("Github API Error", ex);
                    e.rejectValue("githubURL", "githubURL.apiError", "API Error - does the specified Github directory exist?");
                }
            } else {
                // The Github URL is not valid
                logger.error("The Github URL is not valid");
                e.rejectValue("githubURL", "githubURL.invalid");
            }
        } else {
            logger.error("Github URL is empty");
        }

        // Errors will stop this being used anyway
        return null;
    }

    /**
     * Recursively check for CWL files within a Github repository
     * @param repoContents A list of contents within a path in the repository
     * @param githubDetails The details of the repository
     * @return Whether the path contains CWL files
     * @throws IOException Any API errors which may have occurred
     */
    private boolean containsCWL(List<RepositoryContents> repoContents, GithubDetails githubDetails) throws IOException {
        // Check that there is at least 1 .cwl file
        for (RepositoryContents repoContent : repoContents) {
            if (repoContent.getType().equals(GitHubService.TYPE_DIR)) {
                GithubDetails githubSubdir = new GithubDetails(githubDetails.getOwner(),
                        githubDetails.getRepoName(), githubDetails.getBranch(), repoContent.getPath());
                List<RepositoryContents> subdirectory = githubService.getContents(githubSubdir);
                if (containsCWL(subdirectory, githubSubdir)) {
                    return true;
                }
            } else if (repoContent.getType().equals(GitHubService.TYPE_FILE)) {
                int eIndex = repoContent.getName().lastIndexOf('.') + 1;
                if (eIndex > 0) {
                    String extension = repoContent.getName().substring(eIndex);
                    if (extension.equals("cwl")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}