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
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class WorkflowControllerTest {

    /**
     * Use a temporary directory for testing
     */
    @Rule
    public TemporaryFolder roBundleFolder = new TemporaryFolder();

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
     * Endpoint for displaying workflows and directories of workflows
     */
    @Test
    public void getWorkflowByGithubDetails() throws Exception {

        // Mock service to return a bundle file and then throw ROBundleNotFoundException
        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);


        // Mock controller/MVC
        WorkflowController workflowController = new WorkflowController(
                Mockito.mock(WorkflowFormValidator.class),
                mockWorkflowService,
                Mockito.mock(GraphVizService.class));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .build();

        // Redirect with error
        mockMvc.perform(get("/workflows/github.com/owner/reponame/tree/branch/path/within"))
                .andExpect(status().isFound())
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