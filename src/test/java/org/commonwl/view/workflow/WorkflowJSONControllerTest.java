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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.commonwl.view.cwl.CWLToolStatus;
import org.commonwl.view.cwl.CWLValidationException;
import org.commonwl.view.git.GitDetails;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** API testing */
public class WorkflowJSONControllerTest {

  @Test
  public void newWorkflowFromGithubURLJson() throws Exception {

    // Validator pass or fail
    WorkflowFormValidator mockValidator = Mockito.mock(WorkflowFormValidator.class);
    when(mockValidator.validateAndParse(any(), any()))
        .thenReturn(null)
        .thenReturn(
            new GitDetails("https://github.com/owner/repoName.git", "branch", "path/workflow.cwl"))
        .thenReturn(
            new GitDetails("https://github.com/owner/repoName.git", "branch", "path/workflow.cwl"));

    // The eventual accepted valid workflow
    Workflow mockWorkflow = Mockito.mock(Workflow.class);
    when(mockWorkflow.getRetrievedFrom())
        .thenReturn(
            new GitDetails("https://github.com/owner/repoName.git", "branch", "path/workflow.cwl"));
    QueuedWorkflow mockQueuedWorkflow = Mockito.mock(QueuedWorkflow.class);
    when(mockQueuedWorkflow.getId()).thenReturn("123");
    when(mockQueuedWorkflow.getTempRepresentation()).thenReturn(mockWorkflow);
    List<WorkflowOverview> listOfTwoOverviews = new ArrayList<>();
    listOfTwoOverviews.add(new WorkflowOverview("#packedId1", "label", "doc"));
    listOfTwoOverviews.add(new WorkflowOverview("#packedId2", "label2", "doc2"));
    when(mockQueuedWorkflow.getWorkflowList())
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(Collections.singletonList(new WorkflowOverview("#packedId", "Label", "Doc")))
        .thenReturn(listOfTwoOverviews);

    // Mock workflow service returning valid workflow
    WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
    when(mockWorkflowService.getWorkflow(any(GitDetails.class)))
        .thenReturn(mockWorkflow)
        .thenReturn(null);
    when(mockWorkflowService.createQueuedWorkflow(any()))
        .thenThrow(new CWLValidationException("Error"))
        .thenReturn(mockQueuedWorkflow);

    // Mock controller/MVC
    WorkflowJSONController workflowJSONController =
        new WorkflowJSONController(mockValidator, mockWorkflowService);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(workflowJSONController).build();

    // Error in validation
    mockMvc
        .perform(post("/workflows").param("url", "invalidurl").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message", is("Error: Could not parse workflow details from URL")));

    // Workflow already exists
    mockMvc
        .perform(
            post("/workflows")
                .param("url", "https://github.com/owner/repoName/tree/branch/path/workflow.cwl")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isSeeOther())
        .andExpect(
            header()
                .string(
                    "Location",
                    is("/workflows/github.com/owner/repoName/blob/branch/path/workflow.cwl")));

    // Error creating the workflow
    mockMvc
        .perform(
            post("/workflows")
                .param("url", "https://github.com/owner/repoName/tree/branch/path/workflow.cwl")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Success
    mockMvc
        .perform(
            post("/workflows")
                .param("url", "https://github.com/owner/repoName/tree/branch/path/success.cwl")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(header().string("Location", is("/queue/123")));

    // Packed workflow with one ID is still accepted and parsed using that ID
    mockMvc
        .perform(
            post("/workflows")
                .param("url", "https://github.com/owner/repoName/tree/branch/path/singlePacked.cwl")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(header().string("Location", is("/queue/123")));

    // Packed workflow with multiple IDs is unprocessable
    mockMvc
        .perform(
            post("/workflows")
                .param(
                    "url", "https://github.com/owner/repoName/tree/branch/path/multiplePacked.cwl")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath(
                "$.message",
                is(
                    "This workflow file is packed and contains multiple workflow "
                        + "descriptions. Please provide a packedId parameter with one of the following")))
        .andExpect(jsonPath("$.packedId", containsInAnyOrder("packedId1", "packedId2")));
  }

  /** Get a workflow from the database and return the JSON format */
  @Test
  public void getWorkflowByGithubDetailsJson() throws Exception {

    Workflow workflow1 = new Workflow("label", "doc", null, null, null);
    workflow1.setRetrievedFrom(
        new GitDetails("https://github.com/owner/repo.git", "branch", "path/to/workflow.cwl"));

    WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
    when(mockWorkflowService.getWorkflow(any(GitDetails.class))).thenReturn(workflow1);

    WorkflowJSONController workflowJSONController =
        new WorkflowJSONController(Mockito.mock(WorkflowFormValidator.class), mockWorkflowService);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(workflowJSONController).build();

    mockMvc
        .perform(
            get("/workflows/github.com/owner/repo/blob/branch/path/to/workflow.cwl")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.retrievedFrom.repoUrl", is("https://github.com/owner/repo.git")))
        .andExpect(jsonPath("$.retrievedFrom.branch", is("branch")))
        .andExpect(jsonPath("$.retrievedFrom.path", is("path/to/workflow.cwl")))
        .andExpect(jsonPath("$.label", is("label")))
        .andExpect(jsonPath("$.doc", is("doc")))
        .andExpect(
            jsonPath(
                "visualisationPng",
                is("/graph/png/github.com/owner/repo/blob/branch/path/to/workflow.cwl")))
        .andExpect(
            jsonPath(
                "visualisationSvg",
                is("/graph/svg/github.com/owner/repo/blob/branch/path/to/workflow.cwl")));
  }

  /** Checks the queue for a workflow */
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
    Workflow wfSuccess = new Workflow(null, null, null, null, null);
    wfSuccess.setRetrievedFrom(
        new GitDetails("https://github.com/owner/repoName.git", "branch", "path/to/workflow.cwl"));
    qwfSuccess.setTempRepresentation(wfSuccess);

    WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
    when(mockWorkflowService.getQueuedWorkflow(any(String.class)))
        .thenReturn(null)
        .thenReturn(qwfRunning)
        .thenReturn(qwfError)
        .thenReturn(qwfSuccess);

    WorkflowJSONController workflowJSONController =
        new WorkflowJSONController(Mockito.mock(WorkflowFormValidator.class), mockWorkflowService);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(workflowJSONController).build();

    // No workflow
    mockMvc
        .perform(get("/queue/123").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());

    // Running workflow
    mockMvc
        .perform(get("/queue/123").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.cwltoolStatus", is("RUNNING")))
        .andExpect(jsonPath("$.cwltoolVersion", is("v1.0")));

    // Error workflow
    mockMvc
        .perform(get("/queue/123").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.cwltoolStatus", is("ERROR")))
        .andExpect(jsonPath("$.message", is("cwltool error message")))
        .andExpect(jsonPath("$.cwltoolVersion", is("v1.0")));

    // Success workflow
    mockMvc
        .perform(get("/queue/123").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isSeeOther())
        .andExpect(
            header()
                .string(
                    "Location",
                    is("/workflows/github.com/owner/repoName/blob/branch/path/to/workflow.cwl")));
  }
}
