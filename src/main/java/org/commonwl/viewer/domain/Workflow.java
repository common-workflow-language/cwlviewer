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

package org.commonwl.viewer.domain;

import org.commonwl.viewer.services.DotWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

/**
 * Representation of a workflow
 */
@Document
public class Workflow {

    static private final Logger logger = LoggerFactory.getLogger(Workflow.class);

    // ID for database
    @Id
    public String id;

    // Metadata
    @Indexed(unique = true)
    private GithubDetails retrievedFrom;
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm z")
    private Date retrievedOn;

    // A String which represents the path to a RO bundle
    // Path types cannot be stored using Spring Data, unfortunately
    private String roBundle;

    // Contents of the workflow
    private String label;
    private String doc;
    private Map<String, CWLElement> inputs;
    private Map<String, CWLElement> outputs;

    // DOT graph of the contents
    private String dotGraph;

    public Workflow(String label, String doc, Map<String, CWLElement> inputs, Map<String, CWLElement> outputs) {
        this.label = label;
        this.doc = doc;
        this.inputs = inputs;
        this.outputs = outputs;

        // Create a DOT graph for this workflow and store it
        StringWriter graphWriter = new StringWriter();
        DotWriter dotWriter = new DotWriter(graphWriter);
        try {
            dotWriter.writeGraph(this);
            this.dotGraph = graphWriter.toString();
        } catch (IOException ex) {
            logger.error("Failed to create DOT graph for workflow: " + ex.getMessage());
        }
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
}
