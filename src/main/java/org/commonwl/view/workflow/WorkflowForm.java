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

/**
 * Represents the submission form on the main page to create a new workflow
 * Contains the URL and optionally branch and path (for Git repository)
 */
public class WorkflowForm {

    private String url;
    private String branch;
    private String path;
    private String packedId;

    public WorkflowForm() {}

    public WorkflowForm(String url) {
        setUrl(url);
    }

    public WorkflowForm(String url, String branch, String path) {
        setUrl(url);
        this.branch = branch;
        this.path = path;
    }

    public WorkflowForm(String url, String branch, String path, String packedId) {
        setUrl(url);
        this.branch = branch;
        this.path = path;
        this.packedId = packedId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url != null) {
            this.url = trimTrailingSlashes(url);
        }
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

    public String getPackedId() {
        return packedId;
    }

    public void setPackedId(String packedId) {
        this.packedId = packedId;
    }

    /**
     * Cuts any trailing slashes off a string
     * @param url The string to cut the slashes off
     * @return The same string without trailing slashes
     */
    private String trimTrailingSlashes(String url) {
        return url.replaceAll("\\/+$", "");
    }

    @Override
    public String toString() {
        return "WorkflowForm [" + (url != null ? "url=" + url + ", " : "")
                 + (branch != null ? "branch=" + branch + ", " : "") + (path != null ? "path=" + path : "") + "]";
    }

}
