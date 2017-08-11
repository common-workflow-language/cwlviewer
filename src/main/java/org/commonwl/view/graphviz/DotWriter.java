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

import java.io.IOException;
import java.io.Writer;

/**
 * Takes an object and creates a DOT graph of it
 */
public abstract class DotWriter {

    protected static final String EOL = System.getProperty("line.separator");
    private Writer writer;

    public DotWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Write the start of the graph with styling based
     * on the Apache Taverna workflow management system
     * @throws IOException Any errors in writing which may have occurred
     */
    protected void writePreamble() throws IOException {

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

    }

    /**
     * Write a single line using the Writer
     * @param line The line to be written
     * @throws IOException Any errors in writing which may have occurred
     */
    protected void writeLine(String line) throws IOException {
        writer.write(line);
        writer.write(EOL);
    }


}
