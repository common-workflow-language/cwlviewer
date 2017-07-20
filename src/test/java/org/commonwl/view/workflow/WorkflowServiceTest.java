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

import org.commonwl.view.cwl.CWLService;
import org.commonwl.view.cwl.CWLToolRunner;
import org.commonwl.view.github.GitDetails;
import org.commonwl.view.github.GitService;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
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

        GitDetails githubInfo = new GitDetails("owner", "branch", "sha", "path");

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

        GitService mockGithubService = Mockito.mock(GitService.class);
        when(mockGithubService.getCommitSha(anyObject())).thenReturn("master");

        CWLService mockCWLService = Mockito.mock(CWLService.class);
        when(mockCWLService.parseWorkflowNative(anyObject(), anyString())).thenReturn(updatedWorkflow);

        // Create service under test with negative cache time (always create new workflow)
        WorkflowService testWorkflowService = new WorkflowService(
                mockGithubService, mockCWLService,
                mockWorkflowRepo, Mockito.mock(QueuedWorkflowRepository.class),
                Mockito.mock(ROBundleFactory.class),
                Mockito.mock(GraphVizService.class),
                Mockito.mock(CWLToolRunner.class), -1);

        // Will use check cache algorithm, find expired,
        // check github and find commit IDs do not match,
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
        String roBundlePath = roBundleFolder.newFile("bundle.zip").getAbsolutePath();
        workflow.setRoBundlePath(roBundlePath);

        WorkflowRepository mockWorkflowRepo = Mockito.mock(WorkflowRepository.class);
        when(mockWorkflowRepo.findByRetrievedFrom(anyObject())).thenReturn(workflow);

        // Create service under test
        WorkflowService testWorkflowService = new WorkflowService(
                Mockito.mock(GitService.class), Mockito.mock(CWLService.class),
                mockWorkflowRepo, Mockito.mock(QueuedWorkflowRepository.class),
                Mockito.mock(ROBundleFactory.class),
                Mockito.mock(GraphVizService.class),
                Mockito.mock(CWLToolRunner.class), -1);

        File fetchedBundle = testWorkflowService.getROBundle(Mockito.mock(GitDetails.class));
        assertEquals(roBundlePath, fetchedBundle.getAbsolutePath());

    }

}