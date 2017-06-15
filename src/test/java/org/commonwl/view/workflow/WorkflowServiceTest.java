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
import org.commonwl.view.cwl.CWLTool;
import org.commonwl.view.cwl.RDFService;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.eclipse.egit.github.core.RepositoryContents;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
     * Getting a list of workflow overviews from a directory
     */
    @Test
    public void getWorkflowsFromDirectory() throws Exception {

        // Mock Github service redirecting content query to the filesystem
        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        Answer contentsAnswer = new Answer<List<RepositoryContents>>() {
            @Override
            public List<RepositoryContents> answer(InvocationOnMock invocation) throws Throwable {
                List<RepositoryContents> returnList = new ArrayList<>();

                // Add all files from lobstr-v1 directory
                File[] fileList = new File("src/test/resources/cwl/lobstr-v1/").listFiles();
                for (File thisFile : fileList) {
                    RepositoryContents contentsEntry = new RepositoryContents();
                    if (thisFile.isFile()) {
                        contentsEntry.setType(GitHubService.TYPE_FILE);
                        contentsEntry.setSize(100);
                        contentsEntry.setName(thisFile.getName());
                        contentsEntry.setPath("workflows/lobSTR/" + thisFile.getName());
                        returnList.add(contentsEntry);
                    }
                }

                return returnList;
            }
        };
        when(mockGithubService.getContents(anyObject())).thenAnswer(contentsAnswer);

        // Mock CWL service which returns simple overview once simulating 1 workflow found
        CWLService mockCWLService = Mockito.mock(CWLService.class);
        when(mockCWLService.getWorkflowOverview(anyObject()))
                .thenReturn(new WorkflowOverview("workflow.cwl", "label", "doc"))
                .thenReturn(new WorkflowOverview("workflow2.cwl", "label2", "doc2"))
                .thenReturn(null);

        // Create service under test
        WorkflowService testWorkflowService = new WorkflowService(
                mockGithubService, mockCWLService,
                Mockito.mock(CWLTool.class), Mockito.mock(RDFService.class),
                Mockito.mock(WorkflowRepository.class), Mockito.mock(ROBundleFactory.class),
                Mockito.mock(GraphVizService.class), 1);

        // Get a list of workflows from the directory
        List<WorkflowOverview> list = testWorkflowService.getWorkflowsFromDirectory(
                Mockito.mock(GithubDetails.class));

        // 1 workflow should be found
        assertTrue(list.size() == 2);
        assertEquals("workflow.cwl", list.get(0).getFileName());
        assertEquals("label", list.get(0).getLabel());
        assertEquals("doc", list.get(0).getDoc());

        assertEquals("workflow2.cwl", list.get(1).getFileName());
        assertEquals("label2", list.get(1).getLabel());
        assertEquals("doc2", list.get(1).getDoc());

    }

    /**
     * Getting a workflow when cache has expired
     * And a new workflow needs to be created
     */
    @Test
    public void getWorkflowCacheHasExpired() throws Exception {

        Workflow oldWorkflow = new Workflow("old", "This is the expired workflow",
                new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
        oldWorkflow.setId("theworkflowid");
        oldWorkflow.setRetrievedOn(new Date());
        oldWorkflow.setRetrievedFrom(Mockito.mock(GithubDetails.class));
        oldWorkflow.setLastCommit("d46ce365f1a10c4c4d6b0caed51c6f64b84c2f63");
        oldWorkflow.setRoBundle(roBundleFolder.newFile("robundle.zip").getAbsolutePath());

        Workflow updatedWorkflow = new Workflow("new", "This is the updated workflow",
                new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
        updatedWorkflow.setId("newworkflowid");

        WorkflowRepository mockWorkflowRepo = Mockito.mock(WorkflowRepository.class);
        when(mockWorkflowRepo.findByRetrievedFrom(anyObject())).thenReturn(oldWorkflow);

        CWLService mockCWLService = Mockito.mock(CWLService.class);
        when(mockCWLService.parseWorkflow(anyObject(), anyString())).thenReturn(updatedWorkflow);

        // Create service under test with negative cache time (always create new workflow)
        WorkflowService testWorkflowService = new WorkflowService(
                Mockito.mock(GitHubService.class), mockCWLService,
                Mockito.mock(CWLTool.class), Mockito.mock(RDFService.class),
                mockWorkflowRepo, Mockito.mock(ROBundleFactory.class),
                Mockito.mock(GraphVizService.class), -1);

        // Will use check cache algorithm, find expired,
        // check github and find commit IDs do not match,
        // and thus create a new workflow + matching RO bundle
        Workflow workflow = testWorkflowService.getWorkflow(Mockito.mock(GithubDetails.class));

        // Check the new workflow was returned
        assertEquals("newworkflowid", workflow.getID());
        assertEquals("new", workflow.getLabel());
        assertEquals("This is the updated workflow", workflow.getDoc());

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
        workflow.setRoBundle(roBundlePath);

        WorkflowRepository mockWorkflowRepo = Mockito.mock(WorkflowRepository.class);
        when(mockWorkflowRepo.findOne(anyString())).thenReturn(workflow);

        // Create service under test
        WorkflowService testWorkflowService = new WorkflowService(
                Mockito.mock(GitHubService.class), Mockito.mock(CWLService.class),
                Mockito.mock(CWLTool.class), Mockito.mock(RDFService.class),
                mockWorkflowRepo, Mockito.mock(ROBundleFactory.class),
                Mockito.mock(GraphVizService.class), -1);

        File fetchedBundle = testWorkflowService.getROBundle("workflowid");
        assertEquals(roBundlePath, fetchedBundle.getAbsolutePath());

    }

}