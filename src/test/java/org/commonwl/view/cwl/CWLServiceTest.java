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

import org.apache.commons.io.FileUtils;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowOverview;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class CWLServiceTest {

    /**
     * Used for expected IOExceptions for filesize limits
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Test native loading parsing of a the LobSTR workflow CWL version draft-3
     */
    @Test
    public void parseLobSTRDraft3WorkflowNative() throws Exception {

        // Get mock Github service
        GitHubService mockGithubService = getMockGithubService("workflows/lobSTR/",
                "src/test/resources/cwl/lobstr-draft3/");

        // Test cwl service
        CWLService cwlService = new CWLService(mockGithubService,
                new RDFService(), new CWLTool(), 5242880);

        // Get workflow from community repo by commit ID so it will not change
        GithubDetails lobSTRDraft3Details = new GithubDetails("common-workflow-language",
                "workflows", null, "workflows/lobSTR/lobSTR-workflow.cwl");
        Workflow lobSTRDraft3 = cwlService.parseWorkflowNative(lobSTRDraft3Details, "920c6be45f08e979e715a0018f22c532b024074f");

        testLobSTRWorkflow(lobSTRDraft3, true);

    }

    /**
     * Test native loading parsing of a the LobSTR workflow CWL version 1.0
     */
    @Test
    public void parseLobSTRv1WorkflowNative() throws Exception {

        // Get mock Github service
        GitHubService mockGithubService = getMockGithubService("workflows/lobSTR/",
                "src/test/resources/cwl/lobstr-draft3/");

        // Test cwl service
        CWLService cwlService = new CWLService(mockGithubService,
                new RDFService(), new CWLTool(), 5242880);

        // Get workflow from community repo by commit ID so it will not change
        GithubDetails lobSTRv1Details = new GithubDetails("common-workflow-language",
                "workflows", null, "workflows/lobSTR/lobSTR-workflow.cwl");
        Workflow lobSTRv1 = cwlService.parseWorkflowNative(lobSTRv1Details, "933bf2a1a1cce32d88f88f136275535da9df0954");

        testLobSTRWorkflow(lobSTRv1, true);

    }

    /**
     * Test retrieval of a workflow overview for hello world example in cwl
     */
    @Test
    public void getHelloWorkflowOverview() throws Exception {

        // Mock githubService class
        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        File workflowFile = new File("src/test/resources/cwl/hello/hello.cwl");
        when(mockGithubService.downloadFile(anyObject()))
                .thenReturn(FileUtils.readFileToString(workflowFile));

        // Test cwl service
        CWLService cwlService = new CWLService(mockGithubService,
                new RDFService(), Mockito.mock(CWLTool.class), 5242880);

        // Run workflow overview
        GithubDetails helloDetails = new GithubDetails("common-workflow-language",
                "workflows", "8296e92d358bb5da4dc3c6e7aabefa89726e3409", "workflows/hello/hello.cwl");
        WorkflowOverview hello = cwlService.getWorkflowOverview(helloDetails);
        assertNotNull(hello);

        // No docs for this workflow
        assertEquals("Hello World", hello.getLabel());
        assertEquals("Puts a message into a file using echo", hello.getDoc());
        assertEquals("hello.cwl", hello.getFileName());

    }

    /**
     * Test IOException is thrown when files are over limit with getWorkflowOverview
     */
    @Test
    public void workflowOverviewOverSingleFileSizeLimitThrowsIOException() throws Exception {

        // Mock githubService class
        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        File workflowFile = new File("src/test/resources/cwl/hello/hello.cwl");
        when(mockGithubService.downloadFile(anyObject()))
                .thenReturn(FileUtils.readFileToString(workflowFile));

        // Test cwl service with 0 filesize limit
        CWLService cwlService = new CWLService(mockGithubService,
                Mockito.mock(RDFService.class), Mockito.mock(CWLTool.class), 0);

        // Run workflow overview
        GithubDetails helloDetails = new GithubDetails("common-workflow-language",
                "workflows", "8296e92d358bb5da4dc3c6e7aabefa89726e3409", "workflows/hello/hello.cwl");

        // Should throw IOException due to oversized files
        thrown.expect(IOException.class);
        thrown.expectMessage("File 'workflows/hello/hello.cwl' is over singleFileSizeLimit - 672 bytes/0 bytes");
        cwlService.getWorkflowOverview(helloDetails);

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

    /**
     * Get a mock GithubService which redirects downloads to the filesystem
     */
    private GitHubService getMockGithubService(String originalFolder,
                                               String resourcesFolder) throws IOException {
        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        Answer fileAnswer = new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GithubDetails details = (GithubDetails) args[0];
                File workflowFile = new File(resourcesFolder
                        + details.getPath().replace(originalFolder, ""));
                return FileUtils.readFileToString(workflowFile);
            }
        };
        when(mockGithubService.downloadFile(anyObject())).thenAnswer(fileAnswer);
        when(mockGithubService.downloadFile(anyObject(), anyObject())).thenAnswer(fileAnswer);

        return mockGithubService;
    }

}