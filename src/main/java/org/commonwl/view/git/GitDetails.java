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

package org.commonwl.view.git;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import org.commonwl.view.util.LicenseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents all the parameters necessary to access a file/directory with Git */
@JsonIgnoreProperties(
    value = {"internalUrl", "logger"},
    ignoreUnknown = true)
public class GitDetails implements Serializable {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private String repoUrl;
  private String branch;
  private String path;
  private String packedId;

  @JsonCreator
  public GitDetails(String repoUrl, String branch, String path) {
    this.repoUrl = repoUrl;

    // Default to the master branch
    if (branch == null || branch.isEmpty()) {
      // TODO: get default branch name for this rather than assuming master
      this.branch = "master";
    } else {
      this.branch = branch;
    }

    // Default to root path
    setPath(path);
  }

  public String getRepoUrl() {
    return repoUrl;
  }

  public void setRepoUrl(String repoUrl) {
    this.repoUrl = repoUrl;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getPackedId() {
    return packedId;
  }

  public void setPackedId(String packedId) {
    this.packedId = packedId;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    if (path == null || path.isEmpty()) {
      this.path = "/";
    } else if (path.startsWith("/") && path.length() > 1) {
      this.path = path.substring(1);
    } else {
      this.path = path;
    }
  }

  /**
   * Get the type of a repository URL this object refers to
   *
   * @return The type for the URL
   */
  public GitType getType() {
    try {
      URI uri = new URI(repoUrl);
      String domain = uri.getHost();
      if (domain.startsWith("www.")) {
        domain = domain.substring(4);
      }
      return switch (domain) {
        case "github.com" -> GitType.GITHUB;
        case "gitlab.com" -> GitType.GITLAB;
        case "bitbucket.org" -> GitType.BITBUCKET;
        default -> GitType.GENERIC;
      };
    } catch (URISyntaxException ex) {
      return GitType.GENERIC;
    }
  }

  /**
   * Get the URL to the external resource representing this workflow
   *
   * @param branchOverride The branch to use instead of the one in this instance
   * @return The URL
   */
  public String getUrl(String branchOverride) {
    String packedPart = packedId == null ? "" : "#" + packedId;
    return switch (getType()) {
      case GITHUB, GITLAB ->
          "https://"
              + normaliseUrl(repoUrl).replace(".git", "")
              + "/blob/"
              + branchOverride
              + "/"
              + path
              + packedPart;
      case BITBUCKET ->
          "https://"
              + normaliseUrl(repoUrl).replace(".git", "")
              + "/src/"
              + branchOverride
              + "/"
              + path
              + packedPart;
      default -> repoUrl;
    };
  }

  /**
   * Get the URL to the external resource representing this workflow
   *
   * @return The URL
   */
  public String getUrl() {
    return getUrl(branch);
  }

  /**
   * Get the URL to the page containing this workflow
   *
   * @param branchOverride The branch to use instead of the one in this instance
   * @return The URL
   */
  public String getInternalUrl(String branchOverride) {
    String packedPart = packedId == null ? "" : "%23" + packedId;
    String pathPart = path.equals("/") ? "" : "/" + path;
    return switch (getType()) {
      case GITHUB, GITLAB ->
          "/workflows/"
              + normaliseUrl(repoUrl).replace(".git", "")
              + "/blob/"
              + branchOverride
              + pathPart
              + packedPart;
      default ->
          "/workflows/" + normaliseUrl(repoUrl) + "/" + branchOverride + pathPart + packedPart;
    };
  }

  /**
   * Get the URL to the page containing this workflow
   *
   * @return The URL
   */
  public String getInternalUrl() {
    return getInternalUrl(branch);
  }

  /**
   * Get the URL directly to the resource
   *
   * @param branchOverride The branch to use, or <code>null</code> to use this instance's branch
   * @param pathOverride The path to use, or <code>null</code> to use this instance's path
   * @return The URL
   */
  public String getRawUrl(String branchOverride, String pathOverride) {
    if (branchOverride == null) {
      branchOverride = branch;
    }
    if (pathOverride == null) {
      pathOverride = path;
    }
    return switch (getType()) {
      case GITHUB ->
          "https://raw.githubusercontent.com/"
              + normaliseUrl(repoUrl).replace("github.com/", "").replace(".git", "")
              + "/"
              + branchOverride
              + "/"
              + pathOverride;
      case GITLAB, BITBUCKET ->
          "https://"
              + normaliseUrl(repoUrl).replace(".git", "")
              + "/raw/"
              + branchOverride
              + "/"
              + pathOverride;
      default -> repoUrl;
    };
  }

  /**
   * Get the URL directly to the resource
   *
   * @param branchOverride The branch to use instead of the one in this instance
   * @return The URL
   */
  public String getRawUrl(String branchOverride) {
    return getRawUrl(branchOverride, path);
  }

  /**
   * Get the URL directly to the resource
   *
   * @return The URL
   */
  public String getRawUrl() {
    return getRawUrl(branch);
  }

  /**
   * Normalises the URL removing protocol and www.
   *
   * @param url The URL to be normalised
   * @return The normalised URL
   */
  public static String normaliseUrl(String url) {
    return url.replace("http://", "")
        .replace("https://", "")
        .replace("ssh://", "")
        .replace("git://", "")
        .replace("www.", "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GitDetails that = (GitDetails) o;
    return Objects.equals(repoUrl, that.repoUrl)
        && Objects.equals(branch, that.branch)
        && Objects.equals(path, that.path)
        && Objects.equals(packedId, that.packedId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repoUrl, branch, path, packedId);
  }

  public String toSummary() {
    return String.format(
        "repoUrl: %s branch: %s path: %s packedId: %s", repoUrl, branch, path, packedId);
  }

  /**
   * Retrieves license details from the repo, if present.
   *
   * @param workTree the path to the locally cloned repo
   * @return The license URI
   */
  public String getLicense(Path workTree) throws GitLicenseException {
    try {
      String[] command = {"licensee", "detect", "--json", workTree.toString()};
      if (logger.isTraceEnabled()) {
        logger.trace("Calling " + String.join(" ", command));
      }
      Process process = Runtime.getRuntime().exec(command, null);
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonLicenses = mapper.readTree(process.getInputStream());
      if (logger.isTraceEnabled()) {
        logger.trace(
            "Licensee retrieved the following licenses:\n" + jsonLicenses.toPrettyString());
      }
      int size = jsonLicenses.withArray("licenses").size();
      if (size > 0) {
        String licenseCandidate =
            jsonLicenses.withArray("matched_files").get(0).get("filename").asText();
        String licenseLink = getRawUrl(null, licenseCandidate);
        if (logger.isWarnEnabled() && size > 1) {
          logger.warn(
              "There are "
                  + size
                  + " identified license files in the "
                  + repoUrl
                  + " repository. "
                  + "Taking the first one: "
                  + licenseLink);
        }
        String key = jsonLicenses.withArray("licenses").get(0).get("key").asText();
        if (!"other".equals(key)) {
          return LicenseUtils.SPDX_LICENSES_PREFIX
              + jsonLicenses.withArray("licenses").get(0).get("spdx_id").asText();
        } else {
          return licenseLink;
        }
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new GitLicenseException(
          "While attempting to detect license for " + workTree + ": " + e.getMessage(), e);
    }
  }
}
