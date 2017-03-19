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

import org.commonwl.view.github.GithubDetails;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
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
                .thenReturn(new GithubDetails("owner", "repoName", "branch", "path/within"))
                .thenReturn(new GithubDetails("owner", "repoName", "branch", "path/workflow.cwl"));

        // The eventual accepted valid workflow
        Workflow mockWorkflow = Mockito.mock(Workflow.class);
        when(mockWorkflow.getRetrievedFrom())
                .thenReturn(new GithubDetails("owner", "repoName", "branch", "path/workflow.cwl"));

        // Mock workflow service returning valid workflow
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.createWorkflow(anyObject()))
                .thenReturn(null)
                .thenReturn(mockWorkflow);

        // Mock controller/MVC
        WorkflowController workflowController = new WorkflowController(
                mockValidator,
                mockWorkflowService,
                Mockito.mock(GraphVizService.class));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .build();

        // Error in validation, go to index to show error
        mockMvc.perform(post("/")
                .param("githubURL", "invalidurl"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        // Valid directory URL redirect
        mockMvc.perform(post("/")
                .param("githubURL", "https://github.com/owner/repoName/tree/branch/path/within"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/workflows/github.com/owner/repoName/tree/branch/path/within"));

        // Invalid workflow URL, go to index to show error
        mockMvc.perform(post("/")
                .param("githubURL", "https://github.com/owner/repoName/tree/branch/path/nonexistant.cwl"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        // Valid workflow URL redirect
        mockMvc.perform(post("/")
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
        Workflow mockWorkflow2 = Mockito.mock(Workflow.class);

        // Mock service
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.getWorkflow(Matchers.<GithubDetails>anyObject()))
                .thenReturn(mockWorkflow)
                .thenReturn(null);
        when(mockWorkflowService.createWorkflow(anyObject()))
                .thenReturn(mockWorkflow2)
                .thenReturn(null);

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

        // Workflow needs to be created
        mockMvc.perform(get("/workflows/github.com/owner/reponame/tree/branch/path/within/workflow.cwl"))
                .andExpect(status().isOk())
                .andExpect(view().name("workflow"))
                .andExpect(model().attribute("workflow", is(mockWorkflow2)));

        // Error creating workflow
        mockMvc.perform(get("/workflows/github.com/owner/reponame/tree/branch/path/within/badworkflow.cwl"))
                .andExpect(status().isFound())
                .andExpect(flash().attributeExists("errors"))
                .andExpect(redirectedUrl("/?url=https://github.com/owner/reponame/tree/branch/path/within/badworkflow.cwl"));

    }

    /**
     * Displaying directories of workflows
     */
    @Test
    public void directDirectoryURL() throws Exception {

        // Workflow overviews for testing
        WorkflowOverview overview1 = new WorkflowOverview("workflow1.cwl", "label1", "doc1");
        WorkflowOverview overview2 = new WorkflowOverview("workflow2.cwl", "label2", "doc2");

        // Mock service to return these overviews
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.getWorkflowsFromDirectory(anyObject()))
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>(Arrays.asList(overview1)))
                .thenReturn(new ArrayList<>(Arrays.asList(overview1, overview2)))
                .thenReturn(new ArrayList<>(Arrays.asList(overview1, overview2)))
                .thenThrow(new IOException("Error getting contents"));

        // Mock controller/MVC
        WorkflowController workflowController = new WorkflowController(
                Mockito.mock(WorkflowFormValidator.class),
                mockWorkflowService,
                Mockito.mock(GraphVizService.class));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .build();

        // No workflows in directory, redirect with errors
        mockMvc.perform(get("/workflows/github.com/owner/reponame/tree/branch/path/within"))
                .andExpect(status().isFound())
                .andExpect(flash().attributeExists("errors"))
                .andExpect(redirectedUrl("/?url=https://github.com/owner/reponame/tree/branch/path/within"));

        // 1 workflow in directory, redirect to it
        mockMvc.perform(get("/workflows/github.com/common-workflow-language/workflows/tree/master/workflows/lobSTR"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/workflows/github.com/common-workflow-language/workflows/tree/master/workflows/lobSTR/workflow1.cwl"));

        // Multiple workflows in directory, show list
        mockMvc.perform(get("/workflows/github.com/common-workflow-language/workflows/tree/visu/workflows/scidap"))
                .andExpect(status().isOk())
                .andExpect(view().name("selectworkflow"))
                .andExpect(model().attribute("githubDetails", allOf(
                        hasProperty("owner", is("common-workflow-language")),
                        hasProperty("repoName", is("workflows")),
                        hasProperty("branch", is("visu")),
                        hasProperty("path", is("workflows/scidap"))
                )))
                .andExpect(model().attribute("workflowOverviews",
                        containsInAnyOrder(overview1, overview2)));

        // Workflows at the base of a repository
        mockMvc.perform(get("/workflows/github.com/genome/arvados_trial/tree/master"))
                .andExpect(status().isOk())
                .andExpect(view().name("selectworkflow"))
                .andExpect(model().attribute("githubDetails",
                        hasProperty("path", is("/"))));

        // Error getting contents of Github directory, redirect with errors
        mockMvc.perform(get("/workflows/github.com/owner/reponame/tree/branch/path/within"))
                .andExpect(status().isFound())
                .andExpect(flash().attributeExists("errors"))
                .andExpect(redirectedUrl("/?url=https://github.com/owner/reponame/tree/branch/path/within"));

    }

    /**
     * Endpoint for downloading RO bundle for a workflow
     */
    @Test
    public void downloadROBundle() throws Exception {

        // Mock service to return a bundle file and then throw ROBundleNotFoundException
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.getROBundle(anyString()))
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
        mockMvc.perform(get("/workflows/workflowid/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.wf4ever.robundle+zip"));

        // Bundle does not exist, 404 error
        mockMvc.perform(get("/workflows/workflowid/download"))
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
        when(mockWorkflowService.getWorkflow(anyString()))
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
        mockMvc.perform(get("/workflows/workflowid/graph/svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"));

        // Image does not exist, 404 error
        mockMvc.perform(get("/workflows/workflowid/graph/svg"))
                .andExpect(status().isNotFound());

        // Image exists and can be downloaded
        mockMvc.perform(get("/workflows/workflowid/graph/png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        // Image does not exist, 404 error
        mockMvc.perform(get("/workflows/workflowid/graph/png"))
                .andExpect(status().isNotFound());

        // Image exists and can be downloaded
        mockMvc.perform(get("/workflows/workflowid/graph/xdot"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/vnd.graphviz"));

        // Image does not exist, 404 error
        mockMvc.perform(get("/workflows/workflowid/graph/xdot"))
                .andExpect(status().isNotFound());

    }

}