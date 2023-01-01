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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.commonwl.view.cwl.CWLService;
import org.commonwl.view.cwl.CWLToolRunner;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.git.GitSemaphore;
import org.commonwl.view.git.GitService;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

public class WorkflowServiceTest {

  /** Folder for test research object bundles */
  @TempDir public Path roBundleFolder;

  /** Retry the running of cwltool */
  @Test
  public void retryCwltoolGeneration() throws Exception {}

  /** Getting a list of workflow overviews from a directory */
  @Test
  public void getWorkflowsFromDirectory() throws Exception {

    // Mock CWL service which returns simple overview once simulating 1 workflow found
    CWLService mockCWLService = Mockito.mock(CWLService.class);
    when(mockCWLService.getWorkflowOverview(any()))
        .thenReturn(new WorkflowOverview("workflow.cwl", "label", "doc"))
        .thenReturn(new WorkflowOverview("workflow2.cwl", "label2", "doc2"))
        .thenReturn(null);

    Repository mockRepo = Mockito.mock(Repository.class);
    when(mockRepo.getWorkTree()).thenReturn(new File("src/test/resources/cwl/hello"));

    Git mockGitRepo = Mockito.mock(Git.class);
    when(mockGitRepo.getRepository()).thenReturn(mockRepo);

    GitService mockGitService = Mockito.mock(GitService.class);
    when(mockGitService.getRepository(any(GitDetails.class), any(Boolean.class)))
        .thenReturn(mockGitRepo);

    // Create service under test
    WorkflowService testWorkflowService =
        new WorkflowService(
            mockGitService,
            mockCWLService,
            Mockito.mock(WorkflowRepository.class),
            Mockito.mock(QueuedWorkflowRepository.class),
            Mockito.mock(ROBundleFactory.class),
            Mockito.mock(GraphVizService.class),
            Mockito.mock(CWLToolRunner.class),
            Mockito.mock(GitSemaphore.class),
            1);

    // Get a list of workflows from the directory
    List<WorkflowOverview> list =
        testWorkflowService.getWorkflowsFromDirectory(new GitDetails(null, null, "/"));

    // 1 workflow should be found
    assertEquals(2, list.size());
    assertEquals("workflow.cwl", list.get(0).getFileName());
    assertEquals("label", list.get(0).getLabel());
    assertEquals("doc", list.get(0).getDoc());

    assertEquals("workflow2.cwl", list.get(1).getFileName());
    assertEquals("label2", list.get(1).getLabel());
    assertEquals("doc2", list.get(1).getDoc());
  }

  /** Getting a workflow when cache has expired And a new workflow needs to be created */
  @Test
  public void getWorkflowCacheHasExpired() throws Exception {

    GitDetails githubInfo =
        new GitDetails(
            "https://github.com/common-workflow-language/workflows.git", "master", "dna.cwl");

    Workflow oldWorkflow =
        new Workflow(
            "old",
            "This is the expired workflow",
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>());
    oldWorkflow.setId("theworkflowid");
    oldWorkflow.setRetrievedOn(new Date());
    oldWorkflow.setRetrievedFrom(githubInfo);
    oldWorkflow.setLastCommit("d46ce365f1a10c4c4d6b0caed51c6f64b84c2f63");
    oldWorkflow.setRoBundlePath(
        Files.createFile(roBundleFolder.resolve("robundle.zip")).toAbsolutePath().toString());

    Workflow updatedWorkflow =
        new Workflow(
            "new",
            "This is the updated workflow",
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>());
    updatedWorkflow.setId("newworkflowid");

    WorkflowRepository mockWorkflowRepo = Mockito.mock(WorkflowRepository.class);
    when(mockWorkflowRepo.findByRetrievedFrom(any())).thenReturn(oldWorkflow);

    CWLService mockCWLService = Mockito.mock(CWLService.class);
    when(mockCWLService.parseWorkflowNative(any(), any())).thenReturn(updatedWorkflow);

    Repository mockRepo = Mockito.mock(Repository.class);
    when(mockRepo.getWorkTree()).thenReturn(new File("src/test/resources/cwl/make_to_cwl"));

    Git mockGitRepo = Mockito.mock(Git.class);
    when(mockGitRepo.getRepository()).thenReturn(mockRepo);

    GitService mockGitService = Mockito.mock(GitService.class);
    when(mockGitService.getRepository(any(GitDetails.class), any(Boolean.class)))
        .thenReturn(mockGitRepo);
    when(mockGitService.getCurrentCommitID(any())).thenReturn("newCommitId");

    // Create service under test with negative cache time (always create new workflow)
    WorkflowService testWorkflowService =
        new WorkflowService(
            mockGitService,
            mockCWLService,
            mockWorkflowRepo,
            Mockito.mock(QueuedWorkflowRepository.class),
            Mockito.mock(ROBundleFactory.class),
            Mockito.mock(GraphVizService.class),
            Mockito.mock(CWLToolRunner.class),
            Mockito.mock(GitSemaphore.class),
            -1);

    // Will use check cache algorithm, find expired,
    // check git and find commit IDs do not match,
    // and thus create a new workflow + matching RO bundle
    Workflow workflow = testWorkflowService.getWorkflow(githubInfo);

    // Check the old workflow was deleted
    // TODO: check new workflow was queued
    assertNull(workflow);
  }

  /**
   * Get the research object bundle associated with a workflow TODO: Test retry for generation
   * within this method
   */
  @Test
  public void getROBundle() throws Exception {

    Workflow workflow =
        new Workflow(
            "Label", "Doc for the workflow", new HashMap<>(), new HashMap<>(), new HashMap<>());
    workflow.setRetrievedFrom(new GitDetails("url", "commitID", "path"));
    workflow.setLastCommit("commitID");

    String roBundlePath =
        Files.createFile(roBundleFolder.resolve("bundle.zip")).toAbsolutePath().toString();
    workflow.setRoBundlePath(roBundlePath);

    WorkflowRepository mockWorkflowRepo = Mockito.mock(WorkflowRepository.class);
    when(mockWorkflowRepo.findByRetrievedFrom(any())).thenReturn(workflow);

    // Create service under test
    WorkflowService testWorkflowService =
        new WorkflowService(
            Mockito.mock(GitService.class),
            Mockito.mock(CWLService.class),
            mockWorkflowRepo,
            Mockito.mock(QueuedWorkflowRepository.class),
            Mockito.mock(ROBundleFactory.class),
            Mockito.mock(GraphVizService.class),
            Mockito.mock(CWLToolRunner.class),
            Mockito.mock(GitSemaphore.class),
            -1);

    File fetchedBundle = testWorkflowService.getROBundle(null);
    assertEquals(roBundlePath, fetchedBundle.getAbsolutePath());
  }
}
