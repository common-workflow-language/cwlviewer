package org.researchobject.web;

import org.researchobject.domain.WorkflowForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    /**
     * Main page of the application
     * @param model The model for the home page where the workflow form is added
     * @return The view for this page
     */
    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("workflowForm", new WorkflowForm());
        return "index";
    }

}
