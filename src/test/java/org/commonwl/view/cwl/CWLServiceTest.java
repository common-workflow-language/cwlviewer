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

package org.commonwl.view.cwl;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.commonwl.view.git.GitConfig;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowOverview;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {GitConfig.class})
public class CWLServiceTest {

  /** RDFService for testing */
  private RDFService rdfService;

  /** GitConfig for testing */
  private GitConfig gitConfig;

  @BeforeEach
  public void setUp() throws Exception {
    File packedWorkflowRdf = new File("src/test/resources/cwl/make_to_cwl/dna.ttl");
    Model workflowModel = ModelFactory.createDefaultModel();
    workflowModel.read(
        new ByteArrayInputStream(
            readFileToString(packedWorkflowRdf, StandardCharsets.UTF_8).getBytes()),
        null,
        "TURTLE");
    Dataset workflowDataset = DatasetFactory.create();
    workflowDataset.addNamedModel(
        "https://w3id.org/cwl/view/git/549c973ccc01781595ce562dea4cedc6c9540fe0/workflows/make-to-cwl/dna.cwl#main",
        workflowModel);

    Answer<ResultSet> queryRdf =
        new Answer<ResultSet>() {
          @Override
          public ResultSet answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            Query query = QueryFactory.create(args[0].toString());
            try (QueryExecution qexec = QueryExecutionFactory.create(query, workflowDataset)) {
              return ResultSetFactory.copyResults(qexec.execSelect());
            }
          }
        };

    Answer<ResultSet> apacheLicense =
        new Answer<ResultSet>() {
          @Override
          public ResultSet answer(InvocationOnMock invocationOnMock) throws Throwable {
            RDFNode licenseNode = Mockito.mock(RDFNode.class);
            when(licenseNode.toString()).thenReturn("https://www.apache.org/licenses/LICENSE-2.0");
            QuerySolution result = Mockito.mock(QuerySolution.class);
            when(result.get("license")).thenReturn(licenseNode);
            ResultSet resultSet = Mockito.mock(ResultSet.class);
            when(resultSet.hasNext()).thenReturn(true);
            Mockito.when(resultSet.next()).thenReturn(result);
            return resultSet;
          }
        };

    this.rdfService = Mockito.spy(new RDFService("http://localhost:3030/cwlviewer/"));
    Mockito.doAnswer(queryRdf).when(rdfService).runQuery(any());
    Mockito.doAnswer(apacheLicense).when(rdfService).getLicense(any());
    Mockito.doReturn(true).when(rdfService).graphExists(any(String.class));

