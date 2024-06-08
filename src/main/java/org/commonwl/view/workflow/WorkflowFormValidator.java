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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.commonwl.view.git.GitDetails;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/** Runs validation on the workflow form from the main page */
@Component
public class WorkflowFormValidator {

  // URL validation for cwl files on github.com
  private static final String GITHUB_CWL_REGEX =
      "^https?:\\/\\/github\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:tree|blob)\\/([^/]+)(?:\\/(.+\\.cwl))$";
  private static final Pattern githubCwlPattern = Pattern.compile(GITHUB_CWL_REGEX);

  // URL validation for directories on github.com
  private static final String GITHUB_DIR_REGEX =
      "^https?:\\/\\/github\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:(?:tree|blob)\\/([^/]+)\\/?(.*)?)?$";
  private static final Pattern githubDirPattern = Pattern.compile(GITHUB_DIR_REGEX);

  // URL validation for cwl files on gitlab.com
  private static final String GITLAB_CWL_REGEX =
      "^https?:\\/\\/gitlab\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:tree|blob)\\/([^/]+)(?:\\/(.+\\.cwl))$";
  private static final Pattern gitlabCwlPattern = Pattern.compile(GITLAB_CWL_REGEX);

  // URL validation for directories on gitlab.com
  private static final String GITLAB_DIR_REGEX =
      "^https?:\\/\\/gitlab\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:(?:tree|blob)\\/([^/]+)\\/?(.*)?)?$";
  private static final Pattern gitlabDirPattern = Pattern.compile(GITLAB_DIR_REGEX);

  // Generic Git URL validation
  private static final String GIT_REPO_REGEX =
      "^((git|ssh|http(s)?)|(git@[\\w\\.]+))(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?$";
  private static final Pattern gitRepoPattern = Pattern.compile(GIT_REPO_REGEX);

  /**
   * Validates a WorkflowForm to ensure the URL is not empty and links to a cwl file
   *
   * @param form The given WorkflowForm
   * @param e Any errors from validation
   */
  public GitDetails validateAndParse(WorkflowForm form, Errors e) {
    ValidationUtils.rejectIfEmptyOrWhitespace(e, "url", "url.emptyOrWhitespace");

    // If not null and isn't just whitespace
    if (!e.hasErrors()) {

      // Override if specific branch or path is given in the form
      String repoUrl = null;
      String branch = null;
      String path = null;
      String packedId = null;
      if (!isEmptyOrWhitespace(form.getBranch())) {
        branch = form.getBranch();
      }
      if (!isEmptyOrWhitespace(form.getPath())) {
        path = form.getPath();
      }
      if (!isEmptyOrWhitespace(form.getPackedId())) {
        packedId = form.getPackedId();
      }

      // github.com URL
      Matcher m = githubCwlPattern.matcher(form.getUrl());
      if (m.find()) {
        repoUrl = "https://github.com/" + m.group(1) + "/" + m.group(2) + ".git";
        if (branch == null) branch = m.group(3);
        if (path == null) path = m.group(4);
      }

      // gitlab.com URL
      m = gitlabCwlPattern.matcher(form.getUrl());
      if (m.find()) {
        repoUrl = "https://gitlab.com/" + m.group(1) + "/" + m.group(2) + ".git";
        if (branch == null) branch = m.group(3);
        if (path == null) path = m.group(4);
      }

      // github.com Dir URL
      m = githubDirPattern.matcher(form.getUrl());
      if (m.find() && !m.group(2).endsWith(".git")) {
        repoUrl = "https://github.com/" + m.group(1) + "/" + m.group(2) + ".git";
        if (branch == null) branch = m.group(3);
        if (path == null) path = m.group(4);
      }

      // gitlab.com Dir URL
      m = gitlabDirPattern.matcher(form.getUrl());
      if (m.find() && !m.group(2).endsWith(".git")) {
        repoUrl = "https://gitlab.com/" + m.group(1) + "/" + m.group(2) + ".git";
        if (branch == null) branch = m.group(3);
        if (path == null) path = m.group(4);
      }

      // Split off packed ID if present
      if (repoUrl != null) {
        GitDetails details = new GitDetails(repoUrl, branch, path);
        if (packedId != null) {
          details.setPackedId(packedId);
        } else {
          String[] pathSplit = path.split("#");
          if (pathSplit.length > 1) {
            details.setPath(pathSplit[pathSplit.length - 2]);
            details.setPackedId(pathSplit[pathSplit.length - 1]);
          }
        }
        return details;
      }

      // General Git details if didn't match the above
      ValidationUtils.rejectIfEmptyOrWhitespace(e, "branch", "branch.emptyOrWhitespace");
      if (!e.hasErrors()) {
        m = gitRepoPattern.matcher(form.getUrl());
        if (m.find()) {
          GitDetails details = new GitDetails(form.getUrl(), form.getBranch(), form.getPath());
          details.setPackedId(form.getPackedId());
          return details;
        }
      }
    }

    // Errors will stop this being used anyway
    return null;
  }

  /**
   * Checks if a string is empty or whitespace
   *
   * @param str The string to be checked
   * @return Whether the string is empty or whitespace
   */
  private boolean isEmptyOrWhitespace(String str) {
    return (str == null || str.length() == 0 || StringUtils.isWhitespace(str));
  }
}
