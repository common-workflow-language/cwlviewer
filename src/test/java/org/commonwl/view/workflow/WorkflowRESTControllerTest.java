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

import org.commonwl.view.cwl.CWLToolStatus;
import org.commonwl.view.github.GithubDetails;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API testing
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkflowRESTControllerTest {

    /**
     * Get a workflow from the database and return the JSON format
     */
    @Test
    public void getWorkflowByGithubDetailsJson() throws Exception {

        Workflow workflow1 = new Workflow("label", "doc", null, null, null, null);
        workflow1.setRetrievedFrom(new GithubDetails("owner", "repo", "branch", "path/to/workflow.cwl"));

        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.getWorkflow(any(GithubDetails.class))).thenReturn(workflow1);

        WorkflowRESTController workflowRESTController = new WorkflowRESTController(
                Mockito.mock(WorkflowFormValidator.class),
                mockWorkflowService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowRESTController)
                .build();

        mockMvc.perform(
                get("/workflows/github.com/owner/repo/blob/branch/path/to/workflow.cwl")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.retrievedFrom.owner", is("owner")))
                .andExpect(jsonPath("$.retrievedFrom.repoName", is("repo")))
                .andExpect(jsonPath("$.retrievedFrom.branch", is("branch")))
                .andExpect(jsonPath("$.retrievedFrom.path", is("path/to/workflow.cwl")))
                .andExpect(jsonPath("$.label", is("label")))
                .andExpect(jsonPath("$.doc", is("doc")))
                .andExpect(jsonPath("visualisationPng", is("/graph/png/github.com/owner/repo/tree/branch/path/to/workflow.cwl")))
                .andExpect(jsonPath("visualisationSvg", is("/graph/svg/github.com/owner/repo/tree/branch/path/to/workflow.cwl")));
    }

    /**
     * Checks the queue for a workflow
     */
    @Test
    public void checkQueue() throws Exception {

        QueuedWorkflow qwfRunning = new QueuedWorkflow();
        qwfRunning.setCwltoolStatus(CWLToolStatus.RUNNING);
        qwfRunning.setCwltoolVersion("v1.0");

        QueuedWorkflow qwfError = new QueuedWorkflow();
        qwfError.setCwltoolStatus(CWLToolStatus.ERROR);
        qwfError.setCwltoolVersion("v1.0");
        qwfError.setMessage("cwltool error message");

        QueuedWorkflow qwfSuccess = new QueuedWorkflow();
        qwfSuccess.setCwltoolStatus(CWLToolStatus.SUCCESS);
        Workflow wfSuccess = new Workflow(null, null, null, null, null, null);
        wfSuccess.setRetrievedFrom(new GithubDetails("owner", "repo", "branch", "path/to/workflow.cwl"));
        qwfSuccess.setTempRepresentation(wfSuccess);

        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.getQueuedWorkflow(anyString()))
                .thenReturn(null)
                .thenReturn(qwfRunning)
                .thenReturn(qwfError)
                .thenReturn(qwfSuccess);

        WorkflowRESTController workflowRESTController = new WorkflowRESTController(
                Mockito.mock(WorkflowFormValidator.class),
                mockWorkflowService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(workflowRESTController)
                .build();

        // No workflow
        mockMvc.perform(
                get("/queue/123")
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());

        // Running workflow
        mockMvc.perform(
                get("/queue/123")
                    .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.cwltoolStatus", is("RUNNING")))
                .andExpect(jsonPath("$.cwltoolVersion", is("v1.0")));

        // Error workflow
        mockMvc.perform(
                get("/queue/123")
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.cwltoolStatus", is("ERROR")))
                .andExpect(jsonPath("$.message", is("cwltool error message")))
                .andExpect(jsonPath("$.cwltoolVersion", is("v1.0")));

        // Success workflow
        mockMvc.perform(
                get("/queue/123")
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", is("/workflows/github.com/owner/repo/tree/branch/path/to/workflow.cwl")));
    }

}