    this.gitConfig = Mockito.spy(GitConfig.class);
  }

  @Test
  public void parsePackedWorkflowNativePath() throws Exception {
    CWLService cwlService =
        new CWLService(
            rdfService,
            Mockito.mock(CWLTool.class),
            Mockito.mock(GitConfig.class).licenseVocab(),
            5242880);
    Workflow dna =
        cwlService.parseWorkflowNative(
            Paths.get("src/test/resources/cwl/make_to_cwl/dna.cwl"), "main");
    assertNotNull(dna);
    assertEquals("dna.cwl", dna.getLabel());
    assertEquals(1, dna.getInputs().size());
    assertEquals(1, dna.getOutputs().size());
    assertEquals(3, dna.getSteps().size());
  }

  @Test
  public void parsePackedWorkflowNativeStream() throws Exception {
    CWLService cwlService =
        new CWLService(
            rdfService,
            Mockito.mock(CWLTool.class),
            Mockito.mock(GitConfig.class).licenseVocab(),
            5242880);
    Workflow dna =
        cwlService.parseWorkflowNative(
            getClass().getResourceAsStream("/cwl/make_to_cwl/dna.cwl"), "main", "dna.cwl");
    assertNotNull(dna);
    assertEquals("dna.cwl", dna.getLabel());
    assertEquals(1, dna.getInputs().size());
    assertEquals(1, dna.getOutputs().size());
    assertEquals(3, dna.getSteps().size());
  }

  /** Test native loading parsing of a the LobSTR workflow CWL version draft-3 */
  @Test
  public void parseLobSTRDraft3WorkflowNative() throws Exception {
    CWLService cwlService =
        new CWLService(
            rdfService,
            Mockito.mock(CWLTool.class),
            Mockito.mock(GitConfig.class).licenseVocab(),
            5242880);
    Workflow lobSTRDraft3 =
        cwlService.parseWorkflowNative(
            Paths.get("src/test/resources/cwl/lobstr-draft3/lobSTR-workflow.cwl"), null);
    testLobSTRWorkflow(lobSTRDraft3, true);
  }

  /** Test native loading parsing of a the LobSTR workflow CWL version 1.0 */
  @Test
  public void parseLobSTRv1WorkflowNative() throws Exception {
    CWLService cwlService =
        new CWLService(
            rdfService, new CWLTool(), Mockito.mock(GitConfig.class).licenseVocab(), 5242880);
    Workflow lobSTRv1 =
        cwlService.parseWorkflowNative(
            Paths.get("src/test/resources/cwl/lobstr-v1/lobSTR-workflow.cwl"), null);
    testLobSTRWorkflow(lobSTRv1, true);
  }

  /** Test native loading parsing of optional inline types */
  @Test
  public void parseWorkflowInlineOptionalTypesNative() throws Exception {
    CWLService cwlService =
        new CWLService(
            rdfService, new CWLTool(), Mockito.mock(GitConfig.class).licenseVocab(), 5242880);
    //        URI uri = new
    // URI("https://github.com/emo-bon/pipeline-v5/raw/develop/workflows/gos_wf.cwl");
    //        InputStream is = uri.toURL().openStream();
    //        Workflow wkflow = cwlService.parseWorkflowNative(is, null, uri.toURL().getFile());
    Workflow wkflow =
        cwlService.parseWorkflowNative(
            Paths.get("src/test/resources/cwl/oneline_optional_types.cwl"), null);
    assertEquals(wkflow.getInputs().get("qualified_phred_quality").getType(), "int?");
    assertEquals(wkflow.getInputs().get("ncrna_tab_file").getType(), "File?");
    assertEquals(wkflow.getInputs().get("reverse_reads").getType(), "File?");
    assertEquals(wkflow.getInputs().get("ssu_tax").getType(), "string, File");
    assertEquals(
        wkflow.getInputs().get("rfam_models").getType(), "{type=array, items=[string, File]}");
  }

  /** Test native loading parsing of MultipleInputFeatureRequirement using workflows */
  @Test
  public void parseWorkflowMultiInboundLins() throws Exception {
    CWLService cwlService =
        new CWLService(
            rdfService, new CWLTool(), Mockito.mock(GitConfig.class).licenseVocab(), 5242880);
    Workflow wkflow =
        cwlService.parseWorkflowNative(
            Paths.get("src/test/resources/cwl/complex-workflow/complex-workflow-1.cwl"), null);
    assertEquals(
        wkflow.getSteps().get("re_tar_step").getSources().get("file_list").getSourceIDs().get(0),
        "touch_step");
    assertEquals(
        wkflow.getSteps().get("re_tar_step").getSources().get("file_list").getSourceIDs().get(1),
        "files");
  }

  /** Test native loading parsing of nested array types */
  @Test
  public void parseWorkflowNestedArrayTypes() throws Exception {
    CWLService cwlService =
        new CWLService(
            rdfService, new CWLTool(), Mockito.mock(GitConfig.class).licenseVocab(), 5242880);
    Workflow wkflow =
        cwlService.parseWorkflowNative(Paths.get("src/test/resources/cwl/nested_array.cwl"), null);
    assertEquals(wkflow.getInputs().get("overlap_files").getType(), "File[][]");
    assertEquals(wkflow.getOutputs().get("freq_files").getType(), "File[][]");
    assertEquals(
        true, Map.class.isAssignableFrom(wkflow.getSteps().get("dummy").getRun().getClass()));
  }

  /** Test native loading parsing of inputs with null default values */
  @Test
  public void parseWorkflowDefaultNullTypes() throws Exception {
    CWLService cwlService =
        new CWLService(
            rdfService, new CWLTool(), Mockito.mock(GitConfig.class).licenseVocab(), 5242880);
    Workflow wkflow =
        cwlService.parseWorkflowNative(Paths.get("src/test/resources/cwl/null_default.cwl"), null);
    assertEquals(wkflow.getInputs().get("overlap_files").getDefaultVal(), null);
    assertEquals(
        wkflow.getSteps().get("dummy").getSources().get("nested_array").getDefaultVal(), null);
  }

  /** Test parsing of a workflow using cwltool */
  @Test
  public void parseWorkflowWithCwltool() throws Exception {

    // Mock CWLTool
    CWLTool mockCwlTool = Mockito.mock(CWLTool.class);
    File packedWorkflowRdf = new File("src/test/resources/cwl/make_to_cwl/dna.ttl");
    when(mockCwlTool.getRDF(any(String.class)))
        .thenReturn(readFileToString(packedWorkflowRdf, StandardCharsets.UTF_8));

    // CWLService to test
    CWLService cwlService =
        new CWLService(rdfService, mockCwlTool, gitConfig.licenseVocab(), 5242880);

    GitDetails gitInfo =
        new GitDetails(
            "https://github.com/common-workflow-language/workflows.git",
            "549c973ccc01781595ce562dea4cedc6c9540fe0",
            "workflows/make-to-cwl/dna.cwl");
    Workflow basicModel = new Workflow();
    basicModel.setRetrievedFrom(gitInfo);
    gitInfo.setPackedId("main");
    basicModel.setLastCommit("549c973ccc01781595ce562dea4cedc6c9540fe0");

    // Parse the workflow
    Workflow workflow =
        cwlService.parseWorkflowWithCwltool(
            basicModel,
            Paths.get("src/test/resources/cwl/make_to_cwl/dna.cwl"),
            Paths.get("src/test/resources/cwl/make_to_cwl"));

    // Check basic information
    assertNotNull(workflow);
    assertEquals(1, workflow.getInputs().size());
    assertEquals(1, workflow.getOutputs().size());
    assertEquals(3, workflow.getSteps().size());
    File expectedDotCode = new File("src/test/resources/cwl/make_to_cwl/visualisation.dot");
    assertEquals(
        readFileToString(expectedDotCode, StandardCharsets.UTF_8), workflow.getVisualisationDot());
    assertEquals("https://spdx.org/licenses/Apache-2.0", workflow.getLicenseLink());
    assertEquals("Apache License 2.0", workflow.getLicenseName());
  }

  /** Test IOException is thrown when files are over limit */
  @Test
  public void workflowOverSingleFileSizeLimitThrowsIOException() throws Exception {

    // Should throw IOException due to oversized files
    Exception thrown =
        Assertions.assertThrows(
            IOException.class,
            () -> {
              CWLService cwlService =
                  new CWLService(
                      rdfService,
                      Mockito.mock(CWLTool.class),
                      Mockito.mock(GitConfig.class).licenseVocab(),
                      0);
              cwlService.parseWorkflowNative(
                  Paths.get("src/test/resources/cwl/lobstr-draft3/lobSTR-workflow.cwl"), null);
            });
    assertEquals(
        "File 'lobSTR-workflow.cwl' is over singleFileSizeLimit - 2 KB/0 bytes",
        thrown.getMessage());
  }

  /** Test retrieval of a workflow overview for hello world example in cwl */
  @Test
  public void getHelloWorkflowOverview() throws Exception {

    // Test cwl service
    CWLService cwlService =
        new CWLService(
            Mockito.mock(RDFService.class),
            Mockito.mock(CWLTool.class),
            Mockito.mock(GitConfig.class).licenseVocab(),
            5242880);

    // Run workflow overview
    File helloWorkflow = new File("src/test/resources/cwl/hello/hello.cwl");
    WorkflowOverview hello = cwlService.getWorkflowOverview(helloWorkflow);
    assertNotNull(hello);

    // No docs for this workflow
    assertEquals("Hello World", hello.getLabel());
    assertEquals("Puts a message into a file using echo", hello.getDoc());
    assertEquals("/hello.cwl", hello.getFileName());
  }

  /** Test retrieval of a workflow overview with an array ``doc`` field */
  @Test
  public void getHelloWorkflowOverviewDocList() throws Exception {

    // Test cwl service
    CWLService cwlService =
        new CWLService(
            Mockito.mock(RDFService.class),
            Mockito.mock(CWLTool.class),
            Mockito.mock(GitConfig.class).licenseVocab(),
            5242880);

    // Run workflow overview
    File helloWorkflow = new File("src/test/resources/cwl/hello/hello_doclist.cwl");
    System.out.println(cwlService.normaliseLicenseLink("http://asdasdasda"));
    WorkflowOverview hello = cwlService.getWorkflowOverview(helloWorkflow);
    assertNotNull(hello);
    assertEquals("Puts a message into a file using echo. Even more doc", hello.getDoc());
  }

  /** Test IOException is thrown when files are over limit with getWorkflowOverview */
  @Test
  public void workflowOverviewOverSingleFileSizeLimitThrowsIOException() throws Exception {
    // File to test
    File helloWorkflow = new File("src/test/resources/cwl/hello/hello.cwl");

    // Test cwl service
    Exception thrown =
        Assertions.assertThrows(
            IOException.class,
            () -> {
              CWLService cwlService =
                  new CWLService(
                      Mockito.mock(RDFService.class),
                      Mockito.mock(CWLTool.class),
                      Mockito.mock(GitConfig.class).licenseVocab(),
                      0);
              cwlService.getWorkflowOverview(helloWorkflow);
            });
    assertEquals(
        String.format(
            "File 'hello.cwl' is over singleFileSizeLimit - %s bytes/0 bytes",
            helloWorkflow.length()),
        thrown.getMessage());
  }

  /**
   * Get workflow overviews from a packed file TODO: Get better example with multiple workflows with
   * label/doc
   */
  @Test
  public void workflowOverviewsFromPackedFile() throws Exception {
    CWLService cwlService =
        new CWLService(
            Mockito.mock(RDFService.class),
            Mockito.mock(CWLTool.class),
            Mockito.mock(GitConfig.class).licenseVocab(),
            5242880);
    File packedFile = new File("src/test/resources/cwl/make_to_cwl/dna.cwl");
    assertTrue(cwlService.isPacked(packedFile));
    List<WorkflowOverview> overviews = cwlService.getWorkflowOverviewsFromPacked(packedFile);
    assertEquals(1, overviews.size());
    assertEquals("main", overviews.get(0).getFileName());
    assertNull(overviews.get(0).getLabel());
    assertNull(overviews.get(0).getDoc());
  }

  /**
   * Validate a LobSTR workflow See:
   * https://github.com/common-workflow-language/workflows/tree/master/workflows/lobSTR
   */
  private void testLobSTRWorkflow(Workflow lobSTR, boolean nativeParsed) throws Exception {

    // Overall not null
    assertNotNull(lobSTR);

    // Input Tests
    Map<String, CWLElement> inputs = lobSTR.getInputs();
    assertNotNull(inputs);
    assertEquals(8, inputs.size());
    assertNotNull(inputs.get("strinfo"));
    assertEquals("File", inputs.get("strinfo").getType());
    assertNotNull(inputs.get("p2"));
    assertEquals("File[]?", inputs.get("p2").getType());
    assertNotNull(inputs.get("rg-sample"));
    assertEquals("Use this in the read group SM tag", inputs.get("rg-sample").getDoc());

    // Step tests
    Map<String, CWLStep> steps = lobSTR.getSteps();
    assertNotNull(steps);
    assertEquals(4, steps.size());
    assertNotNull(steps.get("lobSTR"));
    assertEquals("lobSTR-tool.cwl", steps.get("lobSTR").getRun());
    assertNotNull(steps.get("samindex"));
    assertTrue(steps.get("samindex").getSources().get("input").getSourceIDs().contains("samsort"));

    // Output tests
    Map<String, CWLElement> outputs = lobSTR.getOutputs();
    assertNotNull(outputs);
    assertEquals(4, outputs.size());
    assertNotNull(outputs.get("bam_stats"));
    assertEquals("File", outputs.get("bam_stats").getType());
    assertTrue(outputs.get("bam").getSourceIDs().contains("samindex"));

    // Extra tests if parsing is done with cwltool
    if (!nativeParsed) {
      assertEquals(CWLProcess.COMMANDLINETOOL, steps.get("lobSTR").getRunType());
    }
  }
}
