package org.commonwl.view.workflow;

import org.commonwl.view.cwl.CWLValidationException;
import org.commonwl.view.github.GithubDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

/**
 * RESTful API controller
 */
@RestController
public class WorkflowRESTController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final WorkflowFormValidator workflowFormValidator;
    private final WorkflowService workflowService;

    /**
     * Autowired constructor to initialise objects used by the controller
     * @param workflowFormValidator Validator to validate the workflow form
     * @param workflowService Builds new Workflow objects
     */
    @Autowired
    public WorkflowRESTController(WorkflowFormValidator workflowFormValidator,
                                  WorkflowService workflowService) {
        this.workflowFormValidator = workflowFormValidator;
        this.workflowService = workflowService;
    }

    /**
     * List all the workflows in the database, paginated
     * @return A list of all the workflows
     */
    @GetMapping(value="/workflows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Page<Workflow> listWorkflowsJson(Model model, @PageableDefault(size = 10) Pageable pageable) {
        return workflowService.getPageOfWorkflows(pageable);
    }

    /**
     * Create a new workflow from the given github URL
     * @param url The URL of the workflow
     * @return Appropriate response code and optional JSON string with message
     */
    @PostMapping(value = "/workflows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> newWorkflowFromGithubURLJson(@RequestParam(value="url") String url,
                                                          HttpServletResponse response) {

        // Run validator which checks the github URL is valid
        WorkflowForm workflowForm = new WorkflowForm(url);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(workflowForm, "errors");
        GithubDetails githubInfo = workflowFormValidator.validateAndParse(workflowForm, errors);

        if (errors.hasErrors() || githubInfo == null) {
            Map<String, String> message = Collections.singletonMap("message", "Error: " +
                    errors.getAllErrors().get(0).getDefaultMessage());
            return new ResponseEntity<Map>(message, HttpStatus.BAD_REQUEST);
        } else {
            if (githubInfo.getPath().endsWith(".cwl")) {
                // Get workflow or create if does not exist
                Workflow workflow = workflowService.getWorkflow(githubInfo);
                String resourceLocation = "/workflows/github.com/" + githubInfo.getOwner()
                        + "/" + githubInfo.getRepoName() + "/tree/" + githubInfo.getBranch()
                        + "/" + githubInfo.getPath();
                if (workflow == null) {
                    try {
                        workflow = workflowService.createWorkflow(githubInfo);
                        response.setHeader("Location", resourceLocation);
                        return new ResponseEntity<>(workflow, HttpStatus.ACCEPTED);
                    } catch (CWLValidationException ex) {
                        Map<String, String> message = Collections.singletonMap("message", "Error:" + ex.getMessage());
                        return new ResponseEntity<Map>(message, HttpStatus.BAD_REQUEST);
                    } catch (Exception ex) {
                        Map<String, String> message = Collections.singletonMap("message",
                                "Error: Workflow could not be created from the provided cwl file");
                        return new ResponseEntity<Map>(message, HttpStatus.BAD_REQUEST);
                    }
                } else {
                    // Workflow already exists and is equivalent
                    response.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    response.setHeader("Location", resourceLocation);
                    return null;
                }
            } else {
                Map<String, String> message = Collections.singletonMap("message",
                        "Error: URL provided was not a .cwl file");
                return new ResponseEntity<Map>(message, HttpStatus.BAD_REQUEST);
            }
        }
    }

    /**
     * Get the JSON representation of a workflow from Github details
     * @param owner The owner of the Github repository
     * @param repoName The name of the repository
     * @param branch The branch of repository
     * @return The JSON representation of the workflow
     */
    @GetMapping(value = "/workflows/github.com/{owner}/{repoName}/tree/{branch}/**",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Workflow getWorkflowByGithubDetailsJson(@PathVariable("owner") String owner,
                                                   @PathVariable("repoName") String repoName,
                                                   @PathVariable("branch") String branch,
                                                   HttpServletRequest request) {
        // The wildcard end of the URL is the path
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = WorkflowController.extractPath(path);

        // Construct a GithubDetails object to search for in the database
        GithubDetails githubDetails = new GithubDetails(owner, repoName, branch, path);

        // Get workflow
        Workflow workflowModel = workflowService.getWorkflow(githubDetails);
        if (workflowModel == null) {
            throw new WorkflowNotFoundException();
        } else {
            return workflowModel;
        }
    }
}
