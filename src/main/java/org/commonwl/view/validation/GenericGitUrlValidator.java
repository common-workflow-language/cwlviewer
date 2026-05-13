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

package org.commonwl.view.validation;

import org.commonwl.view.git.GitDetails;
import org.commonwl.view.workflow.WorkflowForm;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/** Validate generic Git repository URLs. */
public class GenericGitUrlValidator implements GitUrlValidator {

  @Override
  public boolean supports(String url) {
    return url != null && url.endsWith(".git");
  }

  @Override
  public void validate(WorkflowForm form, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "branch", "branch.emptyOrWhitespace");
  }

  @Override
  public GitDetails parse(String url, WorkflowForm form) {
    String branch = "master";
    String path = "/";

    if (form != null) {
      if (form.getBranch() != null && !form.getBranch().isBlank()) {
        branch = form.getBranch();
      }

      if (form.getPath() != null && !form.getPath().isBlank()) {
        path = form.getPath();
      }
    }

    return new GitDetails(url, branch, path);
  }
}
