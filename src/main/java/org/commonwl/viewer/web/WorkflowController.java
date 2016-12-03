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

import org.commonwl.viewer.domain.GithubDetails;
import org.commonwl.viewer.domain.Workflow;
import org.commonwl.viewer.domain.WorkflowForm;
import org.commonwl.viewer.services.WorkflowFactory;
import org.commonwl.viewer.services.WorkflowFormValidator;
import org.commonwl.viewer.services.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.List;

@Controller
public class WorkflowController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final WorkflowFormValidator workflowFormValidator;
    private final WorkflowFactory workflowFactory;
    private final WorkflowRepository workflowRepository;

    /**
     * Autowired constructor to initialise objects used by the controller
     * @param workflowFormValidator Validator to validate the workflow form
     * @param workflowFactory Builds new Workflow objects
     */
    @Autowired
    public WorkflowController(WorkflowFormValidator workflowFormValidator,
                              WorkflowFactory workflowFactory,
                              WorkflowRepository workflowRepository) {
        this.workflowFormValidator = workflowFormValidator;
        this.workflowFactory = workflowFactory;
        this.workflowRepository = workflowRepository;
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
            // The ID of the workflow to be redirected to
            String workflowID;

            // Check database for existing workflow
            Workflow existingWorkflow = workflowRepository.findByRetrievedFrom(githubInfo);
            if (existingWorkflow != null) {
                logger.info("Fetching existing workflow from DB");

                // Get the ID from the existing workflow
                workflowID = existingWorkflow.getID();
            } else {
                // New workflow from Github URL
                Workflow newWorkflow = workflowFactory.workflowFromGithub(githubInfo);

                // Runtime error
                if (newWorkflow == null) {
                    bindingResult.rejectValue("githubURL", "githubURL.parsingError");
                    return new ModelAndView("index");
                }

                // Save to the MongoDB database
                logger.info("Adding new workflow to DB");
                workflowRepository.save(newWorkflow);

                // Get the ID from the new workflow
                workflowID = newWorkflow.getID();
            }

            // Redirect to the workflow
            return new ModelAndView("redirect:/workflow/" + workflowID);
        }
    }

    /**
     * Display a page for a particular workflow
     * @param workflowID The ID of the workflow to be retrieved
     * @return The workflow view with the workflow as a model
     */
    @RequestMapping(value="/workflow/{workflowID}")
    public ModelAndView getWorkflow(@PathVariable String workflowID){

        // Get workflow from database
        // TODO: Check exists / redirect to error page if not
        Workflow workflowModel = workflowRepository.findOne(workflowID);

        // Display this model along with the view
        return new ModelAndView("workflow", "workflow", workflowModel);

    }
}
