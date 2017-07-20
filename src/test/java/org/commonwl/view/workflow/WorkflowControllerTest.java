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

package org.commonwl.view.workflow;

import org.commonwl.view.cwl.CWLValidationException;
import org.commonwl.view.github.GitDetails;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.researchobject.ROBundleNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the controller for workflow related functionality
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkflowControllerTest {

    /**
     * Use a temporary directory for testing
     */
    @Rule
    public TemporaryFolder roBundleFolder = new TemporaryFolder();

    /**
     * Get the full list of workflows
     * TODO: Mock the repository and test model attributes
     */
    @Test
    public void getListOfWorkflows() throws Exception {

        // Mock controller/MVC
        WorkflowController workflowController = new WorkflowController(
                Mockito.mock(WorkflowFormValidator.class),
                Mockito.mock(WorkflowService.class),
                Mockito.mock(GraphVizService.class));

        // Lots of hassle to make Spring Data Pageable work
        PageableHandlerMethodArgumentResolver pageableArgumentResolver =
                new PageableHandlerMethodArgumentResolver();
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/src/main/resources/templates");
        viewResolver.setSuffix(".html");
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setViewResolvers(viewResolver)
                .build();

        // Simple test to check the view
        mockMvc.perform(get("/workflows"))
                .andExpect(status().isOk())
                .andExpect(view().name("workflows"));

    }

    /**
     * Endpoint for main form submission
     */
    @Test
    public void newWorkflowFromGithubURL() throws Exception {

        // Validator pass or fail
        WorkflowFormValidator mockValidator = Mockito.mock(WorkflowFormValidator.class);
        when(mockValidator.validateAndParse(anyObject(), anyObject()))
                .thenReturn(null)
                .thenReturn(new GitDetails("owner", "repoName", "branch", "path/within"))
                .thenReturn(new GitDetails("owner", "repoName", "branch", "path/workflow.cwl"));

        // The eventual accepted valid workflow
        Workflow mockWorkflow = Mockito.mock(Workflow.class);
        when(mockWorkflow.getRetrievedFrom())
                .thenReturn(new GitDetails("owner", "repoName", "branch", "path/workflow.cwl"));
        QueuedWorkflow mockQueuedWorkflow = Mockito.mock(QueuedWorkflow.class);
        when(mockQueuedWorkflow.getTempRepresentation())
                .thenReturn(mockWorkflow);

        // Mock workflow service returning valid workflow
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.createQueuedWorkflow(anyObject()))
                .thenThrow(new CWLValidationException("Error"))
                .thenReturn(mockQueuedWorkflow);

        // Mock controller/MVC
        WorkflowController workflowController = new WorkflowController(
                mockValidator,
                mockWorkflowService,
                Mockito.mock(GraphVizService.class));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .build();

        // Error in validation, go to index to show error
        mockMvc.perform(post("/workflows")
                .param("githubURL", "invalidurl"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        // Valid directory URL redirect
        mockMvc.perform(post("/workflows")
                .param("githubURL", "https://github.com/owner/repoName/tree/branch/path/within"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/workflows/github.com/owner/repoName/tree/branch/path/within"));

        // Invalid workflow URL, go to index to show error
        mockMvc.perform(post("/workflows")
                .param("githubURL", "https://github.com/owner/repoName/tree/branch/path/nonexistant.cwl"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        // Valid workflow URL redirect
        mockMvc.perform(post("/workflows")
                .param("githubURL", "https://github.com/owner/repoName/tree/branch/path/workflow.cwl"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/workflows/github.com/owner/repoName/tree/branch/path/workflow.cwl"));

    }

    /**
     * Displaying workflows
     */
    @Test
    public void directWorkflowURL() throws Exception {

        Workflow mockWorkflow = Mockito.mock(Workflow.class);
        QueuedWorkflow mockQueuedWorkflow = Mockito.mock(QueuedWorkflow.class);

        // Mock service
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.getWorkflow(Matchers.<GitDetails>anyObject()))
                .thenReturn(mockWorkflow)
                .thenReturn(null);
        when(mockWorkflowService.createQueuedWorkflow(anyObject()))
                .thenReturn(mockQueuedWorkflow)
                .thenThrow(new CWLValidationException("Error"));

        // Mock controller/MVC
        WorkflowController workflowController = new WorkflowController(
                Mockito.mock(WorkflowFormValidator.class),
                mockWorkflowService,
                Mockito.mock(GraphVizService.class));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .build();

        // Workflow already exists in the database
        mockMvc.perform(get("/workflows/github.com/owner/reponame/tree/branch/path/within/workflow.cwl"))
                .andExpect(status().isOk())
                .andExpect(view().name("workflow"))
                .andExpect(model().attribute("workflow", is(mockWorkflow)));

        // Workflow needs to be created, loading page
        mockMvc.perform(get("/workflows/github.com/owner/reponame/tree/branch/path/within/workflow.cwl"))
                .andExpect(status().isOk())
                .andExpect(view().name("loading"))
                .andExpect(model().attribute("queued", is(mockQueuedWorkflow)));

        // Error creating workflow
        mockMvc.perform(get("/workflows/github.com/owner/reponame/tree/branch/path/within/badworkflow.cwl"))
                .andExpect(status().isFound())
                .andExpect(flash().attributeExists("errors"))
                .andExpect(redirectedUrl("/?url=https://github.com/owner/reponame/tree/branch/path/within/badworkflow.cwl"));

    }

    /**
     * Endpoint for downloading RO bundle for a workflow
     */
    @Test
    public void downloadROBundle() throws Exception {

        // Mock service to return a bundle file and then throw ROBundleNotFoundException
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.getROBundle(anyObject()))
                .thenReturn(roBundleFolder.newFile("bundle.zip").getAbsoluteFile())
                .thenThrow(new ROBundleNotFoundException());

        // Mock controller/MVC
        WorkflowController workflowController = new WorkflowController(
                Mockito.mock(WorkflowFormValidator.class),
                mockWorkflowService,
                Mockito.mock(GraphVizService.class));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .build();

        // Bundle exists and can be downloaded
        mockMvc.perform(get("/robundle/github.com/owner/repo/blob/branch/path/to/workflow.cwl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.wf4ever.robundle+zip"));

        // Bundle does not exist, 404 error
        mockMvc.perform(get("/robundle/github.com/owner/repo/blob/branch/path/to/workflow.cwl"))
                .andExpect(status().isNotFound());

    }

    /**
     * Endpoints for downloading GraphViz graph files
     */
    @Test
    public void downloadGraphVizFiles() throws Exception {

        // Mock service to return mock workflow
        Workflow mockWorkflow = Mockito.mock(Workflow.class);
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.getWorkflow(Mockito.any(GitDetails.class)))
                .thenReturn(mockWorkflow)
                .thenReturn(null)
                .thenReturn(mockWorkflow)
                .thenReturn(null)
                .thenReturn(mockWorkflow)
                .thenReturn(null);

        // Mock service to return files
        GraphVizService mockGraphVizService = Mockito.mock(GraphVizService.class);
        when(mockGraphVizService.getGraph(anyString(), anyString(), anyString()))
                .thenReturn(roBundleFolder.newFile("graph.svg").getAbsoluteFile())
                .thenReturn(roBundleFolder.newFile("graph.png").getAbsoluteFile())
                .thenReturn(roBundleFolder.newFile("graph.dot").getAbsoluteFile());

        // Mock controller/MVC
        WorkflowController workflowController = new WorkflowController(
                Mockito.mock(WorkflowFormValidator.class),
                mockWorkflowService,
                mockGraphVizService);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .build();

        // Image exists and can be downloaded
        mockMvc.perform(get("/graph/svg/github.com/owner/repo/blob/branch/path/to/workflow.cwl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"));

        // Image does not exist, 404 error
        mockMvc.perform(get("/graph/svg/github.com/owner/repo/blob/branch/path/to/workflow.cwl"))
                .andExpect(status().isNotFound());

        // Image exists and can be downloaded
        mockMvc.perform(get("/graph/png/github.com/owner/repo/blob/branch/path/to/workflow.cwl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        // Image does not exist, 404 error
        mockMvc.perform(get("/graph/png/github.com/owner/repo/blob/branch/path/to/workflow.cwl"))
                .andExpect(status().isNotFound());

        // Image exists and can be downloaded
        mockMvc.perform(get("/graph/xdot/github.com/owner/repo/blob/branch/path/to/workflow.cwl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/vnd.graphviz"));

        // Image does not exist, 404 error
        mockMvc.perform(get("/graph/xdot/github.com/owner/repo/blob/branch/path/to/workflow.cwl"))
                .andExpect(status().isNotFound());

    }

}