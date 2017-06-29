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

import org.commonwl.view.cwl.CWLElement;
import org.commonwl.view.cwl.CWLStep;
import org.commonwl.view.github.GithubDetails;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.Map;

/**
 * Representation of a workflow
 */
@Document
public class Workflow {

    // ID for database
    @Id
    public String id;

    // Metadata
    @Indexed(unique = true)
    private GithubDetails retrievedFrom;
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss z")
    private Date retrievedOn;
    private String packedWorkflowID;

    // The last commit from the branch at the time of fetching
    // Used for caching purposes
    private String lastCommit;

    // A String which represents the path to a RO bundle
    // Path types cannot be stored using Spring Data, unfortunately
    private String roBundle;

    // Contents of the workflow
    private String label;
    private String doc;
    private Map<String, CWLElement> inputs;
    private Map<String, CWLElement> outputs;
    private Map<String, CWLStep> steps;

    // Currently only DockerRequirement is parsed for this
    private String dockerLink;

    // DOT graph of the contents
    private String dotGraph;

    // Cwltool run details
    public enum Status {
        RUNNING, ERROR, SUCCESS
    }
    private Status cwltoolStatus = Status.RUNNING;
    private String cwltoolLog;
    private String cwltoolVersion = "";

    public Workflow(String label, String doc, Map<String, CWLElement> inputs,
                    Map<String, CWLElement> outputs, Map<String, CWLStep> steps, String dockerLink) {
        this.label = label;
        this.doc = doc;
        this.inputs = inputs;
        this.outputs = outputs;
        this.steps = steps;
        this.dockerLink = dockerLink;
    }

    public String getID() { return id; }

    public void setId(String id) {
        this.id = id;
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

    public Map<String, CWLElement> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, CWLElement> inputs) {
        this.inputs = inputs;
    }

    public Map<String, CWLElement> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, CWLElement> outputs) {
        this.outputs = outputs;
    }

    public Map<String, CWLStep> getSteps() {
        return steps;
    }

    public void setSteps(Map<String, CWLStep> steps) {
        this.steps = steps;
    }

    public String getRoBundle() {
        return roBundle;
    }

    public void setRoBundle(String roBundle) {
        this.roBundle = roBundle;
    }

    public GithubDetails getRetrievedFrom() {
        return retrievedFrom;
    }

    public void setRetrievedFrom(GithubDetails retrievedFrom) {
        this.retrievedFrom = retrievedFrom;
    }

    public Date getRetrievedOn() {
        return retrievedOn;
    }

    public void setRetrievedOn(Date retrievedOn) {
        this.retrievedOn = retrievedOn;
    }

    public String getDotGraph() {
        return dotGraph;
    }

    public void setDotGraph(String dotGraph) {
        this.dotGraph = dotGraph;
    }

    public String getLastCommit() {
        return lastCommit;
    }

    public void setLastCommit(String lastCommit) {
        this.lastCommit = lastCommit;
    }

    public String getDockerLink() {
        return dockerLink;
    }

    public void setDockerLink(String dockerLink) {
        this.dockerLink = dockerLink;
    }

    public Status getCwltoolStatus() {
        return cwltoolStatus;
    }

    public void setCwltoolStatus(Status cwltoolStatus) {
        this.cwltoolStatus = cwltoolStatus;
    }

    public String getCwltoolLog() {
        return cwltoolLog;
    }

    public void setCwltoolLog(String cwltoolLog) {
        this.cwltoolLog = cwltoolLog;
    }

    public String getPackedWorkflowID() {
        return packedWorkflowID;
    }

    public void setPackedWorkflowID(String packedWorkflowID) {
        this.packedWorkflowID = packedWorkflowID;
    }

    public String getCwltoolVersion() {
        return cwltoolVersion;
    }

    public void setCwltoolVersion(String cwltoolVersion) {
        this.cwltoolVersion = cwltoolVersion;
    }

    public String getVisualisationPng() {
        return "/graph/png/" + retrievedFrom.getURL().replace("https://", "");
    }

    public String getVisualisationSvg() {
        return "/graph/svg/" + retrievedFrom.getURL().replace("https://", "");
    }
}
