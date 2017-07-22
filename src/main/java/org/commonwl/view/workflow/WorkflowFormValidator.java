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

import org.commonwl.view.git.GitDetails;
import org.commonwl.view.git.GitService;
import org.commonwl.view.git.GitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs validation on the workflow form from the main page
 */
@Component
public class WorkflowFormValidator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // URL validation for cwl files on Github
    private static final String GITHUB_CWL_REGEX = "^https?:\\/\\/github\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:tree|blob)\\/([^/]+)(?:\\/(.+\\.cwl))$";
    private static final Pattern githubCwlPattern = Pattern.compile(GITHUB_CWL_REGEX);

    // URL validation for cwl files on Gitlab
    private static final String GITLAB_CWL_REGEX = "^https?:\\/\\/gitlab\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:tree|blob)\\/([^/]+)(?:\\/(.+\\.cwl))$";
    private static final Pattern gitlabCwlPattern = Pattern.compile(GITLAB_CWL_REGEX);

    // Git URL validation
    private static final String GIT_REPO_REGEX = "^((git|ssh|http(s)?)|(git@[\\w\\.]+))(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?$";
    private static final Pattern gitRepoPattern = Pattern.compile(GIT_REPO_REGEX);

    /**
     * Github API service
     */
    private final GitService githubService;

    @Autowired
    public WorkflowFormValidator(GitService githubService) {
        this.githubService = githubService;
    }

    /**
     * Validates a WorkflowForm to ensure the URL is not empty and links to a cwl file
     * @param form The given WorkflowForm
     * @param e Any errors from validation
     */
    public GitDetails validateAndParse(WorkflowForm form, Errors e) {
        ValidationUtils.rejectIfEmptyOrWhitespace(e, "url", "url.emptyOrWhitespace");

        // If not null and isn't just whitespace
        if (!e.hasErrors()) {

            // Object to be returned if valid Git details are found
            GitDetails gitDetails = null;

            // Github URL
            Matcher m = githubCwlPattern.matcher(form.getUrl());
            if (m.find()) {
                String repoUrl = "https://github.com/" + m.group(1) + "/" + m.group(2) + ".git";
                return new GitDetails(repoUrl, m.group(3), m.group(4), GitType.GITHUB);
            }

            // Gitlab URL
            m = gitlabCwlPattern.matcher(form.getUrl());
            if (m.find()) {
                String repoUrl = "https://gitlab.com/" + m.group(1) + "/" + m.group(2) + ".git";
                return new GitDetails(repoUrl, m.group(3), m.group(4), GitType.GITLAB);
            }

            // General Git details if didn't match the above
            ValidationUtils.rejectIfEmptyOrWhitespace(e, "branch", "branch.emptyOrWhitespace");
            ValidationUtils.rejectIfEmptyOrWhitespace(e, "path", "path.emptyOrWhitespace");
            m = gitRepoPattern.matcher(form.getUrl());
            if (m.find()) {
                return new GitDetails(form.getUrl(), form.getBranch(), form.getPath(), GitType.GENERIC);
            }

        }

        // Errors will stop this being used anyway
        return null;
    }
}