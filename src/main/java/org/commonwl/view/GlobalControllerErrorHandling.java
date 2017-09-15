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

package org.commonwl.view;

import java.util.Collections;

import org.commonwl.view.workflow.MultipleWorkflowsException;
import org.commonwl.view.workflow.RepresentationNotFoundException;
import org.commonwl.view.workflow.WorkflowNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Handles exception handling across the application.
 * <p>
 * Because of Spring Boot's content negotiation these handlers are needed when
 * the error is returned with an otherwise "non acceptable" content type (e.g.
 * Accept: image/svg+xml but we have to say 404 Not Found)
 *
 */
@ControllerAdvice
public class GlobalControllerErrorHandling {

    /**
     * Workflow can not be found
     * @return A plain text error message
     */
    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<?> handleNotFound() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>("Workflow or git commit could not be found", headers, HttpStatus.NOT_FOUND);
    }

    /**
     * More than one workflow (or workflow parts) found
     *
     * @return A text/uri-list of potential representations
     */
    @ExceptionHandler(MultipleWorkflowsException.class)
    public ResponseEntity<?> handleMultipleWorkflows(MultipleWorkflowsException ex) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/uri-list"));
        return new ResponseEntity<>(ex.toString(), headers, HttpStatus.MULTIPLE_CHOICES);
    }

    /**
     * Workflow exists but representation is not found eg Generic git workflow
     * asking for raw workflow URL
     *
     * @return A plain text error message
     */
    @ExceptionHandler(RepresentationNotFoundException.class)
    public ResponseEntity<?> handleNoRepresentation() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setVary(Collections.singletonList("Accept"));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>("The workflow exists but the requested representation could not be found",
                headers, HttpStatus.NOT_ACCEPTABLE);
    }

}
