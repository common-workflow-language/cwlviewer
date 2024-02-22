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

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.commonwl.view.cwl.RDFService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Allows permalinks in URIs across our RDF to identify a workflow or a workflow file
 *
 * <p>Uses content negotiation to return all useful resources related to a workflow Note: May need
 * to edit WebConfig.java to add new content types
 */
@RestController
public class WorkflowPermalinkController {

  private final WorkflowService workflowService;
  private final RDFService rdfService;

  @Autowired
  public WorkflowPermalinkController(WorkflowService workflowService, RDFService rdfService) {
    this.workflowService = workflowService;
    this.rdfService = rdfService;
  }

  /** Generate a URI list of all representations available */
  @GetMapping(
      value = "/git/{commitid}/**",
      produces = {"text/uri-list"})
  public String uriList(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      @RequestParam(name = "format") Optional<String> format,
      HttpServletRequest request,
      HttpServletResponse response) {
    // A bit of a hack - reuse the representation of MultipleWorkflowsException,
    // without setting the Location header or returning 300 Multiple Choices
    Workflow workflow;
    try {
      workflow = getWorkflow(commitId, request, part);
    } catch (MultipleWorkflowsException ex) {
      // No problem, we can use its text/uri-list as-is
      return ex.toString();
    }
    // Return uri-list from a pretend MultipleWorkflowsException
    return new MultipleWorkflowsException(workflow).toString();
  }

  /**
   * Redirect to the viewer for a web browser or API call
   *
   * @param commitId The commit ID of the workflow
   * @return A 302 redirect response to the viewer or 404
   */
  @GetMapping(
      value = "/git/{commitid}/**",
      produces = {
        MediaType.TEXT_HTML_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_JSON_VALUE
      })
  public void goToViewer(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      @RequestParam(name = "format") Optional<String> format,
      HttpServletRequest request,
      HttpServletResponse response) {
    String location;
    Workflow workflow = getWorkflow(commitId, request, part);
    location =
        workflow.getRetrievedFrom().getInternalUrl(commitId)
            + format.map(f -> "?format=" + f).orElse("");
    response.setHeader("Location", location);
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
  }

  /**
   * Redirect to the raw file if this exists
   *
   * @param commitId The commit ID of the workflow
   * @return A 302 redirect response to the raw URL or 406
   */
  @GetMapping(
      value = "/git/{commitid}/**",
      produces = {"application/x-yaml", MediaType.APPLICATION_OCTET_STREAM_VALUE, "*/*"})
  public void goToRawUrl(
      @PathVariable("commitid") String commitId,
      HttpServletRequest request,
      HttpServletResponse response) {
    Optional<String> rawUrl = findRaw(commitId, request);
    if (!rawUrl.isPresent()) {
      throw new RepresentationNotFoundException();
    } else {
      response.setHeader("Location", rawUrl.get());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }
  }

  private Optional<String> findRaw(String commitId, HttpServletRequest request) {
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    final String filepath = WorkflowController.extractPath(path, 3);
    return workflowService.findRawBaseForCommit(commitId).map(base -> base + filepath);
  }

  /**
   * Get the RDF in Turtle format
   *
   * @param commitId The commit ID of the workflow
   * @return The Turtle representation of the RDF for the workflow
   */
  @GetMapping(value = "/git/{commitid}/**", produces = "text/turtle")
  public byte[] getRdfAsTurtle(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      HttpServletRequest request,
      HttpServletResponse response) {
    Workflow workflow = getWorkflow(commitId, request, part);
    String rdfUrl = workflow.getIdentifier();
    if (rdfService.graphExists(rdfUrl)) {
      response.setHeader("Content-Disposition", "inline; filename=\"workflow.ttl\"");
      return rdfService.getModel(rdfUrl, "TURTLE");
    } else {
      throw new WorkflowNotFoundException();
    }
  }

  /**
   * Get the RDF in JsonLD format
   *
   * @param commitId The commit ID of the workflow
   * @return The JsonLD representation of the RDF for the workflow
   */
  @GetMapping(value = "/git/{commitid}/**", produces = "application/ld+json")
  public byte[] getRdfAsJsonLd(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      HttpServletRequest request,
      HttpServletResponse response) {
    Workflow workflow = getWorkflow(commitId, request, part);
    String rdfUrl = workflow.getIdentifier();
    if (rdfService.graphExists(rdfUrl)) {
      response.setHeader("Content-Disposition", "inline; filename=\"workflow.jsonld\"");
      return rdfService.getModel(rdfUrl, "JSON-LD");
    } else {
      throw new WorkflowNotFoundException();
    }
  }

