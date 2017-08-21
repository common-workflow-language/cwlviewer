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

import org.commonwl.view.git.GitDetails;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.Assert.*;

/**
 * Tests the validator. Parsing is already checked in GithubServiceTest
 */
public class WorkflowFormValidatorTest {

    /**
     * Workflow form validator to test
     */
    private WorkflowFormValidator workflowFormValidator = new WorkflowFormValidator();

    /**
     * Github File URL
     */
    @Test
    public void githubUrl() throws Exception {

        WorkflowForm githubUrl = new WorkflowForm("https://github.com/nlesc-sherlock/deeplearning/blob/master/CWLworkflow/pipeline.cwl");
        Errors errors = new BeanPropertyBindingResult(githubUrl, "workflowForm");
        GitDetails details = workflowFormValidator.validateAndParse(githubUrl, errors);

        assertNotNull(details);
        assertEquals("https://github.com/nlesc-sherlock/deeplearning.git", details.getRepoUrl());
        assertEquals("master", details.getBranch());
        assertEquals("CWLworkflow/pipeline.cwl", details.getPath());
        assertFalse(errors.hasErrors());

    }

    /**
     * Gitlab File URL
     */
    @Test
    public void gitlabUrl() throws Exception {

        WorkflowForm gitlabUrl = new WorkflowForm("https://gitlab.com/unduthegun/stellaris-emblem-lab/blob/cwl/textures/textures.cwl");
        Errors errors = new BeanPropertyBindingResult(gitlabUrl, "workflowForm");
        GitDetails details = workflowFormValidator.validateAndParse(gitlabUrl, errors);

        assertNotNull(details);
        assertEquals("https://gitlab.com/unduthegun/stellaris-emblem-lab.git", details.getRepoUrl());
        assertEquals("cwl", details.getBranch());
        assertEquals("textures/textures.cwl", details.getPath());
        assertFalse(errors.hasErrors());

    }

    /**
     * Generic File URL
     */
    @Test
    public void genericUrl() throws Exception {

        WorkflowForm genericUrl = new WorkflowForm("https://bitbucket.org/markrobinson96/workflows.git");
        genericUrl.setBranch("branchName");
        genericUrl.setPath("path/to/workflow.cwl");

        Errors errors = new BeanPropertyBindingResult(genericUrl, "workflowForm");
        GitDetails details = workflowFormValidator.validateAndParse(genericUrl, errors);

        assertNotNull(details);
        assertEquals("https://bitbucket.org/markrobinson96/workflows.git", details.getRepoUrl());
        assertEquals("branchName", details.getBranch());
        assertEquals("path/to/workflow.cwl", details.getPath());
        assertFalse(errors.hasErrors());

    }

    /**
     * Packed URL
     */
    @Test
    public void packedUrl() throws Exception {

        WorkflowForm githubUrl = new WorkflowForm("https://github.com/MarkRobbo/workflows/tree/master/packed.cwl#workflowId");
        Errors errors = new BeanPropertyBindingResult(githubUrl, "workflowForm");
        GitDetails details = workflowFormValidator.validateAndParse(githubUrl, errors);

        assertNotNull(details);
        assertEquals("https://github.com/MarkRobbo/workflows.git", details.getRepoUrl());
        assertEquals("master", details.getBranch());
        assertEquals("packed.cwl", details.getPath());
        assertEquals("workflowId", details.getPackedId());
        assertFalse(errors.hasErrors());

    }

    /**
     * Empty URL
     */
    @Test
    public void emptyURL() throws Exception {

        WorkflowForm emptyURL = new WorkflowForm("");

        Errors errors = new BeanPropertyBindingResult(emptyURL, "workflowForm");
        GitDetails willBeNull = workflowFormValidator.validateAndParse(emptyURL, errors);

        assertNull(willBeNull);
        assertTrue(errors.hasErrors());

    }

    /**
     * Invalid URL
     */
    @Test
    public void invalidURL() throws Exception {

        WorkflowForm invalidURL = new WorkflowForm("https://google.com/clearly/not/github/url");

        Errors errors = new BeanPropertyBindingResult(invalidURL, "workflowForm");
        GitDetails details = workflowFormValidator.validateAndParse(invalidURL, errors);

        assertNull(details);
        assertTrue(errors.hasErrors());

    }

    /**
     * Generic File URL without branch or path
     */
    @Test
    public void noBranchOrPath() throws Exception {

        WorkflowForm genericUrl = new WorkflowForm("https://bitbucket.org/markrobinson96/workflows.git");

        Errors errors = new BeanPropertyBindingResult(genericUrl, "workflowForm");
        GitDetails details = workflowFormValidator.validateAndParse(genericUrl, errors);

        assertNull(details);
        assertTrue(errors.hasErrors());

    }

}