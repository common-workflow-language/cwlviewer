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

import java.net.URI;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.validation.GenericGitUrlValidator;
import org.commonwl.view.validation.GitHubUrlValidator;
import org.commonwl.view.validation.GitLabUrlValidator;
import org.commonwl.view.validation.GitUrlValidator;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/** Runs validation on the workflow form from the main page */
@Component
public class WorkflowFormValidator {

  /**
   * Validates a WorkflowForm to ensure the URL is not empty and links to a cwl file
   *
   * @param form The given WorkflowForm
   * @param e Any errors from validation
   */
  public GitDetails validateAndParse(WorkflowForm form, Errors e) {

    ValidationUtils.rejectIfEmptyOrWhitespace(e, "url", "url.emptyOrWhitespace");

    if (e.hasErrors()) {
      return null;
    }

    List<GitUrlValidator> handlers =
        List.of(new GitHubUrlValidator(), new GitLabUrlValidator(), new GenericGitUrlValidator());

    for (GitUrlValidator handler : handlers) {
      if (handler.supports(form.getUrl())) {
        handler.validate(form, e);

        GitDetails details = handler.parse(form.getUrl(), form);
        if (details != null) {
          attachPackedId(details, form);
          return details;
        }
      }
    }

    ValidationUtils.rejectIfEmptyOrWhitespace(e, "branch", "branch.emptyOrWhitespace");
    return null;
  }

  /**
   * Attaches a packed workflow ID into the Git details.
   *
   * <p>If no workflow is packed in the request, it searches for the information about the workflow
   * in the URL.
   *
   * @param details Git details
   * @param form Workflow form
   */
  private void attachPackedId(GitDetails details, WorkflowForm form) {
    if (isNotEmptyOrWhitespace(form.getPackedId())) {
      details.setPackedId(form.getPackedId());
      return;
    }

    URI uri = URI.create(form.getUrl());

    String fragment = uri.getFragment();

    if (isNotEmptyOrWhitespace(fragment)) {
      details.setPackedId(fragment);
    }
  }

  /**
   * Checks if a string is empty or whitespace
   *
   * @param str The string to be checked
   * @return Whether the string is empty or whitespace
   */
  private boolean isNotEmptyOrWhitespace(String str) {
    return (str != null && !str.isEmpty() && !StringUtils.isWhitespace(str));
  }
}