  /**
   * Get the RDF in RDF/XML format
   *
   * @param commitId The commit ID of the workflow
   * @return The RDF/XML representation of the RDF for the workflow
   */
  @GetMapping(value = "/git/{commitid}/**", produces = "application/rdf+xml")
  public byte[] getRdfAsRdfXml(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      HttpServletRequest request,
      HttpServletResponse response) {
    Workflow workflow = getWorkflow(commitId, request, part);
    String rdfUrl = workflow.getIdentifier();
    if (rdfService.graphExists(rdfUrl)) {
      response.setHeader("Content-Disposition", "inline; filename=\"workflow.rdf\"");
      return rdfService.getModel(rdfUrl, "RDFXML");
    } else {
      throw new WorkflowNotFoundException();
    }
  }

  /**
   * Get the generated graph for a workflow in SVG format
   *
   * @param commitId The commit ID of the workflow
   * @return The SVG image
   * @throws IOException
   * @throws WorkflowNotFoundException
   */
  @GetMapping(value = "/git/{commitid}/**", produces = "image/svg+xml")
  public Resource getGraphAsSvg(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      HttpServletRequest request,
      HttpServletResponse response)
      throws WorkflowNotFoundException, IOException {
    Workflow workflow = getWorkflow(commitId, request, part);
    response.setHeader("Content-Disposition", "inline; filename=\"graph.svg\"");
    return workflowService.getWorkflowGraph("svg", workflow.getRetrievedFrom());
  }

  /**
   * Get the generated graph for a workflow in PNG format
   *
   * @param commitId The commit ID of the workflow
   * @return The PNG image
   * @throws IOException
   * @throws WorkflowNotFoundException
   */
  @GetMapping(value = "/git/{commitid}/**", produces = "image/png")
  public Resource getGraphAsPng(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      HttpServletRequest request,
      HttpServletResponse response)
      throws WorkflowNotFoundException, IOException {
    Workflow workflow = getWorkflow(commitId, request, part);
    response.setHeader("Content-Disposition", "inline; filename=\"graph.png\"");
    return workflowService.getWorkflowGraph("png", workflow.getRetrievedFrom());
  }

  /**
   * Get the generated graph for a workflow in XDOT format
   *
   * @param commitId The commit ID of the workflow
   * @return The XDOT source
   * @throws IOException
   * @throws WorkflowNotFoundException
   */
  @GetMapping(value = "/git/{commitid}/**", produces = "text/vnd+graphviz")
  public Resource getGraphAsXDot(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      HttpServletRequest request,
      HttpServletResponse response)
      throws WorkflowNotFoundException, IOException {
    Workflow workflow = getWorkflow(commitId, request, part);
    response.setHeader("Content-Disposition", "inline; filename=\"graph.dot\"");
    return workflowService.getWorkflowGraph("xdot", workflow.getRetrievedFrom());
  }

  /**
   * Get the Research Object bundle for a workflow
   *
   * @param commitId The commit ID of the workflow
   * @return The Research Object Bundle
   */
  @GetMapping(
      value = "/git/{commitid}/**",
      produces = {"application/vnd.wf4ever.robundle+zip", "application/zip"})
  public Resource getROBundle(
      @PathVariable("commitid") String commitId,
      @RequestParam(name = "part") Optional<String> part,
      HttpServletRequest request,
      HttpServletResponse response) {
    Workflow workflow = getWorkflow(commitId, request, part);
    File bundleDownload = workflowService.getROBundle(workflow.getRetrievedFrom());
    response.setHeader("Content-Disposition", "attachment; filename=bundle.zip;");
    return new FileSystemResource(bundleDownload);
  }

  /**
   * Get a workflow based on commit ID and extracting path from request
   *
   * @param commitId The commit ID of the repository
   * @param request The HttpServletRequest from the controller to extract path
   * @param part The workflow part
   * @throws WorkflowNotFoundException If workflow could not be found (404)
   * @throws MultipleWorkflowsException If multiple workflow (parts) were found (300)
   */
  private Workflow getWorkflow(String commitId, HttpServletRequest request, Optional<String> part)
      throws WorkflowNotFoundException {
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    path = WorkflowController.extractPath(path, 3);
    return workflowService.findByCommitAndPath(commitId, path, part);
  }
}
