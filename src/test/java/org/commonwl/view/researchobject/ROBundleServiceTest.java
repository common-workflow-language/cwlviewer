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

import org.apache.jena.query.ResultSet;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathAnnotation;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.commonwl.view.cwl.CWLTool;
import org.commonwl.view.cwl.RDFService;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.git.GitSemaphore;
import org.commonwl.view.git.GitService;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.workflow.Workflow;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

public class ROBundleServiceTest {

    private static Git gitRepo;

    @Before
    public void setUp() throws Exception {
        Repository mockRepo = Mockito.mock(Repository.class);
        when(mockRepo.getWorkTree()).thenReturn(new File("src/test/resources/cwl/"));

        gitRepo = Mockito.mock(Git.class);
        when(gitRepo.getRepository()).thenReturn(mockRepo);
    }

    /**
     * Use a temporary directory for testing
     */
    @Rule
    public TemporaryFolder roBundleFolder = new TemporaryFolder();

    /**
     * Generate a Research Object bundle from lobstr and check it
     */
    @Test
    public void generateAndSaveROBundle() throws Exception {

        // Get mock Git service
        GitService mockGitService = Mockito.mock(GitService.class);
        when(mockGitService.getRepository(anyObject(), anyBoolean())).thenReturn(gitRepo);

        Set<HashableAgent> authors = new HashSet<>();
        authors.add(new HashableAgent("Mark Robinson", null, new URI("mailto:mark@example.com")));
        when(mockGitService.getAuthors(anyObject(), anyObject()))
                .thenReturn(authors);

        // Mock Graphviz service
        GraphVizService mockGraphvizService = Mockito.mock(GraphVizService.class);
        when(mockGraphvizService.getGraph(anyString(), anyString(), anyString()))
                .thenReturn(new File("src/test/resources/graphviz/testVis.png"))
                .thenReturn(new File("src/test/resources/graphviz/testVis.svg"));

        // Mock CWLTool
        CWLTool mockCwlTool = Mockito.mock(CWLTool.class);
        when(mockCwlTool.getPackedVersion(anyString()))
                .thenReturn("cwlVersion: v1.0");

        // Mock RDF Service
        ResultSet emptyResult = Mockito.mock(ResultSet.class);
        when(emptyResult.hasNext()).thenReturn(false);
        RDFService mockRdfService = Mockito.mock(RDFService.class);
        when(mockRdfService.getAuthors(anyString(), anyString())).thenReturn(emptyResult);
        when(mockRdfService.graphExists(anyString()))
                .thenReturn(true);
        when(mockRdfService.getModel(anyObject(), anyObject()))
                .thenReturn("@prefix cwl: <https://w3id.org/cwl/cwl#> .".getBytes());

        // Workflow details
        GitDetails lobSTRv1Details = new GitDetails("https://github.com/common-workflow-language/workflows.git",
                "933bf2a1a1cce32d88f88f136275535da9df0954", "workflows/lobSTR/lobSTR-workflow.cwl");
        Workflow lobSTRv1 = Mockito.mock(Workflow.class);
        when(lobSTRv1.getID()).thenReturn("testID");
        when(lobSTRv1.getRetrievedFrom()).thenReturn(lobSTRv1Details);

        // RO details
        GitDetails lobSTRv1RODetails = new GitDetails("https://github.com/common-workflow-language/workflows.git",
                "933bf2a1a1cce32d88f88f136275535da9df0954", "lobstr-draft3/");

        // Create new RO bundle
        ROBundleService bundleService = new ROBundleService(roBundleFolder.getRoot().toPath(),
                "CWL Viewer", "https://view.commonwl.org", 5242880,
                mockGraphvizService, mockGitService, mockRdfService,
                Mockito.mock(GitSemaphore.class), mockCwlTool);
        Bundle bundle = bundleService.createBundle(lobSTRv1, lobSTRv1RODetails);
        Path bundleRoot = bundle.getRoot().resolve("workflow");

        // Check bundle exists
        assertNotNull(bundle);

        // Check basic manifest metadata
        Manifest manifest = bundle.getManifest();
        assertEquals("CWL Viewer", manifest.getCreatedBy().getName());
        assertEquals("https://view.commonwl.org", manifest.getCreatedBy().getUri().toString());
        assertEquals("Mark Robinson", manifest.getAuthoredBy().get(0).getName());
        assertEquals(14, manifest.getAggregates().size());

        // Check cwl aggregation information
        PathMetadata cwlAggregate = manifest.getAggregation(
                bundleRoot.resolve("lobSTR-workflow.cwl"));
        assertEquals("https://w3id.org/cwl/v/git/null/workflows/lobSTR/lobSTR-workflow.cwl?format=raw",
                cwlAggregate.getRetrievedFrom().toString());
        assertEquals("Mark Robinson", cwlAggregate.getAuthoredBy().get(0).getName());
        assertEquals("mailto:mark@example.com", cwlAggregate.getAuthoredBy().get(0).getUri().toString());
        assertNull(cwlAggregate.getAuthoredBy().get(0).getOrcid());
        assertEquals("text/x-yaml", cwlAggregate.getMediatype());
        assertEquals("https://w3id.org/cwl/draft-3", cwlAggregate.getConformsTo().toString());

        // Check visualisations exist as aggregates
        PathMetadata pngAggregate = manifest.getAggregation(bundleRoot.resolve("visualisation.png"));
        assertEquals("image/png", pngAggregate.getMediatype());
        PathMetadata svgAggregate = manifest.getAggregation(bundleRoot.resolve("visualisation.svg"));
        assertEquals("image/svg+xml", svgAggregate.getMediatype());

        // Check RDF and packed workflows exist as annotations
        List<PathAnnotation> annotations = manifest.getAnnotations();
        assertEquals(2, annotations.size());
        assertEquals(new URI("annotations/merged.cwl"), annotations.get(0).getContent());
        assertEquals(new URI("annotations/workflow.ttl"), annotations.get(1).getContent());

        // Check git2prov link is in the history
        List<Path> history = manifest.getHistory();
        assertEquals(1, history.size());
        assertEquals("http:/git2prov.org/git2prov?giturl=https:/github.com/common-workflow-language/workflows.git&serialization=PROV-JSON",
                history.get(0).toString());

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

        // Get mock Git service
        GitService mockGitService = Mockito.mock(GitService.class);
        when(mockGitService.getRepository(anyObject(), anyBoolean())).thenReturn(gitRepo);

        Set<HashableAgent> authors = new HashSet<>();
        authors.add(new HashableAgent("Mark Robinson", null, new URI("mailto:mark@example.com")));
        when(mockGitService.getAuthors(anyObject(), anyObject()))
                .thenReturn(authors);

        // Mock Graphviz service
        GraphVizService mockGraphvizService = Mockito.mock(GraphVizService.class);
        when(mockGraphvizService.getGraph(anyString(), anyString(), anyString()))
                .thenReturn(new File("src/test/resources/graphviz/testVis.png"))
                .thenReturn(new File("src/test/resources/graphviz/testVis.svg"));

        // Mock CWLTool
        CWLTool mockCwlTool = Mockito.mock(CWLTool.class);
        when(mockCwlTool.getPackedVersion(anyString()))
                .thenReturn("cwlVersion: v1.0");
        when(mockCwlTool.getRDF(anyString()))
                .thenReturn("@prefix cwl: <https://w3id.org/cwl/cwl#> .");

        // Mock RDF Service
        ResultSet emptyResult = Mockito.mock(ResultSet.class);
        when(emptyResult.hasNext()).thenReturn(false);
        RDFService mockRdfService = Mockito.mock(RDFService.class);
        when(mockRdfService.getAuthors(anyString(), anyString())).thenReturn(emptyResult);

        // Workflow details
        GitDetails lobSTRv1Details = new GitDetails("https://github.com/common-workflow-language/workflows.git",
                "933bf2a1a1cce32d88f88f136275535da9df0954", "workflows/lobSTR/lobSTR-workflow.cwl");
        Workflow lobSTRv1 = Mockito.mock(Workflow.class);
        when(lobSTRv1.getID()).thenReturn("testID");
        when(lobSTRv1.getRetrievedFrom()).thenReturn(lobSTRv1Details);

        // RO details
        GitDetails lobSTRv1RODetails = new GitDetails("https://github.com/common-workflow-language/workflows.git",
                "933bf2a1a1cce32d88f88f136275535da9df0954", "lobstr-draft3/lobSTR-workflow.cwl");

        // Create new RO bundle
        ROBundleService bundleService = new ROBundleService(roBundleFolder.getRoot().toPath(),
                "CWL Viewer", "https://view.commonwl.org", 0, mockGraphvizService,
                mockGitService, mockRdfService, Mockito.mock(GitSemaphore.class), mockCwlTool);
        Bundle bundle = bundleService.createBundle(lobSTRv1, lobSTRv1RODetails);

        Manifest manifest = bundle.getManifest();

        // Check files are externally linked in the aggregate
        assertEquals(14, manifest.getAggregates().size());

        PathMetadata urlAggregate = manifest.getAggregation(
                new URI("https://raw.githubusercontent.com/common-workflow-language/workflows/933bf2a1a1cce32d88f88f136275535da9df0954/lobstr-draft3/lobSTR-workflow.cwl/models/illumina_v3.pcrfree.stepmodel"));
        assertEquals("Mark Robinson", urlAggregate.getAuthoredBy().get(0).getName());

    }

}