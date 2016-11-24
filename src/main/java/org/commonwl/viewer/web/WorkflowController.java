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

import org.commonwl.viewer.domain.Workflow;
import org.commonwl.viewer.domain.WorkflowForm;
import org.commonwl.viewer.services.WorkflowFactory;
import org.commonwl.viewer.services.WorkflowFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;

@Controller
public class WorkflowController {

    private final WorkflowFormValidator workflowFormValidator;
    private final WorkflowFactory workflowFactory;

    /**
     * Autowired constructor to initialise objects used by the controller
     * @param workflowFormValidator Validator to validate the workflow form
     * @param workflowFactory Builds new Workflow objects
     */
    @Autowired
    public WorkflowController(WorkflowFormValidator workflowFormValidator, WorkflowFactory workflowFactory) {
        this.workflowFormValidator = workflowFormValidator;
        this.workflowFactory = workflowFactory;
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
        workflowFormValidator.validate(workflowForm, bindingResult);

        if (bindingResult.hasErrors()) {
            // Go back to index if there are validation errors
            return new ModelAndView("index");
        } else {
            // Create a workflow from the github URL
            Workflow newWorkflow = workflowFactory.workflowFromGithub(workflowForm.getGithubURL());

            // Return new workflow along with the workflow view
            return new ModelAndView("workflow", "workflow", newWorkflow);
        }
    }
}
