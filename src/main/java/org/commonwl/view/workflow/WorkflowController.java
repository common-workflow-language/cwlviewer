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

import org.apache.commons.lang.StringUtils;
import org.commonwl.view.cwl.CWLToolStatus;
import org.commonwl.view.cwl.CWLValidationException;
import org.commonwl.view.github.GitDetails;
import org.commonwl.view.github.GitType;
import org.commonwl.view.graphviz.GraphVizService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;

@Controller
public class WorkflowController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final WorkflowFormValidator workflowFormValidator;
    private final WorkflowService workflowService;
    private final GraphVizService graphVizService;

    /**
     * Autowired constructor to initialise objects used by the controller
     * @param workflowFormValidator Validator to validate the workflow form
     * @param workflowService Builds new Workflow objects
     * @param graphVizService Generates and stores imagess
     */
    @Autowired
    public WorkflowController(WorkflowFormValidator workflowFormValidator,
                              WorkflowService workflowService,
                              GraphVizService graphVizService) {
        this.workflowFormValidator = workflowFormValidator;
        this.workflowService = workflowService;
        this.graphVizService = graphVizService;
    }

    /**
     * List all the workflows in the database, paginated
     * @param model The model for the page
     * @param pageable Pagination for the list of workflows
     * @return The workflows view
     */
    @GetMapping(value="/workflows")
    public String listWorkflows(Model model, @PageableDefault(size = 10) Pageable pageable) {
        model.addAttribute("workflows", workflowService.getPageOfWorkflows(pageable));
        model.addAttribute("pages", pageable);
        return "workflows";
    }

    /**
     * Search all the workflows in the database, paginated
     * @param model The model for the page
     * @param pageable Pagination for the list of workflows
     * @return The workflows view
     */
    @GetMapping(value="/workflows", params="search")
    public String searchWorkflows(Model model,
                                  @PageableDefault(size = 10) Pageable pageable,
                                  @RequestParam(value = "search") String search) {
        model.addAttribute("workflows", workflowService.searchPageOfWorkflows(search, pageable));
        model.addAttribute("pages", pageable);
        model.addAttribute("search", search);
        return "workflows";
    }

    /**
     * Create a new workflow from the given URL in the form
     * @param workflowForm The data submitted from the form
     * @param bindingResult Spring MVC Binding Result object
     * @return The workflow view with new workflow as a model
     */
    @PostMapping("/workflows")
    public ModelAndView newWorkflowFromGithubURL(@Valid WorkflowForm workflowForm, BindingResult bindingResult) {

        // Run validator which checks the github URL is valid
        GitDetails gitInfo = workflowFormValidator.validateAndParse(workflowForm, bindingResult);

        if (bindingResult.hasErrors() || gitInfo == null) {
            // Go back to index if there are validation errors
            return new ModelAndView("index");
        } else {
            // Get workflow or create if does not exist
            Workflow workflow = workflowService.getWorkflow(gitInfo);
            if (workflow == null) {
                try {
                    workflow = workflowService.createQueuedWorkflow(gitInfo).getTempRepresentation();
                } catch (CWLValidationException ex) {
                    bindingResult.rejectValue("url", "cwltool.validationError", ex.getMessage());
                    return new ModelAndView("index");
                } catch (Exception ex) {
                    bindingResult.rejectValue("url", "githubURL.parsingError");
                    logger.error(ex.getMessage(), ex);
                    return new ModelAndView("index");
                }
            }
            gitInfo = workflow.getRetrievedFrom();

            // Redirect to the workflow
            return new ModelAndView("redirect:" + gitInfo.getInternalUrl());
        }
    }

    /**
     * Display a page for a particular workflow from Github details
     * @param owner The owner of the Github repository
     * @param repoName The name of the repository
     * @param branch The branch of repository
     * @return The workflow view with the workflow as a model
     */
    @GetMapping(value={"/workflows/github.com/{owner}/{repoName}/tree/{branch}/**",
                       "/workflows/github.com/{owner}/{repoName}/blob/{branch}/**"})
    public ModelAndView getWorkflowByGithubDetails(@Value("${applicationURL}") String applicationURL,
                                                   @PathVariable("owner") String owner,
                                                   @PathVariable("repoName") String repoName,
                                                   @PathVariable("branch") String branch,
                                                   HttpServletRequest request,
                                                   RedirectAttributes redirectAttrs) {

        // The wildcard end of the URL is the path
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = extractPath(path, 7);

        // Construct a GitDetails object to search for in the database
        GitDetails gitDetails = new GitDetails("https://github.com/" + owner + "/" +
                repoName + ".git", branch, path, GitType.GITHUB);

        // Get workflow
        QueuedWorkflow queued = null;
        Workflow workflowModel = workflowService.getWorkflow(gitDetails);
        if (workflowModel == null) {
            // Check if already queued
            queued = workflowService.getQueuedWorkflow(gitDetails);
            if (queued == null) {
                // Validation
                WorkflowForm workflowForm = new WorkflowForm(gitDetails.getUrl());
                BeanPropertyBindingResult errors = new BeanPropertyBindingResult(workflowForm, "errors");
                workflowFormValidator.validateAndParse(workflowForm, errors);
                if (!errors.hasErrors()) {
                    try {
                        queued = workflowService.createQueuedWorkflow(gitDetails);
                    } catch (GitAPIException ex) {
                        errors.rejectValue("url", "git.retrievalError", ex.getMessage());
                    } catch (CWLValidationException ex) {
                        errors.rejectValue("url", "cwltool.validationError", ex.getMessage());
                    } catch (IOException ex) {
                        errors.rejectValue("url", "githubURL.parsingError", "The workflow could not be parsed from the given URL");
                    }
                }
                // Redirect to main page with errors if they occurred
                if (errors.hasErrors()) {
                    redirectAttrs.addFlashAttribute("errors", errors);
                    return new ModelAndView("redirect:/?url=" + gitDetails.getUrl());
                }
            }
        }

        // Display this model along with the view
        ModelAndView modelAndView;
        if (queued != null) {
            // Retry creation if there has been an error in cwltool parsing
            if (queued.getCwltoolStatus() == CWLToolStatus.ERROR) {
                workflowService.retryCwltool(queued);
            }
            modelAndView = new ModelAndView("loading", "queued", queued);
        } else {
            modelAndView = new ModelAndView("workflow", "workflow", workflowModel);
        }

        return modelAndView.addObject("appURL", applicationURL);

    }

    /**
     * Download the Research Object Bundle for a particular workflow
     * @param owner The owner of the Github repository
     * @param repoName The name of the repository
     * @param branch The branch of repository
     */
    @GetMapping(value={"/robundle/github.com/{owner}/{repoName}/tree/{branch}/**",
                       "/robundle/github.com/{owner}/{repoName}/blob/{branch}/**"},
                produces = "application/vnd.wf4ever.robundle+zip")
    @ResponseBody
    public FileSystemResource downloadROBundle(@PathVariable("owner") String owner,
                                             @PathVariable("repoName") String repoName,
                                             @PathVariable("branch") String branch,
                                             HttpServletRequest request,
                                             HttpServletResponse response) throws IOException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = extractPath(path, 7);
        GitDetails gitDetails = new GitDetails("https://github.com/" + owner + "/" +
                repoName + ".git", branch, path, GitType.GITHUB);
        File bundleDownload = workflowService.getROBundle(gitDetails);
        response.setHeader("Content-Disposition", "attachment; filename=bundle.zip;");
        return new FileSystemResource(bundleDownload);
    }


    /**
     * Download a generated graph for a workflow as an svg
     * @param owner The owner of the Github repository
     * @param repoName The name of the repository
     * @param branch The branch of repository
     */
    @GetMapping(value={"/graph/svg/github.com/{owner}/{repoName}/tree/{branch}/**",
                       "/graph/svg/github.com/{owner}/{repoName}/blob/{branch}/**"},
                produces = "image/svg+xml")
    @ResponseBody
    public FileSystemResource getGraphAsSvg(@PathVariable("owner") String owner,
                                            @PathVariable("repoName") String repoName,
                                            @PathVariable("branch") String branch,
                                            HttpServletRequest request,
                                            HttpServletResponse response) throws IOException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = extractPath(path, 8);
        GitDetails gitDetails = new GitDetails("https://github.com/" + owner + "/" +
                repoName + ".git", branch, path, GitType.GITHUB);
        Workflow workflow = workflowService.getWorkflow(gitDetails);
        if (workflow == null) {
            throw new WorkflowNotFoundException();
        }
        File out = graphVizService.getGraph(workflow.getID() + ".svg", workflow.getVisualisationDot(), "svg");
        response.setHeader("Content-Disposition", "inline; filename=\"graph.svg\"");
        return new FileSystemResource(out);
    }

    /**
     * Download a generated DOT graph for a workflow as a png
     * @param owner The owner of the Github repository
     * @param repoName The name of the repository
     * @param branch The branch of repository
     */
    @GetMapping(value={"/graph/png/github.com/{owner}/{repoName}/tree/{branch}/**",
                       "/graph/png/github.com/{owner}/{repoName}/blob/{branch}/**"},
                produces = "image/png")
    @ResponseBody
    public FileSystemResource getGraphAsPng(@PathVariable("owner") String owner,
                                            @PathVariable("repoName") String repoName,
                                            @PathVariable("branch") String branch,
                                            HttpServletRequest request,
                                            HttpServletResponse response) throws IOException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = extractPath(path, 8);
        GitDetails gitDetails = new GitDetails("https://github.com/" + owner + "/" +
                repoName + ".git", branch, path, GitType.GITHUB);
        Workflow workflow = workflowService.getWorkflow(gitDetails);
        if (workflow == null) {
            throw new WorkflowNotFoundException();
        }
        File out = graphVizService.getGraph(workflow.getID() + ".png", workflow.getVisualisationDot(), "png");
        response.setHeader("Content-Disposition", "inline; filename=\"graph.png\"");
        return new FileSystemResource(out);
    }

    /**
     * Download a generated DOT graph for a workflow as xdot
     * @param owner The owner of the Github repository
     * @param repoName The name of the repository
     * @param branch The branch of repository
     */
    @GetMapping(value={"/graph/xdot/github.com/{owner}/{repoName}/tree/{branch}/**",
                       "/graph/xdot/github.com/{owner}/{repoName}/blob/{branch}/**"},
                produces = "text/vnd.graphviz")
    @ResponseBody
    public FileSystemResource getGraphAsXdot(@PathVariable("owner") String owner,
                                             @PathVariable("repoName") String repoName,
                                             @PathVariable("branch") String branch,
                                             HttpServletRequest request,
                                             HttpServletResponse response) throws IOException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = extractPath(path, 8);
        GitDetails gitDetails = new GitDetails("https://github.com/" + owner + "/" +
                repoName + ".git", branch, path, GitType.GITHUB);
        Workflow workflow = workflowService.getWorkflow(gitDetails);
        if (workflow == null) {
            throw new WorkflowNotFoundException();
        }
        File out = graphVizService.getGraph(workflow.getID() + ".dot", workflow.getVisualisationDot(), "xdot");
        response.setHeader("Content-Disposition", "inline; filename=\"graph.dot\"");
        return new FileSystemResource(out);
    }

    /**
     * Get a temporary graph for a pending workflow
     * @param queueID The ID in the queue
     * @return The visualisation image
     */
    @GetMapping(value={"/queue/{queueID}/tempgraph.png"},
            produces = "image/png")
    @ResponseBody
    public FileSystemResource getTempGraphAsPng(@PathVariable("queueID") String queueID,
                                                HttpServletResponse response) throws IOException {
        QueuedWorkflow queued = workflowService.getQueuedWorkflow(queueID);
        if (queued == null) {
            throw new WorkflowNotFoundException();
        }
        File out = graphVizService.getGraph(queued.getId() + ".png",
                queued.getTempRepresentation().getVisualisationDot(), "png");
        response.setHeader("Content-Disposition", "inline; filename=\"graph.png\"");
        return new FileSystemResource(out);
    }


    /**
     * Extract the Github path from the end of a full request string
     * @param path The full request string path
     * @param startSlashNum The ordinal slash index of the start of the path
     * @return THe Github path from the end
     */
    public static String extractPath(String path, int startSlashNum) {
        int pathStartIndex = StringUtils.ordinalIndexOf(path, "/", startSlashNum);
        if (pathStartIndex > -1 && pathStartIndex < path.length() - 1) {
            return path.substring(pathStartIndex + 1).replaceAll("\\/$", "");
        } else {
            return "/";
        }
    }
}
