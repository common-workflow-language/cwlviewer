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

package org.commonwl.view.graphviz;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.commonwl.view.cwl.CWLProcess;
import org.commonwl.view.cwl.RDFService;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes GraphViz DOT files from a workflow RDF model
 */
public class RDFDotWriter extends DotWriter {

    private final IRIFactory iriFactory = IRIFactory.iriImplementation();

    private RDFService rdfService;
    private String gitPath;

    public RDFDotWriter(Writer writer, RDFService rdfService, String gitPath) {
        super(writer);
        this.rdfService = rdfService;
        this.gitPath = gitPath;
    }

    /**
     * Write a graph representing a workflow to the Writer
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurred
     */
    public void writeGraph(String workflowUri) throws IOException {
        writePreamble();
        writeInputs(workflowUri);
        writeOutputs(workflowUri);
        writeSteps(workflowUri, false);
        writeStepLinks(workflowUri);
        writeLine("}");
    }

    /**
     * Writes a set of inputs from a workflow to the Writer
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeInputs(String workflowUri) throws IOException {

        // Start of subgraph with styling
        writeLine("  subgraph cluster_inputs {");
        writeLine("    rank = \"same\";");
        writeLine("    style = \"dashed\";");
        writeLine("    label = \"Workflow Inputs\";");

        // Write each of the inputs as a node
        ResultSet inputs = rdfService.getInputs(workflowUri);
        while (inputs.hasNext()) {
            QuerySolution input = inputs.nextSolution();
            writeInputOutput(input);
        }

        // End subgraph
        writeLine("  }");
    }

    /**
     * Writes a set of outputs from a workflow to the Writer
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeOutputs(String workflowUri) throws IOException {
        // Start of subgraph with styling
        writeLine("  subgraph cluster_outputs {");
        writeLine("    rank = \"same\";");
        writeLine("    style = \"dashed\";");
        writeLine("    labelloc = \"b\";");
        writeLine("    label = \"Workflow Outputs\";");

        // Write each of the outputs as a node
        ResultSet outputs = rdfService.getOutputs(workflowUri);
        while (outputs.hasNext()) {
            QuerySolution output = outputs.nextSolution();
            writeInputOutput(output);
        }

        // End subgraph
        writeLine("  }");
    }

    /**
     * Writes a set of steps from a workflow to the Writer
     * @param workflowUri The URI of the workflow in the model
     * @param subworkflow Whether these are steps of a subworkflow
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeSteps(String workflowUri, boolean subworkflow) throws IOException {

        ResultSet steps = rdfService.getSteps(workflowUri);
        Set<String> addedSteps = new HashSet<>();
        while (steps.hasNext()) {
            QuerySolution step = steps.nextSolution();
            String stepName = rdfService.stepNameFromURI(gitPath, step.get("step").toString());

            // Only write each step once
            if (!addedSteps.contains(stepName)) {
                String label;
                if (step.contains("label")) {
                    label = step.get("label").toString();
                } else {
                    label = rdfService.labelFromName(stepName);
                }

                // Distinguish nested workflows
                CWLProcess runType = rdfService.strToRuntype(step.get("runtype").toString());
                if (runType == CWLProcess.WORKFLOW) {
                    writeLine("  \"" + stepName + "\" [label=\"" + label +
                            "\", fillcolor=\"#F3CEA1\"];");
                } else {
                    writeLine("  \"" + stepName + "\" [label=\"" + label + "\"];");
                }
                addedSteps.add(stepName);
            }
        }

    }

    /**
     * Write the links between steps for the entire model
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurredn
     */
    private void writeStepLinks(String workflowUri) throws IOException {
        // Write links between steps
        ResultSet stepLinks = rdfService.getStepLinks(workflowUri);
        int defaultCount = 1;
        while (stepLinks.hasNext()) {
            QuerySolution stepLink = stepLinks.nextSolution();
            if (stepLink.contains("src")) {
                // Normal link from step
                String sourceID = nodeIDFromUri(stepLink.get("src").toString());
                String dest = stepLink.get("dest").toString();
                String destID = nodeIDFromUri(dest);
                String destInput = dest.substring(dest.replaceAll("#", "/").lastIndexOf("/") + 1);
                writeLine("  \"" + sourceID + "\" -> \"" + destID + "\" [label=\"" + destInput + "\"];");
            } else if (stepLink.contains("default")) {
                // Collect default values
                String destID = rdfService.stepNameFromURI(gitPath, stepLink.get("dest").toString());
                String label;
                if (stepLink.get("default").isLiteral()) {
                    label = rdfService.formatDefault(stepLink.get("default").toString());
                } else if (stepLink.get("default").isURIResource()) {
                    IRI workflowPath = iriFactory.construct(workflowUri).resolve("./");
                    IRI resourcePath = iriFactory.construct(stepLink.get("default").asResource().getURI());
                    label = workflowPath.relativize(resourcePath).toString();
                } else {
                    label = "[Complex Object]";
                }

                // Write default
                String dest = stepLink.get("dest").toString();
                String inputName = dest.substring(dest.lastIndexOf("/") + 1);
                writeLine("  \"default" + defaultCount + "\" -> \"" + destID + "\" "
                        + "[label=\"" + inputName + "\"];");
                writeLine("  \"default" + defaultCount + "\" [label=\"" + label
                        + "\", fillcolor=\"#D5AEFC\"];");
                defaultCount++;
            }
        }

        // Write links between steps and outputs
        ResultSet outputLinks = rdfService.getOutputLinks(workflowUri);
        while (outputLinks.hasNext()) {
            QuerySolution outputLink = outputLinks.nextSolution();
            String sourceID = nodeIDFromUri(outputLink.get("src").toString());
            String destID = nodeIDFromUri(outputLink.get("dest").toString());
            writeLine("  \"" + sourceID + "\" -> \"" + destID + "\";");
        }
    }

    /**
     * Get a node ID from a URI, with ID handling for subworkflows
     * @param uri The URI of the step
     * @return A string in the format filename#stepID
     */
    private String nodeIDFromUri(String uri) {
        return rdfService.stepNameFromURI(gitPath, uri);
    }

    /**
     * Writes a single input or output to the Writer
     * @param inputOutput The input or output
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeInputOutput(QuerySolution inputOutput) throws IOException {
        // List of options for this node
        List<String> nodeOptions = new ArrayList<>();
        nodeOptions.add("fillcolor=\"#94DDF4\"");

        // Label for the node
        String label;
        if (inputOutput.contains("label")) {
            label = inputOutput.get("label").toString();
        } else {
            label = rdfService.labelFromName(inputOutput.get("name").toString());
        }
        nodeOptions.add("label=\"" + label + "\"");

        // Write the line for the node
        String inputOutputName = rdfService.stepNameFromURI(gitPath, inputOutput.get("name").toString());
        writeLine("    \"" + inputOutputName + "\" [" + String.join(",", nodeOptions) + "];");
    }

}
