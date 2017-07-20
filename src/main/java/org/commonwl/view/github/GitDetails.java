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

package org.commonwl.view.github;

import java.io.Serializable;

/**
 * Represents all the parameters necessary to access a file/directory with Git
 */
public class GitDetails implements Serializable {

    private String repoUrl;
    private String branch;
    private String path;
    private GitType type;

    public GitDetails(String repoUrl, String branch, String path, GitType type) {
        this.repoUrl = repoUrl;

        // Default to the master branch
        if (branch == null || branch.isEmpty()) {
            // TODO: get default branch name for this rather than assuming master
            this.branch = "master";
        } else {
            this.branch = branch;
        }

        // Default to root path
        if (path == null || path.isEmpty()) {
            this.path = "/";
        } else {
            this.path = path;
        }

        this.type = type;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public GitType getType() {
        return type;
    }

    public void setType(GitType type) {
        this.type = type;
    }

    /**
     * Get the URL to the external resource representing this workflow
     * @return The URL
     */
    public String getUrl() {
        switch (this.type) {
            case GENERIC:
                return repoUrl;
            case GITHUB:
            case GITLAB:
                return "https://" + normaliseURL(repoUrl).replace(".git", "") + "/" + branch + "/" + path;
            default:
                return null;
        }
    }

    /**
     * Get the URL to the page containing this workflow
     * @return The URL
     */
    public String getInternalUrl() {
        switch (this.type) {
            case GENERIC:
                return "/workflows/" + normaliseURL(repoUrl) + "/" + branch + "/" + path;
            case GITHUB:
            case GITLAB:
                return "/workflows/" + normaliseURL(repoUrl).replace(".git", "") + "/" + branch + "/" + path;
            default:
                return null;
        }
    }

    /**
     * Normalises the URL removing protocol and www.
     * @param url The URL to be normalised
     * @return The normalised URL
     */
    private String normaliseURL(String url) {
        return url.replace("http://", "")
                  .replace("https://", "")
                  .replace("ssh://", "")
                  .replace("git://", "")
                  .replace("www.", "");
    }

}
