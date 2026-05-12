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
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.workflow.WorkflowForm;

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
  public GitDetails parse(String url, WorkflowForm form) {
    final URI uri = URI.create(url);
    final String[] parts = uri.getPath().split("/");

    if (parts.length < 3) return null;

    final String owner = parts[1];
    final String repo = parts[2];

    final String repoUrl = repoBaseUrl(owner, repo);

    String branch = form.getBranch();
    String path = form.getPath();

    for (int i = 3; i < parts.length; i++) {
      if ("tree".equals(parts[i]) || "blob".equals(parts[i])) {
        if (branch == null && i + 1 < parts.length) {
          branch = parts[i + 1];
        }
        if (path == null && i + 2 < parts.length) {
          path = String.join("/", java.util.Arrays.copyOfRange(parts, i + 2, parts.length));
        }
        break;
      }
    }

    return new GitDetails(repoUrl, branch, path);
  }
}
