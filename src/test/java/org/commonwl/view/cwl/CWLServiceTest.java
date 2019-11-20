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

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowOverview;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CWLServiceTest {

    /**
     * RDFService for testing
     */
    private RDFService rdfService;

    @Before
    public void setUp() throws Exception {
        File packedWorkflowRdf = new File("src/test/resources/cwl/make_to_cwl/dna.ttl");
        Model workflowModel = ModelFactory.createDefaultModel();
        workflowModel.read(new ByteArrayInputStream(readFileToString(packedWorkflowRdf).getBytes()), null, "TURTLE");
        Dataset workflowDataset = DatasetFactory.create();
        workflowDataset.addNamedModel("https://w3id.org/cwl/view/git/549c973ccc01781595ce562dea4cedc6c9540fe0/workflows/make-to-cwl/dna.cwl#main", workflowModel);

        Answer queryRdf = new Answer<ResultSet>() {
            @Override
            public ResultSet answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Query query = QueryFactory.create(args[0].toString());
                try (QueryExecution qexec = QueryExecutionFactory.create(query, workflowDataset)) {
                    return ResultSetFactory.copyResults(qexec.execSelect());
                }
            }
        };

        this.rdfService = Mockito.spy(new RDFService("http://localhost:3030/cwlviewer/"));
        Mockito.doAnswer(queryRdf).when(rdfService).runQuery(anyObject());
        Mockito.doReturn(true).when(rdfService).graphExists(anyString());
    }

    /**
     * Used for expected IOExceptions for filesize limits
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parsePackedWorkflowNativePath() throws Exception {
        CWLService cwlService = new CWLService(rdfService, Mockito.mock(CWLTool.class), 5242880);
        Workflow dna = cwlService.parseWorkflowNative(
                Paths.get("src/test/resources/cwl/make_to_cwl/dna.cwl"), "main");
        assertNotNull(dna);
        assertEquals("dna.cwl", dna.getLabel());
        assertEquals(1, dna.getInputs().size());
        assertEquals(1, dna.getOutputs().size());
        assertEquals(3, dna.getSteps().size());

    }

    @Test
    public void parsePackedWorkflowNativeStream() throws Exception {
        CWLService cwlService = new CWLService(rdfService, Mockito.mock(CWLTool.class), 5242880);        
        Workflow dna = cwlService.parseWorkflowNative(
        		getClass().getResourceAsStream("/cwl/make_to_cwl/dna.cwl"), "main", "dna.cwl");
        assertNotNull(dna);
        assertEquals("dna.cwl", dna.getLabel());
        assertEquals(1, dna.getInputs().size());
        assertEquals(1, dna.getOutputs().size());
        assertEquals(3, dna.getSteps().size());

    }

    
    /**
     * Test native loading parsing of a the LobSTR workflow CWL version draft-3
     */
    @Test
    public void parseLobSTRDraft3WorkflowNative() throws Exception {
        CWLService cwlService = new CWLService(rdfService, Mockito.mock(CWLTool.class), 5242880);
        Workflow lobSTRDraft3 = cwlService.parseWorkflowNative(
                Paths.get("src/test/resources/cwl/lobstr-draft3/lobSTR-workflow.cwl"), null);
        testLobSTRWorkflow(lobSTRDraft3, true);
    }

    /**
     * Test native loading parsing of a the LobSTR workflow CWL version 1.0
     */
    @Test
    public void parseLobSTRv1WorkflowNative() throws Exception {
        CWLService cwlService = new CWLService(rdfService, new CWLTool(), 5242880);
        Workflow lobSTRv1 = cwlService.parseWorkflowNative(
                Paths.get("src/test/resources/cwl/lobstr-v1/lobSTR-workflow.cwl"), null);
        testLobSTRWorkflow(lobSTRv1, true);
    }

    /**
     * Test parsing of a workflow using cwltool
     */
    @Test
    public void parseWorkflowWithCwltool() throws Exception {

        // Mock CWLTool
        CWLTool mockCwlTool = Mockito.mock(CWLTool.class);
        File packedWorkflowRdf = new File("src/test/resources/cwl/make_to_cwl/dna.ttl");
        when(mockCwlTool.getRDF(anyString()))
                .thenReturn(readFileToString(packedWorkflowRdf));

        // CWLService to test
        CWLService cwlService = new CWLService(rdfService, mockCwlTool, 5242880);

        GitDetails gitInfo = new GitDetails("https://github.com/common-workflow-language/workflows.git",
                "549c973ccc01781595ce562dea4cedc6c9540fe0", "workflows/make-to-cwl/dna.cwl");
        Workflow basicModel = new Workflow();
        basicModel.setRetrievedFrom(gitInfo);
        gitInfo.setPackedId("main");
        basicModel.setLastCommit("549c973ccc01781595ce562dea4cedc6c9540fe0");

        // Parse the workflow
        Workflow workflow = cwlService.parseWorkflowWithCwltool(basicModel,
                Paths.get("src/test/resources/cwl/make_to_cwl/dna.cwl"), Paths.get("src/test/resources/cwl/make_to_cwl"));

        // Check basic information
        assertNotNull(workflow);
        assertEquals(1, workflow.getInputs().size());
        assertEquals(1, workflow.getOutputs().size());
        assertEquals(3, workflow.getSteps().size());
        File expectedDotCode = new File("src/test/resources/cwl/make_to_cwl/visualisation.dot");
        assertEquals(readFileToString(expectedDotCode), workflow.getVisualisationDot());

    }

    /**
     * Test IOException is thrown when files are over limit
     */
    @Test
    public void workflowOverSingleFileSizeLimitThrowsIOException() throws Exception {

        // Should throw IOException due to oversized files
        thrown.expect(IOException.class);
        thrown.expectMessage("File 'lobSTR-workflow.cwl' is over singleFileSizeLimit - 2 KB/0 bytes");

        CWLService cwlService = new CWLService(rdfService, Mockito.mock(CWLTool.class), 0);
        cwlService.parseWorkflowNative(
                Paths.get("src/test/resources/cwl/lobstr-draft3/lobSTR-workflow.cwl"), null);

    }

    /**
     * Test retrieval of a workflow overview for hello world example in cwl
     */
    @Test
    public void getHelloWorkflowOverview() throws Exception {

        // Test cwl service
        CWLService cwlService = new CWLService(Mockito.mock(RDFService.class),
                Mockito.mock(CWLTool.class), 5242880);

        // Run workflow overview
        File helloWorkflow = new File("src/test/resources/cwl/hello/hello.cwl");
        WorkflowOverview hello = cwlService.getWorkflowOverview(helloWorkflow);
        assertNotNull(hello);

        // No docs for this workflow
        assertEquals("Hello World", hello.getLabel());
        assertEquals("Puts a message into a file using echo", hello.getDoc());
        assertEquals("/hello.cwl", hello.getFileName());

    }

    /**
     * Test IOException is thrown when files are over limit with getWorkflowOverview
     */
    @Test
    public void workflowOverviewOverSingleFileSizeLimitThrowsIOException() throws Exception {

        // Test cwl service
        CWLService cwlService = new CWLService(Mockito.mock(RDFService.class),
                Mockito.mock(CWLTool.class), 0);

        // File to test
        File helloWorkflow = new File("src/test/resources/cwl/hello/hello.cwl");

        // Should throw IOException due to oversized file
        thrown.expect(IOException.class);
        thrown.expectMessage(String.format("File 'hello.cwl' is over singleFileSizeLimit - %s bytes/0 bytes", 
                helloWorkflow.length()));
        cwlService.getWorkflowOverview(helloWorkflow);

    }

    /**
     * Get workflow overviews from a packed file
     * TODO: Get better example with multiple workflows with label/doc
     */
    @Test
    public void workflowOverviewsFromPackedFile() throws Exception {
        CWLService cwlService = new CWLService(Mockito.mock(RDFService.class),
                Mockito.mock(CWLTool.class), 5242880);
        File packedFile = new File("src/test/resources/cwl/make_to_cwl/dna.cwl");
        assertTrue(cwlService.isPacked(packedFile));
        List<WorkflowOverview> overviews = cwlService.getWorkflowOverviewsFromPacked(packedFile);
        assertEquals(1, overviews.size());
        assertEquals("main", overviews.get(0).getFileName());
        assertNull(overviews.get(0).getLabel());
        assertNull(overviews.get(0).getDoc());
    }

    /**
     * Validate a LobSTR workflow
     * See: https://github.com/common-workflow-language/workflows/tree/master/workflows/lobSTR
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