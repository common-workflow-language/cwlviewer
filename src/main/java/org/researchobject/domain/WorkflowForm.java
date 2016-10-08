package org.researchobject.domain;

/**
 * Represents the submission form on the main page to create a new workflow
 * Currently just contains a github URL
 */
public class WorkflowForm {

    private String githubURL;

    public String getGithubURL() {
        return githubURL;
    }

    public void setGithubURL(String githubURL) {
        this.githubURL = githubURL;
    }
}
