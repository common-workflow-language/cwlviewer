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

import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests the validator. Parsing is already checked in GithubServiceTest
 */
public class WorkflowFormValidatorTest {

    /**
     * Workflow form validator to test
     */
    private WorkflowFormValidator workflowFormValidator;

    /**
     * Set up new validator with mock Github service
     */
    @Before
    public void setUp() throws Exception {
        // Mock Github service which always returns non-null for downloads
        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        when(mockGithubService.downloadFile(anyObject())).thenReturn("");
        when(mockGithubService.detailsFromDirURL(anyString())).thenCallRealMethod();
        when(mockGithubService.detailsFromFileURL(anyString())).thenCallRealMethod();

        workflowFormValidator = new WorkflowFormValidator(mockGithubService);
    }

    /**
     * Github Directory URL
     */
    @Test
    public void directoryURL() throws Exception {

        WorkflowForm dirURL = new WorkflowForm("https://github.com/common-workflow-language/cwltool/tree/master/cwltool/schemas");

        Errors errors = new BeanPropertyBindingResult(dirURL, "workflowForm");
        GithubDetails details = workflowFormValidator.validateAndParse(dirURL, errors);

        assertNotNull(details);
        assertFalse(errors.hasErrors());

    }

    /**
     * Github File URL
     */
    @Test
    public void fileURL() throws Exception {

        WorkflowForm fileURL = new WorkflowForm("https://github.com/nlesc-sherlock/deeplearning/blob/master/CWLworkflow/pipeline.cwl");

        Errors errors = new BeanPropertyBindingResult(fileURL, "workflowForm");
        GithubDetails details = workflowFormValidator.validateAndParse(fileURL, errors);

        assertNotNull(details);
        assertFalse(errors.hasErrors());

    }

    /**
     * Empty URL
     */
    @Test
    public void emptyURL() throws Exception {

        WorkflowForm emptyURL = new WorkflowForm("");

        Errors errors = new BeanPropertyBindingResult(emptyURL, "workflowForm");
        GithubDetails willBeNull = workflowFormValidator.validateAndParse(emptyURL, errors);

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
        GithubDetails details = workflowFormValidator.validateAndParse(invalidURL, errors);

        assertNull(details);
        assertTrue(errors.hasErrors());

    }

    /**
     * Invalid URL
     */
    @Test
    public void cannotDownloadFile() throws Exception {

        // Mock Github service which always throws an exception for downloads
        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        when(mockGithubService.downloadFile(anyObject())).thenThrow(new IOException("404 Error"));
        when(mockGithubService.detailsFromDirURL(anyString())).thenCallRealMethod();
        when(mockGithubService.detailsFromFileURL(anyString())).thenCallRealMethod();

        WorkflowFormValidator validator = new WorkflowFormValidator(mockGithubService);

        WorkflowForm validFileURL = new WorkflowForm("https://github.com/nlesc-sherlock/deeplearning/blob/master/CWLworkflow/pipeline.cwl");

        Errors errors = new BeanPropertyBindingResult(validFileURL, "workflowForm");
        GithubDetails details = validator.validateAndParse(validFileURL, errors);

        assertNull(details);
        assertTrue(errors.hasErrors());

    }


}