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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.workflow.WorkflowForm;
import org.springframework.validation.Errors;

/**
 * Abstract implementation of a Git validator.
 *
 * <p>Implementations must provide methods to retrieve the host and the base URL.
 */
public abstract class AbstractGitValidator implements GitUrlValidator {

  /**
   * Git host (e.g., github.com).
   *
   * @return Git host
   */
  protected abstract String host();

  /**
   * Git repository base URL (e.g., <a href="https://github.com/">...</a><owner>/<repo>.git).
   *
   * @param owner owner or organisation
   * @param repo repository name
   * @return a string representation of the Git repository base URL
   */
  protected abstract String repoBaseUrl(String owner, String repo);

  @Override
  public boolean supports(String url) {
    try {
      URI uri = URI.create(url);
      return host().equalsIgnoreCase(uri.getHost());
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public void validate(WorkflowForm form, Errors errors) {}

  @Override
  public GitDetails parse(String url, WorkflowForm form) {
    final URI uri = URI.create(url);

    List<String> parts = Arrays.stream(uri.getPath().split("/")).filter(p -> !p.isBlank()).toList();

    if (parts.size() < 2) return null;

    String owner = parts.get(0);
    String repo = parts.get(1);

    String repoUrl = repoBaseUrl(owner, repo);

    String branch = null;
    String path = null;

    // Detect branch/path from /tree/ or /blob/
    for (int i = 2; i < parts.size(); i++) {
      String p = parts.get(i);

      if ("tree".equals(p) || "blob".equals(p)) {
        if (i + 1 < parts.size()) {
          branch = parts.get(i + 1);
        }
        if (i + 2 < parts.size()) {
          path = String.join("/", parts.subList(i + 2, parts.size()));
        }
        break;
      }
    }

    // Optional GitHub-style fragment fallback (#branch/path)
    String fragment = uri.getFragment();
    if (fragment != null && !fragment.isBlank()) {
      String[] fragParts = fragment.split("/", 2);
      if (branch == null) {
        branch = fragParts[0];
      }
      if (path == null && fragParts.length > 1) {
        path = fragParts[1];
      }
    }

    if (form != null) {
      if (StringUtils.isNotBlank(form.getBranch())) {
        branch = form.getBranch();
      }
      if (StringUtils.isNotBlank(form.getPath())) {
        path = form.getPath();
      }
    }

    return new GitDetails(repoUrl, branch, path);
  }
}
