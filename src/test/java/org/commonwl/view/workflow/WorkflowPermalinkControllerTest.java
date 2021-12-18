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

import org.apache.commons.io.IOUtils;
import org.commonwl.view.cwl.RDFService;
import org.commonwl.view.git.GitDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.core.io.PathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the controller for workflow related functionality
 */
public class WorkflowPermalinkControllerTest {

    @TempDir
    private Path tempDir;
    private MockMvc mockMvc;
    private byte[] rdfResponse;
    private final PathResource png = new PathResource(Paths.get("src/test/resources/graphviz/testVis.png"));
    private final PathResource svg = new PathResource(Paths.get("src/test/resources/graphviz/testVis.svg"));
    private final PathResource dot = new PathResource(Paths.get("src/test/resources/graphviz/testWorkflow.dot"));

    @BeforeEach
    public void setUp() throws Exception {

        Workflow mockWorkflow = Mockito.mock(Workflow.class);
        when(mockWorkflow.getRetrievedFrom())
                .thenReturn(new GitDetails("https://github.com/MarkRobbo/workflows.git",
                        "master", "path/to/workflow.cwl"));

        WorkflowService mockWorkflowService = Mockito.mock(WorkflowService.class);
        when(mockWorkflowService.findByCommitAndPath(any(String.class), any(String.class), any(Optional.class)))
        	.thenReturn(mockWorkflow);

        when(mockWorkflowService.findRawBaseForCommit(any(String.class))).thenReturn(
        		Optional.of("https://raw.githubusercontent.com/MarkRobbo/workflows/commitidhere/"));
        
        when(mockWorkflowService.getWorkflowGraph(eq("svg"), any(GitDetails.class))).thenReturn(svg);
        when(mockWorkflowService.getWorkflowGraph(eq("png"), any(GitDetails.class))).thenReturn(png);
        when(mockWorkflowService.getWorkflowGraph(eq("xdot"), any(GitDetails.class))).thenReturn(dot);
        Path path = Files.createFile(tempDir.resolve("nonsense.zip"));
        when(mockWorkflowService.getROBundle(any())).thenReturn(new File(path.toAbsolutePath().toString()));

        RDFService mockRdfService = Mockito.mock(RDFService.class);
        when(mockRdfService.graphExists(any())).thenReturn(true);

        File turtleFile = new File("src/test/resources/cwl/make_to_cwl/dna.ttl");
        try (FileInputStream fileInputStream = new FileInputStream(turtleFile)) {
            rdfResponse = IOUtils.toByteArray(fileInputStream);
        }
        when(mockRdfService.getModel(any(), any(String.class)))
                .thenReturn(rdfResponse);

        // Mock controller/MVC
        WorkflowPermalinkController underTest = new WorkflowPermalinkController(
                mockWorkflowService,
                mockRdfService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(underTest)
                .build();
    }

    @Test
    public void goToViewer() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "text/html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/workflows/github.com/MarkRobbo/workflows/blob/commitidhere/path/to/workflow.cwl"));
    }

    @Test
    public void goToRawFile() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "application/x-yaml"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://raw.githubusercontent.com/MarkRobbo/workflows/commitidhere/path/to/workflow.cwl"));
    }

    @Test
    public void getRdfTurtle() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "text/turtle"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/turtle"))
                .andExpect(content().bytes(rdfResponse));
    }

    @Test
    public void getRdfJsonld() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "application/ld+json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/ld+json"))
                .andExpect(content().bytes(rdfResponse));
    }

    @Test
    public void getRdfXml() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "application/rdf+xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/rdf+xml"))
                .andExpect(content().bytes(rdfResponse));
    }

    @Test
    public void getPng() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "image/png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(IOUtils.toByteArray(png.getInputStream())));
    }

    @Test
    public void getSvg() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "image/svg+xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().bytes(IOUtils.toByteArray(svg.getInputStream())));
    }

    @Test
    public void getDot() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "text/vnd+graphviz"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/vnd+graphviz"))
                .andExpect(content().bytes(IOUtils.toByteArray(dot.getInputStream())));
    }

    @Test
    public void getRoBundle() throws Exception {
        mockMvc.perform(get("/git/commitidhere/path/to/workflow.cwl")
                .header("accept", "application/vnd.wf4ever.robundle+zip"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.wf4ever.robundle+zip"));
    }
}