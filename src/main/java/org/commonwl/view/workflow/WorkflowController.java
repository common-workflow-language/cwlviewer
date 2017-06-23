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
import org.commonwl.view.cwl.CWLValidationException;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.graphviz.GraphVizService;
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
import java.util.List;

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
    @RequestMapping(value="/workflows")
    public String listWorkflows(Model model, @PageableDefault(size = 10) Pageable pageable) {
        model.addAttribute("workflows", workflowService.getPageOfWorkflows(pageable));
        model.addAttribute("pages", pageable);
        return "workflows";
    }

    /**
     * Create a new workflow from the given github URL in the form
     * @param workflowForm The data submitted from the form
     * @param bindingResult Spring MVC Binding Result object
     * @return The workflow view with new workflow as a model
     */
    @PostMapping("/")
    public ModelAndView newWorkflowFromGithubURL(@Valid WorkflowForm workflowForm, BindingResult bindingResult) {

        // Run validator which checks the github URL is valid
        GithubDetails githubInfo = workflowFormValidator.validateAndParse(workflowForm, bindingResult);

        if (bindingResult.hasErrors() || githubInfo == null) {
            // Go back to index if there are validation errors
            return new ModelAndView("index");
        } else {
            if (githubInfo.getPath().endsWith(".cwl")) {
                // Get workflow or create if does not exist
                Workflow workflow = workflowService.getWorkflow(githubInfo);
                if (workflow == null) {
                    try {
                        workflow = workflowService.createWorkflow(githubInfo);
                    } catch (CWLValidationException ex) {
                        bindingResult.rejectValue("githubURL", "cwltool.validationError", ex.getMessage());
                        return new ModelAndView("index");
                    } catch (Exception ex) {
                        bindingResult.rejectValue("githubURL", "githubURL.parsingError");
                        logger.error(ex.getMessage(), ex);
                        return new ModelAndView("index");
                    }
                }
                githubInfo = workflow.getRetrievedFrom();
            }
            // Redirect to the workflow or choice of files
            return new ModelAndView("redirect:/workflows/github.com/" + githubInfo.getOwner()
                    + "/" + githubInfo.getRepoName() + "/tree/" + githubInfo.getBranch()
                    + "/" + githubInfo.getPath());
        }
    }

    /**
     * Display a page for a particular workflow from Github details
     * @param owner The owner of the Github repository
     * @param repoName The name of the repository
     * @param branch The branch of repository
     * @return The workflow view with the workflow as a model
     */
    @RequestMapping(value="/workflows/github.com/{owner}/{repoName}/tree/{branch}/**")
    public ModelAndView getWorkflowByGithubDetails(@Value("${applicationURL}") String applicationURL,
                                                   @PathVariable("owner") String owner,
                                                   @PathVariable("repoName") String repoName,
                                                   @PathVariable("branch") String branch,
                                                   HttpServletRequest request,
                                                   RedirectAttributes redirectAttrs) {

        // The wildcard end of the URL is the path
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        int pathStartIndex = StringUtils.ordinalIndexOf(path, "/", 7);
        if (pathStartIndex > -1 && pathStartIndex < path.length() - 1) {
            path = path.substring(pathStartIndex + 1).replaceAll("\\/$", "");
        } else {
            path = "/";
        }

        // Construct a GithubDetails object to search for in the database
        GithubDetails githubDetails = new GithubDetails(owner, repoName, branch, path);

        // Get workflow
        Workflow workflowModel = workflowService.getWorkflow(githubDetails);
        if (workflowModel == null) {
            // Validation
            WorkflowForm workflowForm = new WorkflowForm(githubDetails.getURL());
            BeanPropertyBindingResult errors = new BeanPropertyBindingResult(workflowForm, "errors");
            workflowFormValidator.validateAndParse(workflowForm, errors);
            if (!errors.hasErrors()) {
                if (githubDetails.getPath().endsWith(".cwl")) {
                    try {
                        workflowModel = workflowService.createWorkflow(githubDetails);
                    } catch (CWLValidationException ex) {
                        errors.rejectValue("githubURL", "cwltool.validationError", ex.getMessage());
                    } catch (IOException ex) {
                        errors.rejectValue("githubURL", "githubURL.parsingError", "The workflow could not be parsed from the given URL");
                    }
                } else {
                    // If this is a directory, get a list of workflows and return the view for it
                    try {
                        List<WorkflowOverview> workflowOverviews = workflowService.getWorkflowsFromDirectory(githubDetails);
                        if (workflowOverviews.size() > 1) {
                            return new ModelAndView("selectworkflow", "workflowOverviews", workflowOverviews)
                                    .addObject("githubDetails", githubDetails);
                        } else if (workflowOverviews.size() == 1) {
                            return new ModelAndView("redirect:/workflows/github.com/" + githubDetails.getOwner()
                                    + "/" + githubDetails.getRepoName() + "/tree/" + githubDetails.getBranch()
                                    + "/" + githubDetails.getPath() + "/" + workflowOverviews.get(0).getFileName());
                        } else {
                            logger.error("No .cwl files were found in the given directory");
                            errors.rejectValue("githubURL", "githubURL.invalid", "You must enter a valid Github URL to a .cwl file");
                        }
                    } catch (IOException ex) {
                        logger.error("Contents of Github directory could not be found", ex);
                        errors.rejectValue("githubURL", "githubURL.invalid", "You must enter a valid Github URL to a .cwl file");
                    }
                }
            }

            // Redirect to main page with errors if they occurred
            if (errors.hasErrors()) {
                redirectAttrs.addFlashAttribute("errors", errors);
                return new ModelAndView("redirect:/?url=https://github.com/" +
                        owner + "/" + repoName + "/tree/" + branch + "/" + path);
            }
        }

        // Retry creation if there is an error in cwltool parsing
        if (workflowModel.getCwltoolStatus() == Workflow.Status.ERROR) {
            workflowService.updateWorkflow(workflowModel, githubDetails);
        }

        // Display this model along with the view
        String model;
        if (workflowModel.getCwltoolStatus() == Workflow.Status.RUNNING) {
            model = "loading";
        } else {
            model = "workflow";
        }
        return new ModelAndView(model, "workflow", workflowModel).addObject("appURL", applicationURL);

    }

    /**
     * Checks whether cwltool has finished running on a workflow
     * @param workflowID The workflow ID to check
     * @return Either "RUNNING", "SUCCESS" or an error message
     */
    @RequestMapping(value = "/workflows/{workflowID}/cwlstatus",
            method = RequestMethod.GET)
    @ResponseBody
    public String checkCwlStatus(@PathVariable("workflowID") String workflowID) {
        Workflow workflowModel = workflowService.getWorkflow(workflowID);
        if (workflowModel == null) {
            throw new WorkflowNotFoundException();
        }
        switch (workflowModel.getCwltoolStatus()) {
            case RUNNING:
                return "RUNNING";
            case SUCCESS:
                return "SUCCESS";
            case ERROR:
                return workflowModel.getCwltoolLog();
        }
        return "";
    }

    /**
     * Download the Research Object Bundle for a particular workflow
     * @param workflowID The ID of the workflow to download
     */
    @RequestMapping(value = "/workflows/{workflowID}/download",
                    method = RequestMethod.GET,
                    produces = "application/vnd.wf4ever.robundle+zip")
    @ResponseBody
    public FileSystemResource downloadROBundle(@PathVariable("workflowID") String workflowID,
                                               HttpServletResponse response) {
        File bundleDownload = workflowService.getROBundle(workflowID);
        response.setHeader("Content-Disposition", "attachment; filename=bundle.zip;");
        return new FileSystemResource(bundleDownload);
    }

    /**
     * Download a generated DOT graph for a workflow as an svg
     * @param workflowID The ID of the workflow to download the graph for
     */
    @RequestMapping(value = "/workflows/{workflowID}/graph/svg",
                    method = RequestMethod.GET,
                    produces = "image/svg+xml")
    @ResponseBody
    public FileSystemResource getGraphAsSvg(@PathVariable("workflowID") String workflowID) throws IOException {
        Workflow workflowModel = workflowService.getWorkflow(workflowID);
        if (workflowModel == null) {
            throw new WorkflowNotFoundException();
        }
        File out = graphVizService.getGraph(workflowID + ".svg", workflowModel.getDotGraph(), "svg");
        return new FileSystemResource(out);
    }

    /**
     * Download a generated DOT graph for a workflow as a png
     * @param workflowID The ID of the workflow to download the graph for
     */
    @RequestMapping(value = "/workflows/{workflowID}/graph/png",
            method = RequestMethod.GET,
            produces = "image/png")
    @ResponseBody
    public FileSystemResource getGraphAsPng(@PathVariable("workflowID") String workflowID) throws IOException {
        Workflow workflowModel = workflowService.getWorkflow(workflowID);
        if (workflowModel == null) {
            throw new WorkflowNotFoundException();
        }
        File out = graphVizService.getGraph(workflowID + ".png", workflowModel.getDotGraph(), "png");
        return new FileSystemResource(out);
    }

    /**
     * Download a generated DOT graph for a workflow as xdot
     * @param workflowID The ID of the workflow to download the graph for
     */
    @RequestMapping(value = "/workflows/{workflowID}/graph/xdot",
            method = RequestMethod.GET,
            produces = "text/vnd.graphviz")
    @ResponseBody
    public FileSystemResource getGraphAsXdot(@PathVariable("workflowID") String workflowID) throws IOException {
        Workflow workflowModel = workflowService.getWorkflow(workflowID);
        if (workflowModel == null) {
            throw new WorkflowNotFoundException();
        }
        File out = graphVizService.getGraph(workflowID + ".dot", workflowModel.getDotGraph(), "xdot");
        return new FileSystemResource(out);
    }
}
