package org.researchobject.web;

import org.researchobject.domain.Workflow;
import org.researchobject.domain.WorkflowForm;
import org.researchobject.services.WorkflowFactory;
import org.researchobject.services.WorkflowFormValidator;
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
