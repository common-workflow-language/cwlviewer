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
 * Currently just contains a Github or Gitlab URL
 */
public class WorkflowForm {

    private String url;

    public WorkflowForm() {}

    public WorkflowForm(String url) {
        if (url != null) {
            this.url = trimTrailingSlashes(url);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url != null) {
            this.url = trimTrailingSlashes(url);
        }
    }

    /**
     * Cuts any trailing slashes off a string
     * @param url The string to cut the slashes off
     * @return The same string without trailing slashes
     */
    private String trimTrailingSlashes(String url) {
        return url.replaceAll("\\/+$", "");
    }
}
