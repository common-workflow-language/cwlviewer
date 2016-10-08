package org.researchobject.domain;

import org.apache.taverna.cwl.utilities.PortDetail;

import java.util.Map;

/**
 * Representation of a workflow
 */
public class Workflow {

    private String label;
    private String doc;
    private Map<String, PortDetail> inputs;
    private Map<String, PortDetail> outputs;

    public Workflow(String label, String doc, Map<String, PortDetail> inputs, Map<String, PortDetail> outputs) {
        this.label = label;
        this.doc = doc;
        this.inputs = inputs;
        this.outputs = outputs;
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

    public Map<String, PortDetail> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, PortDetail> inputs) {
        this.inputs = inputs;
    }

    public Map<String, PortDetail> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, PortDetail> outputs) {
        this.outputs = outputs;
    }
}
