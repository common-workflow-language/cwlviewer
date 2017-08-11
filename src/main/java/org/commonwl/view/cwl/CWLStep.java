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

package org.commonwl.view.cwl;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Represents a step of a workflow
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CWLStep {

    private String label;
    private String doc;
    private String run;
    private CWLProcess runType;
    private Map<String, CWLElement> sources;

    public CWLStep() {
    }

    public CWLStep(String label, String doc, String run,
                   Map<String, CWLElement> sources) {
        this.label = label;
        this.doc = doc;
        this.run = run;
        this.sources = sources;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public String getRun() {
        return run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public CWLProcess getRunType() {
        return runType;
    }

    public void setRunType(CWLProcess runType) {
        this.runType = runType;
    }

    public Map<String, CWLElement> getSources() {
        return sources;
    }

    public void setSources(Map<String, CWLElement> sources) {
        this.sources = sources;
    }

}
