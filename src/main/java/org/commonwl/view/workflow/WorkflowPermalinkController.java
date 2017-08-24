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

import org.commonwl.view.cwl.RDFService;
import org.commonwl.view.git.GitType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows permalinks in URIs across our RDF to identify a
 * workflow or a workflow file
 *
 * Uses content negotiation to return all useful resources
 * related to a workflow
 * Note: May need to edit WebConfig.java to add new content types
 */
@RestController
public class WorkflowPermalinkController {

    private final WorkflowService workflowService;
    private final RDFService rdfService;

    @Autowired
    public WorkflowPermalinkController(WorkflowService workflowService,
                                       RDFService rdfService) {
        this.workflowService = workflowService;
        this.rdfService = rdfService;
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
    public ResponseEntity<?> goToViewer(@PathVariable("commitid") String commitId,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            response.setHeader("Location", workflows.get(0).getRetrievedFrom().getInternalUrl(commitId));
            return new ResponseEntity(HttpStatus.TEMPORARY_REDIRECT);
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }

    /**
     * Redirect to the raw file if this exists
     * @param commitId The commit ID of the workflow
     * @return A 302 redirect response to the raw URL or 404
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = {"application/x-yaml", MediaType.APPLICATION_OCTET_STREAM_VALUE, "*/*"})
    public ResponseEntity<?> goToRawUrl(@PathVariable("commitid") String commitId,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            if (workflows.get(0).getRetrievedFrom().getType() == GitType.GENERIC) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            } else {
                response.setHeader("Location", workflows.get(0).getRetrievedFrom().getRawUrl(commitId));
                return new ResponseEntity(HttpStatus.TEMPORARY_REDIRECT);
            }
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }

    /**
     * Get the RDF in Turtle format
     * @param commitId The commit ID of the workflow
     * @return The Turtle representation of the RDF for the workflow
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "text/turtle")
    public ResponseEntity<?> getRdfAsTurtle(@PathVariable("commitid") String commitId,
                                 HttpServletRequest request) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            String rdfUrl = workflows.get(0).getRetrievedFrom().getUrl(commitId).replace("https://", "");
            if (rdfService.graphExists(rdfUrl)) {
                return new ResponseEntity<>(rdfService.getModel(rdfUrl, "TURTLE"), HttpStatus.OK);
            } else {
                throw new WorkflowNotFoundException();
            }
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }

    /**
     * Get the RDF in JsonLD format
     * @param commitId The commit ID of the workflow
     * @return The JsonLD representation of the RDF for the workflow
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "application/ld+json")
    public ResponseEntity<?> getRdfAsJsonLd(@PathVariable("commitid") String commitId,
                                 HttpServletRequest request) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            String rdfUrl = workflows.get(0).getRetrievedFrom().getUrl(commitId).replace("https://", "");
            if (rdfService.graphExists(rdfUrl)) {
                return new ResponseEntity<>(rdfService.getModel(rdfUrl, "JSON-LD"), HttpStatus.OK);
            } else {
                throw new WorkflowNotFoundException();
            }
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }

    /**
     * Get the RDF in RDF/XML format
     * @param commitId The commit ID of the workflow
     * @return The RDF/XML representation of the RDF for the workflow
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "application/rdf+xml")
    public ResponseEntity<?> getRdfAsRdfXml(@PathVariable("commitid") String commitId,
                                 HttpServletRequest request) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            String rdfUrl = workflows.get(0).getRetrievedFrom().getUrl(commitId).replace("https://", "");
            if (rdfService.graphExists(rdfUrl)) {
                return new ResponseEntity<>(rdfService.getModel(rdfUrl, "RDFXML"), HttpStatus.OK);
            } else {
                throw new WorkflowNotFoundException();
            }
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }

    /**
     * Get the generated graph for a workflow in SVG format
     * @param commitId The commit ID of the workflow
     * @return The SVG image
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "image/svg+xml")
    public ResponseEntity<?> getGraphAsSvg(@PathVariable("commitid") String commitId,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            response.setHeader("Content-Disposition", "inline; filename=\"graph.svg\"");
            FileSystemResource image = workflowService.getWorkflowGraph("svg", workflows.get(0).getRetrievedFrom());
            return new ResponseEntity<>(image, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }

    /**
     * Get the generated graph for a workflow in PNG format
     * @param commitId The commit ID of the workflow
     * @return The PNG image
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "image/png")
    public ResponseEntity<?> getGraphAsPng(@PathVariable("commitid") String commitId,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            response.setHeader("Content-Disposition", "inline; filename=\"graph.png\"");
            FileSystemResource image = workflowService.getWorkflowGraph("png", workflows.get(0).getRetrievedFrom());
            return new ResponseEntity<>(image, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }

    /**
     * Get the generated graph for a workflow in XDOT format
     * @param commitId The commit ID of the workflow
     * @return The XDOT source
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = "text/vnd+graphviz")
    public ResponseEntity<?> getGraphAsXDot(@PathVariable("commitid") String commitId,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            response.setHeader("Content-Disposition", "inline; filename=\"graph.dot\"");
            FileSystemResource image = workflowService.getWorkflowGraph("xdot", workflows.get(0).getRetrievedFrom());
            return new ResponseEntity<>(image, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }


    /**
     * Get the Research Object bundle for a workflow
     * @param commitId The commit ID of the workflow
     * @return The Research Object Bundle
     */
    @GetMapping(value = "/git/{commitid}/**",
                produces = {"application/vnd.wf4ever.robundle+zip", "application/zip"})
    public ResponseEntity<?> getROBundle(@PathVariable("commitid") String commitId,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        List<Workflow> workflows = getWorkflows(commitId, request);
        if (workflows.size() == 1) {
            File bundleDownload = workflowService.getROBundle(workflows.get(0).getRetrievedFrom());
            response.setHeader("Content-Disposition", "attachment; filename=bundle.zip;");
            return new ResponseEntity<>(new FileSystemResource(bundleDownload), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(getPermalinks(workflows), HttpStatus.MULTIPLE_CHOICES);
        }
    }

    /**
     * Get workflows based on commit ID and extracting path from request
     * @param commitId The commit ID of the repository
     * @param request The HttpServletRequest from the controller to extract path
     * @throws WorkflowNotFoundException If workflow could not be found (404)
     */
    private List<Workflow> getWorkflows(String commitId,
                                        HttpServletRequest request) throws WorkflowNotFoundException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = WorkflowController.extractPath(path, 3);
        return workflowService.findByCommitAndPath(commitId, path);
    }

    /**
     * Get a list of permalinks from a list of workflows
     * @param workflows The list of workflows
     * @return The list of permalinks for the given workflows
     */
    private List<String> getPermalinks(List<Workflow> workflows) {
        return workflows.stream().map(Workflow::getPermalink).collect(Collectors.toList());
    }

}
