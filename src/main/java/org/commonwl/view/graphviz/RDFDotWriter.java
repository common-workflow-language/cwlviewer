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

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.commonwl.view.cwl.CWLProcess;
import org.commonwl.view.cwl.RDFService;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Writes GraphViz DOT files from a workflow RDF model
 */
public class RDFDotWriter extends DotWriter {

    private RDFService rdfService;
    private Map<String, String> subworkflows = new HashMap<>();

    public RDFDotWriter(Writer writer, RDFService rdfService) {
        super(writer);
        this.rdfService = rdfService;
    }

    /**
     * Write a graph representing a workflow to the Writer
     * @param rdfModel The model containing the workflow to be graphed
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurred
     */
    public void writeGraph(Model rdfModel, String workflowUri) throws IOException {
        writePreamble();
        writeInputs(rdfModel, workflowUri);
        writeOutputs(rdfModel, workflowUri);
        writeSteps(rdfModel, workflowUri, false);
        writeStepLinks(rdfModel, workflowUri);
        writeLine("}");
    }

    /**
     * Writes a set of inputs from a workflow to the Writer
     * @param rdfModel The model containing the workflow to be graphed
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeInputs(Model rdfModel, String workflowUri) throws IOException {

        // Start of subgraph with styling
        writeLine("  subgraph cluster_inputs {");
        writeLine("    rank = \"same\";");
        writeLine("    style = \"dashed\";");
        writeLine("    label = \"Workflow Inputs\";");

        // Write each of the inputs as a node
        ResultSet inputs = rdfService.getInputs(rdfModel, workflowUri);
        while (inputs.hasNext()) {
            QuerySolution input = inputs.nextSolution();
            writeInputOutput(input);
        }

        // End subgraph
        writeLine("  }");
    }

    /**
     * Writes a set of outputs from a workflow to the Writer
     * @param rdfModel The model containing the workflow to be graphed
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeOutputs(Model rdfModel, String workflowUri) throws IOException {
        // Start of subgraph with styling
        writeLine("  subgraph cluster_outputs {");
        writeLine("    rank = \"same\";");
        writeLine("    style = \"dashed\";");
        writeLine("    label = \"Workflow Outputs\";");

        // Write each of the outputs as a node
        ResultSet outputs = rdfService.getOutputs(rdfModel, workflowUri);
        while (outputs.hasNext()) {
            QuerySolution output = outputs.nextSolution();
            writeInputOutput(output);
        }

        // End subgraph
        writeLine("  }");
    }

    /**
     * Writes a set of steps from a workflow to the Writer
     * @param rdfModel The model containing the workflow to be graphed
     * @param workflowUri The URI of the workflow in the model
     * @param subworkflow Whether these are steps of a subworkflow
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeSteps(Model rdfModel, String workflowUri, boolean subworkflow) throws IOException {

        ResultSet steps = rdfService.getSteps(rdfModel, workflowUri);
        Set<String> addedSteps = new HashSet<>();
        while (steps.hasNext()) {
            QuerySolution step = steps.nextSolution();
            String stepName = rdfService.lastURIPart(step.get("step").toString());

            // Only write each step once
            if (!addedSteps.contains(stepName)) {
                // Distinguish nested workflows
                CWLProcess runType = rdfService.strToRuntype(step.get("runtype").toString());
                if (runType == CWLProcess.WORKFLOW) {
                    //if (subworkflow) {
                        writeLine("  \"" + stepName + "\" [fillcolor=\"#F3CEA1\"];");
                    /*} else {
                        String runFile = step.get("run").toString();
                        subworkflows.put(stepName, FilenameUtils.getName(runFile));
                        writeSubworkflow(stepName, rdfModel, runFile);
                    }*/
                } else {
                    String label = rdfService.labelFromName(stepName);
                    writeLine("  \"" + stepName + "\" [label=\"" + label + "\"];");
                }
                addedSteps.add(stepName);
            }
        }

    }

    /**
     * Write the links between steps for the entire model
     * @param rdfModel The model containing workflows and tools
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurredn
     */
    private void writeStepLinks(Model rdfModel, String workflowUri) throws IOException {

        // Write links between steps
        ResultSet stepLinks = rdfService.getStepLinks(rdfModel);
        int defaultCount = 0;
        while (stepLinks.hasNext()) {
            QuerySolution stepLink = stepLinks.nextSolution();
            if (stepLink.contains("src")) {
                // Normal link from step
                String sourceID = nodeIDFromUri(stepLink.get("src").toString());
                String destID = nodeIDFromUri(stepLink.get("dest").toString());
                writeLine("  \"" + sourceID + "\" -> \"" + destID + "\";");
            } else if (stepLink.contains("default")) {
                // Default values
                defaultCount++;
                String defaultVal = rdfService.formatDefault(stepLink.get("default").toString());
                String destID = rdfService.lastURIPart(stepLink.get("dest").toString());
                String label;
                if (stepLink.get("default").isLiteral()) {
                    label = "\\\"" + defaultVal + "\\\"";
                } else if (stepLink.get("default").isURIResource()) {
                    Path workflowPath = Paths.get(FilenameUtils.getPath(workflowUri));
                    Path resourcePath = Paths.get(defaultVal);
                    label = workflowPath.relativize(resourcePath).toString();
                } else {
                    label = "[Complex Object]";
                }
                writeLine("  \"default" + defaultCount + "\" -> \"" + destID + "\";");
                writeLine("  \"default" + defaultCount + "\" [label=\"" + label + "\", fillcolor=\"#D5AEFC\"];");
            }
        }

    }

    /**
     * Get a node ID from a URI, with ID handling for subworkflows
     * @param uri The URI of the step
     * @return A string in the format filename#stepID
     */
    private String nodeIDFromUri(String uri) {

        String nodeID = rdfService.lastURIPart(uri);
        if (subworkflows.containsKey(nodeID)) {
            int slashAfterHashIndex = uri.indexOf('/', uri.lastIndexOf('#'));
            if (slashAfterHashIndex != -1) {
                String subworkflowStepID = uri.substring(slashAfterHashIndex + 1);
                nodeID = subworkflows.get(nodeID) + "#" + subworkflowStepID;
            }
        }
        return nodeID;
    }

    /**
     * Writes a subworkflow as a cluster on the graph
     * @param name The name of the step the subworkflow is running in
     * @param rdfModel The model containing the workflow to be graphed
     * @param subWorkflowUri The URI of the subworkflow in the model
     */
    private void writeSubworkflow(String name, Model rdfModel, String subWorkflowUri) throws IOException {

        // Start of subgraph with styling
        writeLine("  subgraph \"cluster_" + name + "\" {");
        writeLine("    rank = \"same\";");
        writeLine("    style=\"filled,dotted\";");
        writeLine("    color=\"black\";");
        writeLine("    fillcolor=\"#F9DBB7\";");
        writeLine("    label = \"" + rdfService.labelFromName(name) + "\";");

        writeInputs(rdfModel, subWorkflowUri);
        writeSteps(rdfModel, subWorkflowUri, true);
        writeOutputs(rdfModel, subWorkflowUri);

        writeLine("  }");
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
        String inputOutputName = rdfService.lastURIPart(inputOutput.get("name").toString());
        writeLine("    \"" + inputOutputName + "\" [" + String.join(",", nodeOptions) + "];");
    }

}
