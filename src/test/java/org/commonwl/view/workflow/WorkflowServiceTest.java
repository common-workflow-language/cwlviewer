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

import org.commonwl.view.cwl.CWLToolRunner;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.git.GitService;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class WorkflowServiceTest {

    /**
     * Folder for test research object bundles
     */
    @Rule
    public TemporaryFolder roBundleFolder = new TemporaryFolder();

    /**
     * Getting a workflow when cache has expired
     * And a new workflow needs to be created
     */
    @Test
    public void getWorkflowCacheHasExpired() throws Exception {

        GitDetails githubInfo = new GitDetails("https://github.com/owner/repoName.git", "sha", "path");

        Workflow oldWorkflow = new Workflow("old", "This is the expired workflow",
                new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
        oldWorkflow.setId("theworkflowid");
        oldWorkflow.setRetrievedOn(new Date());
        oldWorkflow.setRetrievedFrom(githubInfo);
        oldWorkflow.setLastCommit("d46ce365f1a10c4c4d6b0caed51c6f64b84c2f63");
        oldWorkflow.setRoBundlePath(roBundleFolder.newFile("robundle.zip").getAbsolutePath());

        Workflow updatedWorkflow = new Workflow("new", "This is the updated workflow",
                new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
        updatedWorkflow.setId("newworkflowid");

        WorkflowRepository mockWorkflowRepo = Mockito.mock(WorkflowRepository.class);
        when(mockWorkflowRepo.findByRetrievedFrom(anyObject())).thenReturn(oldWorkflow);

        Repository mockRepo = Mockito.mock(Repository.class);
        when(mockRepo.getWorkTree()).thenReturn(new File("src/test/resources/cwl/make_to_cwl/dna.cwl"));

        Git mockGitRepo = Mockito.mock(Git.class);
        when(mockGitRepo.getRepository()).thenReturn(mockRepo);

        GitService mockGitService = Mockito.mock(GitService.class);
        when(mockGitService.getRepository(anyObject())).thenReturn(mockGitRepo);
        when(mockGitService.getCurrentCommitID(anyObject())).thenReturn("newCommitId");

        // Create service under test with negative cache time (always create new workflow)
        WorkflowService testWorkflowService = new WorkflowService(
                mockGitService, mockWorkflowRepo, Mockito.mock(QueuedWorkflowRepository.class),
                Mockito.mock(ROBundleFactory.class),
                Mockito.mock(GraphVizService.class),
                Mockito.mock(CWLToolRunner.class), -1);

        // Will use check cache algorithm, find expired,
        // check git and find commit IDs do not match,
        // and thus create a new workflow + matching RO bundle
        Workflow workflow = testWorkflowService.getWorkflow(githubInfo);

        // Check the old workflow was deleted
        // TODO: check new workflow was queued
        assertEquals(workflow, null);

    }

    /**
     * Get the research object bundle associated with a workflow
     * TODO: Test retry for generation within this method
     */
    @Test
    public void getROBundle() throws Exception {

        Workflow workflow = new Workflow("Label", "Doc for the workflow",
                new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
        workflow.setRetrievedFrom(new GitDetails("url", "commitID", "path"));
        workflow.setLastCommit("commitID");

        String roBundlePath = roBundleFolder.newFile("bundle.zip").getAbsolutePath();
        workflow.setRoBundlePath(roBundlePath);

        WorkflowRepository mockWorkflowRepo = Mockito.mock(WorkflowRepository.class);
        when(mockWorkflowRepo.findByRetrievedFrom(anyObject())).thenReturn(workflow);

        // Create service under test
        WorkflowService testWorkflowService = new WorkflowService(
                Mockito.mock(GitService.class),
                mockWorkflowRepo, Mockito.mock(QueuedWorkflowRepository.class),
                Mockito.mock(ROBundleFactory.class),
                Mockito.mock(GraphVizService.class),
                Mockito.mock(CWLToolRunner.class), -1);

        File fetchedBundle = testWorkflowService.getROBundle(Mockito.mock(GitDetails.class));
        assertEquals(roBundlePath, fetchedBundle.getAbsolutePath());

    }

}