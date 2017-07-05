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

package org.commonwl.view.researchobject;

import org.apache.commons.io.FileUtils;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.commonwl.view.cwl.CWLTool;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.workflow.Workflow;
import org.eclipse.egit.github.core.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class ROBundleServiceTest {

    /**
     * Use a temporary directory for testing
     */
    @Rule
    public TemporaryFolder roBundleFolder = new TemporaryFolder();

    /**
     * Generate a Research Object bundle from lobstr-v1 and check it
     */
    @Test
    public void generateAndSaveROBundle() throws Exception {

        // Get mock Github service
        GitHubService mockGithubService = getMock();

        // Create new RO bundle
        GithubDetails lobSTRv1Details = new GithubDetails("common-workflow-language", "workflows",
                "933bf2a1a1cce32d88f88f136275535da9df0954", "workflows/lobSTR");
        ROBundleService bundleService = new ROBundleService(roBundleFolder.getRoot().toPath(),
                "CWL Viewer", "https://view.commonwl.org", 5242880, mockGithubService,
                Mockito.mock(GraphVizService.class), Mockito.mock(CWLTool.class));
        Workflow lobSTRv1 = Mockito.mock(Workflow.class);
        when(lobSTRv1.getRetrievedFrom()).thenReturn(lobSTRv1Details);
        Bundle bundle = bundleService.newBundleFromGithub(lobSTRv1, lobSTRv1Details);
        Path bundleRoot = bundle.getRoot().resolve("workflow");

        // Check bundle exists
        assertNotNull(bundle);

        // Check basic manifest metadata
        Manifest manifest = bundle.getManifest();
        assertEquals("CWL Viewer", manifest.getCreatedBy().getName());
        assertEquals("https://view.commonwl.org", manifest.getCreatedBy().getUri().toString());
        assertEquals("Mark Robinson", manifest.getAuthoredBy().get(0).getName());
        assertEquals(12, manifest.getAggregates().size());

        // Check cwl aggregation information
        PathMetadata cwlAggregate = manifest.getAggregation(
                bundleRoot.resolve("lobSTR-workflow.cwl"));
        assertEquals("https://raw.githubusercontent.com/common-workflow-language/workflows/933bf2a1a1cce32d88f88f136275535da9df0954/workflows/lobSTR/lobSTR-workflow.cwl",
                cwlAggregate.getRetrievedFrom().toString());
        assertEquals("Mark Robinson", cwlAggregate.getAuthoredBy().get(0).getName());
        assertNull(cwlAggregate.getAuthoredBy().get(0).getOrcid());
        assertEquals("text/x-yaml", cwlAggregate.getMediatype());
        assertEquals("https://w3id.org/cwl/v1.0", cwlAggregate.getConformsTo().toString());

        // Save and check it exists in the temporary folder
        bundleService.saveToFile(bundle);
        File[] fileList =  roBundleFolder.getRoot().listFiles();
        assertTrue(fileList.length == 1);
        for (File ro : fileList) {
            assertTrue(ro.getName().endsWith(".zip"));
            Bundle savedBundle = Bundles.openBundle(ro.toPath());
            assertNotNull(savedBundle);
        }

    }

    /**
     * Test file size limit
     */
    @Test
    public void filesOverLimit() throws Exception {

        // Get mock Github service
        GitHubService mockGithubService = getMock();

        // Create new RO bundle where all files are external
        GithubDetails lobSTRv1Details = new GithubDetails("common-workflow-language", "workflows",
                "933bf2a1a1cce32d88f88f136275535da9df0954", "workflows/lobSTR");
        ROBundleService bundleService = new ROBundleService(roBundleFolder.getRoot().toPath(),
                "CWL Viewer", "https://view.commonwl.org", 0, mockGithubService,
                Mockito.mock(GraphVizService.class), Mockito.mock(CWLTool.class));
        Workflow lobSTRv1 = Mockito.mock(Workflow.class);
        when(lobSTRv1.getRetrievedFrom()).thenReturn(lobSTRv1Details);
        Bundle bundle = bundleService.newBundleFromGithub(lobSTRv1, lobSTRv1Details);

        Manifest manifest = bundle.getManifest();

        // Check files are externally linked in the aggregate
        assertEquals(12, manifest.getAggregates().size());

        PathMetadata urlAggregate = manifest.getAggregation(
                new URI("https://raw.githubusercontent.com/common-workflow-language/workflows/" +
                        "933bf2a1a1cce32d88f88f136275535da9df0954/workflows/lobSTR/models/" +
                        "illumina_v3.pcrfree.stepmodel"));
        assertEquals("Mark Robinson", urlAggregate.getAuthoredBy().get(0).getName());

    }

    /**
     * Get a mock Github service redirecting file downloads to file system
     * and providing translation from the file system to Github API returns
     * @return The constructed mock object
     */
    private GitHubService getMock() throws Exception {

        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        Answer fileAnswer = new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GithubDetails details = (GithubDetails) args[0];
                File workflowFile = new File("src/test/resources/cwl/lobstr-v1/"
                        + details.getPath().replace("workflows/lobSTR/", ""));
                return FileUtils.readFileToString(workflowFile);
            }
        };
        when(mockGithubService.downloadFile(anyObject())).thenAnswer(fileAnswer);
        when(mockGithubService.downloadFile(anyObject(), anyObject())).thenAnswer(fileAnswer);

        Answer contentsAnswer = new Answer<List<RepositoryContents>>() {
            @Override
            public List<RepositoryContents> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GithubDetails details = (GithubDetails) args[0];

                List<RepositoryContents> returnList = new ArrayList<>();

                if (!details.getPath().endsWith("models")) {
                    // Add all files from lobstr-v1 directory
                    File[] fileList = new File("src/test/resources/cwl/lobstr-v1/").listFiles();
                    for (File thisFile : fileList) {
                        RepositoryContents contentsEntry = new RepositoryContents();
                        if (thisFile.isDirectory()) {
                            contentsEntry.setType(GitHubService.TYPE_DIR);
                            contentsEntry.setSize(0);
                        } else if (thisFile.isFile()) {
                            contentsEntry.setType(GitHubService.TYPE_FILE);
                            contentsEntry.setSize(100);
                        }
                        contentsEntry.setName(thisFile.getName());
                        contentsEntry.setPath("workflows/lobSTR/" + thisFile.getName());
                        returnList.add(contentsEntry);
                    }
                } else {
                    // Add all files from lobstr-v1/models subdirectory
                    File[] subDirFileList = new File("src/test/resources/cwl/lobstr-v1/models/").listFiles();
                    for (File thisFile : subDirFileList) {
                        RepositoryContents contentsEntry = new RepositoryContents();
                        contentsEntry.setType(GitHubService.TYPE_FILE);
                        contentsEntry.setName(thisFile.getName());
                        contentsEntry.setSize(100);
                        contentsEntry.setPath("workflows/lobSTR/models/" + thisFile.getName());
                        returnList.add(contentsEntry);
                    }
                }

                return returnList;
            }
        };
        when(mockGithubService.getContents(anyObject())).thenAnswer(contentsAnswer);

        Answer commitsAnswer = new Answer<List<RepositoryCommit>>() {
            @Override
            public List<RepositoryCommit> answer(InvocationOnMock invocation) throws Throwable {
                // Make up a commit for the file and return it
                List<RepositoryCommit> commitList = new ArrayList<>();
                RepositoryCommit commit = new RepositoryCommit();

                User author = new User();
                author.setName("Mark Robinson");
                author.setHtmlUrl("https://github.com/MarkRobbo");
                commit.setAuthor(author);
                commit.setSha("933bf2a1a1cce32d88f88f136275535da9df0954");
                commit.setCommit(new Commit().setAuthor(new CommitUser().setName("Mark Robinson")));
                commitList.add(commit);

                return commitList;
            }
        };
        when(mockGithubService.getCommits(anyObject())).thenAnswer(commitsAnswer);

        return mockGithubService;
    }

}