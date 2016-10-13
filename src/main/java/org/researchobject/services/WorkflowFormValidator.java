package org.researchobject.services;

import org.eclipse.egit.github.core.RepositoryContents;
import org.researchobject.domain.WorkflowForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.io.IOException;
import java.util.List;

/**
 * Runs validation on the workflow form from the main page
 */
@Component
public class WorkflowFormValidator implements Validator {

    /**
     * Github API service
     */
    private final GitHubUtil githubUtil;

    @Autowired
    public WorkflowFormValidator(GitHubUtil githubUtil) {
        this.githubUtil = githubUtil;
    }

    /**
     * Types of class the this validator supports, WorkflowForm
     * @param theClass The class which is being validated
     * @return Whether the class can be validated using this validator
     */
    public boolean supports(Class theClass) {
        return WorkflowForm.class.equals(theClass);
    }

    /**
     * Validates a WorkflowForm to ensure the URL is not empty and directory contains cwl files
     * @param obj The given WorkFlowForm
     * @param e Any errors from validation
     */
    public void validate(Object obj, Errors e) {
        ValidationUtils.rejectIfEmptyOrWhitespace(e, "githubURL", "githubURL.emptyOrWhitespace");

        // Only continue if not null and isn't just whitespace
        if (!e.hasErrors()) {
            WorkflowForm form = (WorkflowForm) obj;
            List<String> directoryDetails = githubUtil.detailsFromDirURL(form.getGithubURL());

            // If the URL is valid and details could be extracted
            if (directoryDetails.size() > 0) {

                // Store returned details
                final String owner = directoryDetails.get(0);
                final String repoName = directoryDetails.get(1);
                final String branch = directoryDetails.get(2);
                final String path = directoryDetails.get(3);

                // Check the repository exists and get content to ensure that branch/path exist
                try {
                    List<RepositoryContents> repoContents = githubUtil.getContents(owner, repoName, branch, path);

                    // Check that there is at least 1 .cwl file
                    boolean foundCWL = false;
                    for (RepositoryContents repoContent : repoContents) {
                        int eIndex = repoContent.getName().lastIndexOf('.') + 1;
                        if (eIndex > 0) {
                            String extension = repoContent.getName().substring(eIndex);
                            if (extension.equals("cwl")) {
                                foundCWL = true;
                            }
                        }
                    }
                    if (!foundCWL) {
                        // The URL does not contain any .cwl files
                        e.rejectValue("githubURL", "githubURL.missingWorkflow");
                    }
                } catch (IOException ex) {
                    // Given repository/branch/path does not exist or API error occured
                    e.rejectValue("githubURL", "githubURL.apiError");
                }
            } else {
                // The Github URL is not valid
                e.rejectValue("githubURL", "githubURL.invalid");
            }
        }
    }
}