package org.commonwl.view.workflow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class WorkflowFormTest {

    /**
     * Test for the form stripping unnecessary trailing slashes from directory URLs
     */
    @Test
    public void getGithubURL() throws Exception {

        String unchangedURL = "https://github.com/common-workflow-language/workflows/tree/master/workflows/compile";
        WorkflowForm testForm = new WorkflowForm(unchangedURL);
        assertEquals(unchangedURL, testForm.getGithubURL());

        WorkflowForm testForm2 = new WorkflowForm("https://github.com/common-workflow-language/workflows/tree/master/workflows/compile/");
        assertEquals("https://github.com/common-workflow-language/workflows/tree/master/workflows/compile", testForm2.getGithubURL());

        testForm2.setGithubURL("https://github.com/common-workflow-language/workflows/tree/master/workflows/make-to-cwl/////");
        assertEquals("https://github.com/common-workflow-language/workflows/tree/master/workflows/make-to-cwl", testForm2.getGithubURL());

    }

}