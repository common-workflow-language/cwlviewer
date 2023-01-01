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

import com.github.jabbalaci.graphviz.GraphViz;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Handles Graphviz rendering from DOT generated by RDFDotWriter */
@Service
public class GraphVizService {

  private final String graphvizStorage;

  @Autowired
  public GraphVizService(@Value("${graphvizStorage}") String graphvizStorage) {
    this.graphvizStorage = graphvizStorage;
  }

  /**
   * Generate a graph in a specified format using GraphViz
   *
   * @param dot The DOT source
   * @param format The format for the graph to be generated in, e.g. "svg", "png", "dot"
   * @return An InputStream containing the graph in desired image format.
   */
  public InputStream getGraphStream(String dot, String format) {
    // Generate graphviz image if it does not already exist
    GraphViz gv = new GraphViz();

    // Different DPI and transparency for svg files
    if (format.equals("svg")) {
      gv.decreaseDpi();
      gv.decreaseDpi();
      gv.decreaseDpi();
      dot = dot.replace("bgcolor = \"#eeeeee\"", "bgcolor = \"transparent\"");
    }

    byte[] dotBytes = gv.getGraph(dot, format, "dot");
    return new ByteArrayInputStream(dotBytes);
  }

  /**
   * Generate a graph in a specified format using GraphViz
   *
   * @param fileName The relative name of the file to be generated in the graphvizStorage directory
   * @param dot The DOT source
   * @param format The format for the graph to be generated in, e.g. "svg", "png", "dot"
   * @return The file containing the graph
   * @throws IOException if the writing failed (e.g. out of disk space)
   */
  public Path getGraphPath(String fileName, String dot, String format) throws IOException {
    // Generate graphviz image if it does not already exist
    Path out = Paths.get(graphvizStorage).resolve(fileName);
    try {
      if (!Files.exists(out)) {
        Files.copy(getGraphStream(dot, format), out);
      }
    } catch (FileAlreadyExistsException ex) {
      return out;
    }
    return out;
  }

  /**
   * Delete the cache of workflow images for a workflow TODO: Make this more dynamic in some way,
   * either store in workflow object or clear folder
   *
   * @param workflowID The ID of the workflow used for assuming file locations
   */
  public void deleteCache(String workflowID) {
    File graphvizSvg = new File(graphvizStorage + "/" + workflowID + ".svg");
    graphvizSvg.delete();
    File graphvizPng = new File(graphvizStorage + "/" + workflowID + ".png");
    graphvizPng.delete();
    File graphvizXdot = new File(graphvizStorage + "/" + workflowID + ".dot");
    graphvizXdot.delete();
  }
}
