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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.commonwl.view.cwl.CWLElement;
import org.commonwl.view.cwl.CWLProcess;
import org.commonwl.view.cwl.CWLStep;
import org.commonwl.view.workflow.Workflow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelDotWriterTest {

  private Workflow testWorkflow;

  /**
   * Manually make a workflow with multiple steps, default value, nested workflow etc to test DOT
   * generation TODO: This is a pain, can it be made simpler?
   */
  @BeforeEach
  public void setUp() throws Exception {

    // Inputs
    Map<String, CWLElement> inputs = new HashMap<>();
    CWLElement input1 = new CWLElement();
    input1.setLabel("First Input");
    inputs.put("input1", input1);
    inputs.put("input2", new CWLElement());

    // Steps
    Map<String, CWLStep> steps = new HashMap<>();

    CWLElement step1InputElement = new CWLElement();
    step1InputElement.addSourceID("input1");
    step1InputElement.addSourceID("input2");
    Map<String, CWLElement> step1inputs = new HashMap<>();
    step1inputs.put("toolinput1", step1InputElement);
    CWLStep step1 = new CWLStep(null, null, null, step1inputs);
    steps.put("step1", step1);

    CWLElement default1InputElement = new CWLElement();
    default1InputElement.setDefaultVal("examplefile.jar");
    Map<String, CWLElement> default1inputs = new HashMap<>();
    step1inputs.put("defaultInput", default1InputElement);
    CWLStep default1 = new CWLStep(null, null, null, default1inputs);
    steps.put("default1", default1);

    CWLElement step2InputElement = new CWLElement();
    step2InputElement.addSourceID("step1");
    step2InputElement.addSourceID("default1");
    Map<String, CWLElement> step2inputs = new HashMap<>();
    step2inputs.put("toolinput1", step2InputElement);
    CWLStep step2 = new CWLStep(null, null, null, step2inputs);
    step2.setRunType(CWLProcess.WORKFLOW);
    step2.setRun("subworkflow.cwl");
    step2.setLabel("Label for step 2");
    steps.put("step2", step2);

    // Output
    Map<String, CWLElement> outputs = new HashMap<>();
    CWLElement output = new CWLElement();
    output.setLabel("Single Output");
    output.setDoc("Description");
    output.addSourceID("step2");
    outputs.put("output", output);

    // Save workflow model
    testWorkflow = new Workflow("Example Workflow", "Description", inputs, outputs, steps);
  }

  /** Test functionality to write a Workflow to DOT format */
  @Test
  public void writeGraph() throws Exception {

    StringWriter dotSource = new StringWriter();
    ModelDotWriter dotWriter = new ModelDotWriter(dotSource);

    dotWriter.writeGraph(testWorkflow);

    File expectedDot = new File("src/test/resources/graphviz/testWorkflow.dot");
    assertEquals(
        FileUtils.readFileToString(expectedDot, StandardCharsets.UTF_8), dotSource.toString());
  }
}
