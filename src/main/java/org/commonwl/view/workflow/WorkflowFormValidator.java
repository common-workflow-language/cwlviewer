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

package org.commonwl.view.workflow;

import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.io.IOException;

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
     * Validates a WorkflowForm to ensure the URL is not empty and links to a cwl file
     * @param form The given WorkflowForm
     * @param e Any errors from validation
     */
    public GithubDetails validateAndParse(WorkflowForm form, Errors e) {
        ValidationUtils.rejectIfEmptyOrWhitespace(e, "githubURL", "githubURL.emptyOrWhitespace");

        // If not null and isn't just whitespace
        if (!e.hasErrors()) {
            // Check for valid CWL file
            GithubDetails githubInfo = githubService.detailsFromFileURL(form.getUrl());
            if (githubInfo != null) {
                try {
                    // Downloads the workflow file to check for existence
                    if (githubService.downloadFile(githubInfo) != null) {
                        return githubInfo;
                    }
                } catch (IOException ex) {
                    logger.error("Given URL " + form.getUrl() + " was not a .cwl file");
                    e.rejectValue("githubURL", "githubURL.missingWorkflow", "Workflow was not found at the given Github URL");
                }
            } else {
                // Check for valid Github directory
                githubInfo = githubService.detailsFromDirURL(form.getUrl());
                if (githubInfo != null) {
                   return githubInfo;
                } else {
                    logger.error("The Github URL " + form.getUrl() + " is not valid");
                    e.rejectValue("githubURL", "githubURL.invalid", "You must enter a valid Github URL to a .cwl file");
                }
            }
        } else {
            logger.error("Github URL is empty");
        }

        // Errors will stop this being used anyway
        return null;
    }
}