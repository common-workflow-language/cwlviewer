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

import static org.commonwl.view.cwl.CWLToolStatus.SUCCESS;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.commonwl.view.cwl.CWLValidationException;
import org.commonwl.view.git.GitDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

/** JSON API Controller */
@RestController
public class WorkflowJSONController {

  private final WorkflowFormValidator workflowFormValidator;
  private final WorkflowService workflowService;

  /**
   * Autowired constructor to initialise objects used by the controller
   *
   * @param workflowFormValidator Validator to validate the workflow form
   * @param workflowService Builds new Workflow objects
   */
  @Autowired
  public WorkflowJSONController(
      WorkflowFormValidator workflowFormValidator, WorkflowService workflowService) {
    this.workflowFormValidator = workflowFormValidator;
    this.workflowService = workflowService;
  }

  /**
   * List all the workflows in the database, paginated
   *
   * @return A list of all the workflows
   */
  @GetMapping(value = "/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<Workflow> listWorkflowsJson(
      Model model, @PageableDefault(size = 10) Pageable pageable) {
    return workflowService.getPageOfWorkflows(pageable);
  }

  /**
   * Search all workflows for a string in the label or doc
   *
   * @return A list of all the workflows
   */
  @GetMapping(value = "/workflows", params = "search", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<Workflow> searchWorkflowsJson(
      Model model,
      @PageableDefault(size = 10) Pageable pageable,
      @RequestParam(value = "search") String search) {
    return workflowService.searchPageOfWorkflows(search, pageable);
  }

  /**
   * Create a new workflow from the given URL
   *
   * @param url The URL of the workflow
   * @param branch The branch where the workflow can be found
   * @param path The path within the repository to the workflow file
   * @param packedId The ID of the workflow if the file is packed
   * @return Appropriate response code and optional JSON string with message
   */
  @PostMapping(value = "/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> newWorkflowFromGitURLJson(
      @RequestParam(value = "url") String url,
      @RequestParam(value = "branch", required = false) String branch,
      @RequestParam(value = "path", required = false) String path,
      @RequestParam(value = "packedId", required = false) String packedId,
      HttpServletResponse response) {

    // Run validator which checks the URL is valid
    WorkflowForm workflowForm = new WorkflowForm(url, branch, path, packedId);
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(workflowForm, "errors");
    GitDetails gitInfo = workflowFormValidator.validateAndParse(workflowForm, errors);

    if (errors.hasErrors() || gitInfo == null) {
      String error;
      if (errors.hasErrors()) {
        error = errors.getAllErrors().get(0).getDefaultMessage();
      } else {
        error = "Could not parse workflow details from URL";
      }
      Map<String, String> message = Collections.singletonMap("message", "Error: " + error);
      return new ResponseEntity<Map<String, String>>(message, HttpStatus.BAD_REQUEST);
    } else {
      // Get workflow or create if does not exist
      Workflow workflow = workflowService.getWorkflow(gitInfo);
      if (workflow == null) {
        // Check if already queued
        QueuedWorkflow queued = workflowService.getQueuedWorkflow(gitInfo);
        if (queued == null) {
          try {
            queued = workflowService.createQueuedWorkflow(gitInfo);
            if (queued.getWorkflowList() != null) {
              if (queued.getWorkflowList().size() == 1) {
                // Parse the packed workflow within automatically if there is only one
                gitInfo.setPackedId(queued.getWorkflowList().get(0).getFileName());
                queued = workflowService.getQueuedWorkflow(gitInfo);
                if (queued == null) {
                  queued = workflowService.createQueuedWorkflow(gitInfo);
                }
              } else {
                // Error with alternatives suggested
                List<String> workflowUris = new ArrayList<>();
                for (WorkflowOverview overview : queued.getWorkflowList()) {
                  workflowUris.add(overview.getFileName().substring(1));
                }
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put(
                    "message",
                    "This workflow file is packed and contains multiple workflow "
                        + "descriptions. Please provide a packedId parameter with one of the following");
                responseMap.put("packedId", workflowUris);
                return new ResponseEntity<Map<String, Object>>(
                    responseMap, HttpStatus.UNPROCESSABLE_ENTITY);
              }
            }
          } catch (CWLValidationException ex) {
            Map<String, String> message =
                Collections.singletonMap("message", "Error:" + ex.getMessage());
            return new ResponseEntity<Map<String, String>>(message, HttpStatus.BAD_REQUEST);
          } catch (Exception ex) {
            Map<String, String> message =
                Collections.singletonMap(
                    "message", "Error: Workflow could not be created from the provided cwl file");
            return new ResponseEntity<Map<String, String>>(message, HttpStatus.BAD_REQUEST);
          }
        }
        response.setHeader("Location", "/queue/" + queued.getId());
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        return null;
      } else {
        // Workflow already exists and is equivalent
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", gitInfo.getInternalUrl());
        return null;
      }
    }
  }

  /**
   * Get the JSON representation of a workflow from github.com or gitlab.com details
   *
   * @param domain The domain of the hosting site, github.com or gitlab.com
   * @param owner The owner of the Github repository
   * @param repoName The name of the repository
   * @param branch The branch of repository
   * @return The JSON representation of the workflow
   */
  @GetMapping(
      value = {
        "/workflows/{domain}.com/{owner}/{repoName}/tree/{branch}/**",
        "/workflows/{domain}.com/{owner}/{repoName}/blob/{branch}/**"
      },
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Workflow getWorkflowJson(
      @PathVariable("domain") String domain,
      @PathVariable("owner") String owner,
      @PathVariable("repoName") String repoName,
      @PathVariable("branch") String branch,
      HttpServletRequest request) {
    // The wildcard end of the URL is the path
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    path = WorkflowController.extractPath(path, 7);

    // Construct a GitDetails object to search for in the database
    GitDetails gitDetails = WorkflowController.getGitDetails(domain, owner, repoName, branch, path);

    // Get workflow
    Workflow workflowModel = workflowService.getWorkflow(gitDetails);
    if (workflowModel == null) {
      throw new WorkflowNotFoundException();
    } else {
      return workflowModel;
    }
  }

  /**
   * Get JSON representation of a workflow from generic git details
   *
   * @param branch The branch of the repository
   * @return The JSON representation of the workflow
   */
  @GetMapping(
      value = "/workflows/**/*.git/{branch}/**",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Workflow getWorkflowJsonGeneric(
      @PathVariable("branch") String branch, HttpServletRequest request) {
    // The wildcard end of the URL is the path
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

    // Construct a GitDetails object to search for in the database
    GitDetails gitDetails = WorkflowController.getGitDetails(11, path, branch);

    // Get workflow
    Workflow workflowModel = workflowService.getWorkflow(gitDetails);
    if (workflowModel == null) {
      throw new WorkflowNotFoundException();
    } else {
      return workflowModel;
    }
  }

  /**
   * Query progress of a queued workflow
   *
   * @param queueID The queued workflow ID to check
   * @return 303 see other status w/ location header if success, otherwise JSON representation of
   *     object
   */
  @GetMapping(value = "/queue/{queueID}", produces = MediaType.APPLICATION_JSON_VALUE)
  public QueuedWorkflow checkQueueJson(
      @PathVariable("queueID") String queueID, HttpServletResponse response) {
    QueuedWorkflow queuedWorkflow = workflowService.getQueuedWorkflow(queueID);
    if (queuedWorkflow == null) {
      throw new WorkflowNotFoundException();
    }

    if (queuedWorkflow.getCwltoolStatus() == SUCCESS) {
      GitDetails gitInfo = queuedWorkflow.getTempRepresentation().getRetrievedFrom();
      String resourceLocation = gitInfo.getInternalUrl();
      response.setHeader("Location", resourceLocation);
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
    }
    return queuedWorkflow;
  }
}
