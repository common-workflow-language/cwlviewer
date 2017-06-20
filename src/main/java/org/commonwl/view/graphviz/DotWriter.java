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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes GraphViz DOT files from Workflows
 */
public class DotWriter {

    private static final String EOL = System.getProperty("line.separator");
    private Writer writer;
    private RDFService rdfService;

    public DotWriter(Writer writer, RDFService rdfService) {
        this.writer = writer;
        this.rdfService = rdfService;
    }

    /**
     * Write a graph representing a workflow to the Writer
     * @param rdfModel The model containing the workflow to be graphed
     * @param workflowUri The URI of the workflow in the model
     * @throws IOException Any errors in writing which may have occurred
     */
    public void writeGraph(Model rdfModel, String workflowUri) throws IOException {

        /**
         * DOT graph styling is based on the Apache
         * Taverna workflow management system
         */
        // Begin graph
        writeLine("digraph workflow {");

        // Overall graph style
        writeLine("  graph [");
        writeLine("    bgcolor = \"#eeeeee\"");
        writeLine("    color = \"black\"");
        writeLine("    fontsize = \"10\"");
        writeLine("    labeljust = \"left\"");
        writeLine("    clusterrank = \"local\"");
        writeLine("    ranksep = \"0.22\"");
        writeLine("    nodesep = \"0.05\"");
        writeLine("  ]");

        // Overall node style
        writeLine("  node [");
        writeLine("    fontname = \"Helvetica\"");
        writeLine("    fontsize = \"10\"");
        writeLine("    fontcolor = \"black\"");
        writeLine("    shape = \"record\"");
        writeLine("    height = \"0\"");
        writeLine("    width = \"0\"");
        writeLine("    color = \"black\"");
        writeLine("    fillcolor = \"lightgoldenrodyellow\"");
        writeLine("    style = \"filled\"");
        writeLine("  ];");

        // Overall edge style
        writeLine("  edge [");
        writeLine("    fontname=\"Helvetica\"");
        writeLine("    fontsize=\"8\"");
        writeLine("    fontcolor=\"black\"");
        writeLine("    color=\"black\"");
        writeLine("    arrowsize=\"0.7\"");
        writeLine("  ];");

        // Write inputs as a subgraph
        writeInputs(rdfModel, workflowUri);

        // Write outputs as a subgraph
        writeOutputs(rdfModel, workflowUri);

        // Write steps as nodes
        writeSteps(rdfModel, workflowUri);

        // End graph
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
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeSteps(Model rdfModel, String workflowUri) throws IOException {

        ResultSet steps = rdfService.getSteps(rdfModel, workflowUri);
        while (steps.hasNext()) {
            QuerySolution step = steps.nextSolution();
            Set<String> addedSteps = new HashSet<>();
            String stepName = rdfService.stepFromURI(step.get("step").toString());

            // Only write each step once
            if (!addedSteps.contains(stepName)) {
                // Distinguish nested workflows
                CWLProcess runType = rdfService.strToRuntype(step.get("runtype").toString());
                if (runType == CWLProcess.WORKFLOW) {
                    writeLine("  \"" + stepName + "\" [fillcolor=\"#F3CEA1\"];");
                } else {
                    writeLine("  \"" + stepName + "\";");
                }
                addedSteps.add(stepName);
            }
        }

        // Write links between steps
        ResultSet stepLinks = rdfService.getStepLinks(rdfModel, workflowUri);
        int defaultCount = 0;
        while (stepLinks.hasNext()) {
            QuerySolution stepLink = stepLinks.nextSolution();
            if (stepLink.contains("src")) {
                // Normal link from step
                String sourceID = rdfService.stepFromURI(stepLink.get("src").toString());
                String destID = rdfService.stepFromURI(stepLink.get("dest").toString());
                writeLine("  \"" + sourceID + "\" -> \"" + destID + "\";");
            } else if (stepLink.contains("default")) {
                // Default values
                defaultCount++;
                String defaultVal = rdfService.formatDefault(stepLink.get("default").toString());
                String destID = rdfService.stepFromURI(stepLink.get("dest").toString());
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
                writeLine("  \"default" + defaultCount + "\" [label=\"" + label + "\", fillcolor=\"#D5AEFC\"]");
            }
        }
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

        // Use label if it is defined
        if (inputOutput.contains("label")) {
            String label = inputOutput.get("label").toString();
            nodeOptions.add("label=\"" + label + "\";");
        }

        // Write the line for the node
        String inputOutputName = rdfService.stepFromURI(inputOutput.get("name").toString());
        writeLine("    \"" + inputOutputName + "\" [" + String.join(",", nodeOptions) + "];");
    }

    /**
     * Write a single line using the Writer
     * @param line The line to be written
     * @throws IOException Any errors in writing which may have occurred
     */
    private void writeLine(String line) throws IOException {
        writer.write(line);
        writer.write(EOL);
    }

}
