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

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

/**
 * Allows permalinks in URIs across our RDF to identify a
 * workflow or a workflow file
 *
 * Uses content negotiation to return all useful resources
 * related to a workflow
 */
public class WorkflowPermalinkController {

    /**
     * Redirect to the viewer for a web browser or API call
     * @param commitId The commit ID of the workflow
     * @return A 302 redirect response to the viewer or 404
     */
    @GetMapping(value = "/git/{commitid}/**",
            produces = {MediaType.TEXT_HTML_VALUE,
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_JSON_UTF8_VALUE})
    public Workflow goToViewer(@PathVariable("commitid") String commitId,
                               HttpServletRequest request) {
        return null;
    }

    /**
     * Redirect to the raw file if this exists
     * @param commitId The commit ID of the workflow
     * @return A 302 redirect response to the raw URL or 404
     */
    @GetMapping(value = "/git/{commitid}/**",
            produces = {"application/x-yaml", MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public Workflow goToRawUrl(@PathVariable("commitid") String commitId,
                               HttpServletRequest request) {
        return null;
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


}
