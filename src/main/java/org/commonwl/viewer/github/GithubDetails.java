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

package org.commonwl.viewer.github;

import java.io.Serializable;

/**
 * Represents all the parameters necessary to access a file/directory in Github
 */
public class GithubDetails implements Serializable {

    private String owner;
    private String repoName;
    private String branch;
    private String path;

    public GithubDetails(String owner, String repoName, String branch, String path) {
        this.owner = owner;
        this.repoName = repoName;
        this.path = path;

        // Default to the master branch
        if (branch == null || branch.isEmpty()) {
            // TODO: get default branch name for this rather than assuming master
            this.branch = "master";
        } else {
            this.branch = branch;
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
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

    /**
     * Get the link to the place on github this represents
     * @return The Github URL including branch and path if given
     */
    public String getURL() {
        return getURL(branch);
    }

    /**
     * Get the link to the place on github this represents with set branch name/commit ID
     * @return The Github URL including commit ID and path if given
     */
    public String getURL(String ref) {
        String url = "https://github.com/" + owner + "/" + repoName + "/tree/" + ref;
        if (path != null) {
            url += "/" + this.path;
        }
        return url;
    }
}
