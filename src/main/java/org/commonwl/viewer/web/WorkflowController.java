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

package org.commonwl.viewer.web;

import com.github.jabbalaci.graphviz.GraphViz;
import org.apache.commons.lang.StringUtils;
import org.commonwl.viewer.domain.GithubDetails;
import org.commonwl.viewer.domain.Workflow;
import org.commonwl.viewer.domain.WorkflowForm;
import org.commonwl.viewer.services.WorkflowService;
import org.commonwl.viewer.services.WorkflowFormValidator;
import org.commonwl.viewer.services.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

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
    private final WorkflowRepository workflowRepository;

    /**
     * Autowired constructor to initialise objects used by the controller
     * @param workflowFormValidator Validator to validate the workflow form
     * @param workflowService Builds new Workflow objects
     */
    @Autowired
    public WorkflowController(WorkflowFormValidator workflowFormValidator,
                              WorkflowService workflowService,
                              WorkflowRepository workflowRepository) {
        this.workflowFormValidator = workflowFormValidator;
        this.workflowService = workflowService;
        this.workflowRepository = workflowRepository;
    }

    /**
     * List all the workflows in the database, paginated
     * @param model The model for the page
     * @param pageable Pagination for the list of workflows
     * @return The workflows view
     */
    @RequestMapping(value="/workflows")
    public String listWorkflows(Model model, @PageableDefault(size = 10) Pageable pageable) {
        model.addAttribute("workflows", workflowRepository.findAllByOrderByRetrievedOnDesc(pageable));
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
        logger.info("Retrieving workflow from Github: \"" + workflowForm.getGithubURL() + "\"");

        // Run validator which checks the github URL is valid
        GithubDetails githubInfo = workflowFormValidator.validateAndParse(workflowForm, bindingResult);

        if (bindingResult.hasErrors()) {
            // Go back to index if there are validation errors
            return new ModelAndView("index");
        } else {
            // Check database for existing workflow
            Workflow workflow = workflowRepository.findByRetrievedFrom(githubInfo);

            // Create a new workflow if we do not have one already
            if (workflow == null) {
                // New workflow from Github URL
                workflow = workflowService.newWorkflowFromGithub(githubInfo);

                // Runtime error if workflow could not be generated
                if (workflow == null) {
                    bindingResult.rejectValue("githubURL", "githubURL.parsingError");
                    return new ModelAndView("index");
                }

                // Save to the MongoDB database
                logger.debug("Adding new workflow to DB");
                workflowRepository.save(workflow);
            } else {
                logger.debug("Fetching existing workflow from DB");
            }

            // Redirect to the workflow
            GithubDetails githubDetails = workflow.getRetrievedFrom();
            return new ModelAndView("redirect:/workflows/github.com/" + githubDetails.getOwner()
                    + "/" + githubDetails.getRepoName() + "/tree/" + githubDetails.getBranch()
                    + "/" + githubDetails.getPath());
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
                                                   HttpServletRequest request) {

        // The wildcard end of the URL is the path
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        int pathStartIndex = StringUtils.ordinalIndexOf(path, "/", 7) + 1;
        path = path.substring(pathStartIndex);

        // Construct a GithubDetails object to search for in the database
        GithubDetails githubDetails = new GithubDetails(owner, repoName, branch, path);

        // Get workflow from database
        Workflow workflowModel = workflowRepository.findByRetrievedFrom(githubDetails);

        // Delete the existing workflow if the cache has expired
        if (workflowModel != null) {
            boolean cacheExpired = workflowService.cacheExpired(workflowModel);
            if (cacheExpired) {
                // Update by trying to add a new workflow
                Workflow newWorkflow = workflowService.newWorkflowFromGithub(workflowModel.getRetrievedFrom());

                // Only replace workflow if it could be successfully parsed
                if (newWorkflow == null) {
                    logger.error("Could not parse updated workflow " + workflowModel.id);
                } else {
                    // Delete the existing workflow
                    workflowService.removeWorkflow(workflowModel);

                    // Save new workflow
                    workflowRepository.save(newWorkflow);
                    workflowModel = newWorkflow;
                }
            }
        }

        // Redirect to index with form autofilled if workflow does not already exist
        if (workflowModel == null) {
            return new ModelAndView("redirect:/?url=https://github.com/" +
                    owner + "/" + repoName + "/tree/" + branch + "/" + path);
        }

        // Display this model along with the view
        return new ModelAndView("workflow", "workflow", workflowModel).addObject("appURL", applicationURL);

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

        // Get workflow from database
        Workflow workflowModel = workflowRepository.findOne(workflowID);

        // 404 error if workflow does not exist or the bundle doesn't yet
        if (workflowModel == null || workflowModel.getRoBundle() == null) {
            throw new WorkflowNotFoundException();
        }

        // Set a sensible default file name for the browser
        response.setHeader("Content-Disposition", "attachment; filename=bundle.zip;");

        // Serve the file from the local filesystem
        File bundleDownload = new File(workflowModel.getRoBundle());
        logger.info("Serving download for workflow " + workflowID + " [" + bundleDownload.toString() + "]");
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
    public FileSystemResource getGraphAsSvg(@Value("${graphvizStorage}") String graphvizStorage,
                                            @PathVariable("workflowID") String workflowID) throws IOException {

        // Get workflow from database
        Workflow workflowModel = workflowRepository.findOne(workflowID);

        // 404 error if workflow does not exist
        if (workflowModel == null) {
            throw new WorkflowNotFoundException();
        }

        // Generate graphviz image if it does not already exist
        File out = new File(graphvizStorage + "/" + workflowID + ".svg");
        if (!out.exists()) {
            GraphViz gv = new GraphViz();
            gv.decreaseDpi();
            gv.decreaseDpi();
            gv.decreaseDpi();
            gv.writeGraphToFile(gv.getGraph(workflowModel.getDotGraph()
                    .replace("bgcolor = \"#eeeeee\"", "bgcolor = \"transparent\""), "svg", "dot"), out.getAbsolutePath());
        }

        // Output the graph image
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
    public FileSystemResource getGraphAsPng(@Value("${graphvizStorage}") String graphvizStorage,
                                            @PathVariable("workflowID") String workflowID) throws IOException {

        // Get workflow from database
        Workflow workflowModel = workflowRepository.findOne(workflowID);

        // 404 error if workflow does not exist
        if (workflowModel == null) {
            throw new WorkflowNotFoundException();
        }

        // Generate graphviz image if it does not already exist
        File out = new File(graphvizStorage + "/" + workflowID + ".png");
        if (!out.exists()) {
            GraphViz gv = new GraphViz();
            gv.writeGraphToFile(gv.getGraph(workflowModel.getDotGraph(), "png", "dot"), out.getAbsolutePath());
        }

        // Output the graph image
        return new FileSystemResource(out);
    }
}
