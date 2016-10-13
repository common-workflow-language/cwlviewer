package org.researchobject.domain;

import org.apache.taverna.cwl.utilities.PortDetail;

import java.util.Map;

/**
 * Representation of a workflow
 */
public class Workflow {

    private String label;
    private String doc;
    private Map<String, InputOutput> inputs;
    private Map<String, InputOutput> outputs;

    public Workflow(String label, String doc, Map<String, InputOutput> inputs, Map<String, InputOutput> outputs) {
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

    public Map<String, InputOutput> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, InputOutput> inputs) {
        this.inputs = inputs;
    }

    public Map<String, InputOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, InputOutput> outputs) {
        this.outputs = outputs;
    }
}
