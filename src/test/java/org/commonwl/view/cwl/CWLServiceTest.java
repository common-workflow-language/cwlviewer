package org.commonwl.view.cwl;

import org.apache.commons.io.FileUtils;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowOverview;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class CWLServiceTest {

    /**
     * Test main parsing of a the LobSTR workflow CWL version draft-3
     */
    @Test
    public void parseLobSTRDraft3Workflow() throws Exception {

        // Mock githubService class to redirect downloads to resources folder
        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        Answer fileAnswer = new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GithubDetails details = (GithubDetails) args[0];
                File workflowFile = new File("src/test/resources/cwl/lobstr-draft3/"
                        + details.getPath().replace("workflows/lobSTR/", ""));
                return FileUtils.readFileToString(workflowFile);
            }
        };
        when(mockGithubService.downloadFile(anyObject())).thenAnswer(fileAnswer);
        when(mockGithubService.downloadFile(anyObject(), anyObject())).thenAnswer(fileAnswer);

        // Test cwl service
        CWLService cwlService = new CWLService(mockGithubService, 5242880);

        // Get workflow from community repo by commit ID so it will not change
        GithubDetails lobSTRDraft3Details = new GithubDetails("common-workflow-language",
                "workflows", null, "workflows/lobSTR/lobSTR-workflow.cwl");
        Workflow lobSTRDraft3 = cwlService.parseWorkflow(lobSTRDraft3Details, "920c6be45f08e979e715a0018f22c532b024074f");

        testLobSTRWorkflow(lobSTRDraft3);

        // Extra docker requirement test
        assertEquals("true", lobSTRDraft3.getDockerLink());
    }

    /**
     * Test main parsing of a the LobSTR workflow CWL version 1.0
     */
    @Test
    public void parseLobSTRv1Workflow() throws Exception {

        // Mock githubService class to redirect downloads to resources folder
        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        Answer fileAnswer = new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GithubDetails details = (GithubDetails) args[0];
                File workflowFile = new File("src/test/resources/cwl/lobstr-v1/"
                        + details.getPath().replace("workflows/lobSTR/", ""));
                return FileUtils.readFileToString(workflowFile);
            }
        };
        when(mockGithubService.downloadFile(anyObject())).thenAnswer(fileAnswer);
        when(mockGithubService.downloadFile(anyObject(), anyObject())).thenAnswer(fileAnswer);

        // Test cwl service
        CWLService cwlService = new CWLService(mockGithubService, 5242880);

        // Get workflow from community repo by commit ID so it will not change
        GithubDetails lobSTRv1Details = new GithubDetails("common-workflow-language",
                "workflows", null, "workflows/lobSTR/lobSTR-workflow.cwl");
        Workflow lobSTRv1 = cwlService.parseWorkflow(lobSTRv1Details, "933bf2a1a1cce32d88f88f136275535da9df0954");

        testLobSTRWorkflow(lobSTRv1);

        // Extra docker requirement test
        assertEquals("https://hub.docker.com/r/rabix/lobstr", lobSTRv1.getDockerLink());

    }

    /**
     * Validate a LobSTR workflow
     * See: https://github.com/common-workflow-language/workflows/tree/master/workflows/lobSTR
     */
    private void testLobSTRWorkflow(Workflow lobSTR) throws Exception {

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
        assertEquals(CWLProcess.COMMANDLINETOOL, steps.get("lobSTR").getRunType());
        assertNotNull(steps.get("samindex"));
        assertTrue(steps.get("samindex").getInputs().get("input").getSourceIDs().contains("samsort"));

        // Output tests
        Map<String, CWLElement> outputs = lobSTR.getOutputs();
        assertNotNull(outputs);
        assertEquals(4, outputs.size());
        assertNotNull(outputs.get("bam_stats"));
        assertEquals("File", outputs.get("bam_stats").getType());
        assertTrue(outputs.get("bam").getSourceIDs().contains("samindex"));

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
        CWLService cwlService = new CWLService(mockGithubService, 5242880);

        // Get workflow from community repo by commit ID so it will not change
        GithubDetails helloDetails = new GithubDetails("common-workflow-language",
                "workflows", "8296e92d358bb5da4dc3c6e7aabefa89726e3409", "workflows/hello/hello.cwl");
        WorkflowOverview hello = cwlService.getWorkflowOverview(helloDetails);
        assertNotNull(hello);

        // No docs for this workflow
        assertEquals("Hello World", hello.getLabel());
        assertEquals("Puts a message into a file using echo", hello.getDoc());
        assertEquals("hello.cwl", hello.getFileName());

    }

}