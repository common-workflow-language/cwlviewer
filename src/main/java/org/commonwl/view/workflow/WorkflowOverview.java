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
 * Gives an overview of a workflow
 */
public class WorkflowOverview {

    private final String fileName;
    private final String label;
    private final String doc;

    public WorkflowOverview(String fileName, String label, String doc) {
        this.fileName = fileName;
        this.label = label;
        this.doc = doc;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLabel() {
        return label;
    }

    public String getDoc() {
        return doc;
    }

}