package org.commonwl.view.researchobject;

import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowRepository;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

/**
 * Test the separate async method to call the ROBundle constructor
 */
public class ROBundleFactoryTest {

    /**
     * Simulate creation of a valid workflow
     */
    @Test
    public void bundleForValidWorkflow() throws Exception {

        Workflow validWorkflow = new Workflow("Valid Workflow", "Doc for Valid Workflow",
                new HashMap<>(), new HashMap<>(), new HashMap<>(), null);

        // Mocked path to a RO bundle
        ROBundleService mockROBundleService = Mockito.mock(ROBundleService.class);
        when(mockROBundleService.saveToFile(anyObject()))
                .thenReturn(Paths.get("test/path/to/check/for.zip"));

        // Test method retries multiple times to get workflow model before success
        WorkflowRepository mockRepository = Mockito.mock(WorkflowRepository.class);
        when(mockRepository.findByRetrievedFrom(anyObject()))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(validWorkflow);

        // Create factory under test
        ROBundleFactory factory = new ROBundleFactory(mockROBundleService, mockRepository);

        // Attempt to add RO to workflow
        factory.workflowROFromGithub(Mockito.mock(GithubDetails.class));

        assertEquals("test/path/to/check/for.zip", validWorkflow.getRoBundle());

    }

}