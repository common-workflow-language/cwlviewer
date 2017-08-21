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

import org.commonwl.view.git.GitType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Allows permalinks in URIs across our RDF to identify a
 * workflow or a workflow file
 *
 * Uses content negotiation to return all useful resources
 * related to a workflow
 */
@RestController
public class WorkflowPermalinkController {

    private final WorkflowRepository workflowRepository;

    @Autowired
    public WorkflowPermalinkController(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    /**
     * Redirect to the viewer for a web browser or API call
     * @param commitId The commit ID of the workflow
     * @return A 302 redirect response to the viewer or 404
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = {MediaType.TEXT_HTML_VALUE,
                            MediaType.APPLICATION_JSON_VALUE,
                            MediaType.APPLICATION_JSON_UTF8_VALUE})
    public void goToViewer(@PathVariable("commitid") String commitId,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = WorkflowController.extractPath(path, 3);
        Workflow workflow = findByCommitAndPath(commitId, path);
        response.setHeader("Location", workflow.getRetrievedFrom().getInternalUrl(commitId));
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    /**
     * Redirect to the raw file if this exists
     * @param commitId The commit ID of the workflow
     * @return A 302 redirect response to the raw URL or 404
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = {"application/x-yaml", MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public void goToRawUrl(@PathVariable("commitid") String commitId,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = WorkflowController.extractPath(path, 3);
        Workflow workflow = findByCommitAndPath(commitId, path);
        if (workflow.getRetrievedFrom().getType() == GitType.GENERIC) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setHeader("Location", workflow.getRetrievedFrom().getRawUrl(commitId));
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        }
    }

    /**
     * Get the RDF in Turtle format
     * @param commitId The commit ID of the workflow
     * @return The Turtle representation of the RDF for the workflow
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "text/turtle")
    public Workflow getRdfAsTurtle(@PathVariable("commitid") String commitId,
                                   HttpServletRequest request) {
        return null;
    }

    /**
     * Get the RDF in JsonLD format
     * @param commitId The commit ID of the workflow
     * @return The JsonLD representation of the RDF for the workflow
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "application/ld+json")
    public Workflow getRdfAsJsonLd(@PathVariable("commitid") String commitId,
                                   HttpServletRequest request) {
        return null;
    }

    /**
     * Get the RDF in RDF/XML format
     * @param commitId The commit ID of the workflow
     * @return The RDF/XML representation of the RDF for the workflow
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "application/rdf+xml")
    public Workflow getRdfAsRdfXml(@PathVariable("commitid") String commitId,
                                   HttpServletRequest request) {
        return null;
    }

    /**
     * Get the generated graph for a workflow in SVG format
     * @param commitId The commit ID of the workflow
     * @return The SVG image
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "image/svg+xml")
    public Workflow getGraphAsSvg(@PathVariable("commitid") String commitId,
                                  HttpServletRequest request) {
        return null;
    }

    /**
     * Get the generated graph for a workflow in PNG format
     * @param commitId The commit ID of the workflow
     * @return The PNG image
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "image/png")
    public Workflow getGraphAsPng(@PathVariable("commitid") String commitId,
                                  HttpServletRequest request) {
        return null;
    }

    /**
     * Get the generated graph for a workflow in XDOT format
     * @param commitId The commit ID of the workflow
     * @return The XDOT source
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "text/vnd+graphviz")
    public Workflow getGraphAsDot(@PathVariable("commitid") String commitId,
                                  HttpServletRequest request) {
        return null;
    }


    /**
     * Get the Research Object bundle for a workflow
     * @param commitId The commit ID of the workflow
     * @return The Research Object Bundle
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = {"application/ro+zip", "application/vnd.wf4ever.robundle+zip"})
    public Workflow getROBundle(@PathVariable("commitid") String commitId,
                                HttpServletRequest request) {
        return null;
    }

    /**
     * Find a workflow by commit ID and path
     * @param commitID The commit ID of the workflow
     * @param path The path to the workflow within the repository
     * @return A workflow model with the above two parameters
     */
    private Workflow findByCommitAndPath(String commitID, String path) throws WorkflowNotFoundException {
        List<Workflow> matches = workflowRepository.findByCommitAndPath(commitID, path);
        if (matches == null || matches.size() == 0) {
            throw new WorkflowNotFoundException();
        } else if (matches.size() == 1) {
            return matches.get(0);
        } else {
            // Multiple matches means either added by both branch and ID
            // Or packed workflow
            for (Workflow workflow : matches) {
                if (workflow.getPackedWorkflowID() != null) {
                    // This is a packed file
                    // TODO: return 300 multiple choices response for this in controller
                    throw new WorkflowNotFoundException();
                }
            }
            // Not a packed workflow, just different references to the same ID
            return matches.get(0);
        }
    }

}
