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

package org.commonwl.view.git;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GitServiceTest {

    private final RefNotFoundException branchNotFoundException = new RefNotFoundException("Branch not found");
    private final RefNotFoundException tagNotFoundException = new RefNotFoundException("Tag not found");

    private GitService spyGitService;
    private Git mockGit;
    private CheckoutCommand mockGoodCheckoutCommand;
    private CheckoutCommand mockBranchNotFoundCommand;
    private CheckoutCommand mockTagNotFoundCommand;
    private CheckoutCommand mockCheckoutCommand;

    @Before
    public void setup() throws GitAPIException {
        GitService gitService = new GitService(null, false);
        this.spyGitService = spy(gitService);
        this.mockGit = mock(Git.class);
        this.mockGoodCheckoutCommand = mock(CheckoutCommand.class);
        this.mockBranchNotFoundCommand = mock(CheckoutCommand.class);
        this.mockTagNotFoundCommand = mock(CheckoutCommand.class);
        this.mockCheckoutCommand = mock(CheckoutCommand.class);
        when(this.mockGit.checkout()).thenReturn(this.mockCheckoutCommand);
        when(this.mockBranchNotFoundCommand.call()).thenThrow(branchNotFoundException);
        when(this.mockTagNotFoundCommand.call()).thenThrow(tagNotFoundException);
    }

    @Test
    public void transferPathToBranch() throws Exception {
        GitService gitService = new GitService(null, false);
        GitDetails slashesInBranch = new GitDetails(null, "branchpart1",
                "branchpart2/branchpart3/workflowInRoot.cwl");

        GitDetails step1 = gitService.transferPathToBranch(slashesInBranch);
        assertEquals("branchpart1/branchpart2", step1.getBranch());
        assertEquals("branchpart3/workflowInRoot.cwl", step1.getPath());

        GitDetails step2 = gitService.transferPathToBranch(step1);
        assertEquals("branchpart1/branchpart2/branchpart3", step2.getBranch());
        assertEquals("workflowInRoot.cwl", step2.getPath());
    }

    @Test
    public void checksOutTag() throws Exception {
        when(mockCheckoutCommand.setName("refs/remotes/origin/mytag")).thenReturn(this.mockBranchNotFoundCommand);
        when(mockCheckoutCommand.setName("mytag")).thenReturn(this.mockGoodCheckoutCommand);
        doReturn(this.mockGit).when(this.spyGitService).cloneRepo(Mockito.any(String.class), Mockito.any(File.class));
        assertNotNull(this.spyGitService.getRepository(new GitDetails(null, "mytag", "foo"), false));
    }

    @Test
    public void checksOutBranch() throws GitAPIException, IOException {
        when(mockCheckoutCommand.setName("refs/remotes/origin/mybranch")).thenReturn(this.mockGoodCheckoutCommand);
        when(mockCheckoutCommand.setName("mytag")).thenReturn(this.mockTagNotFoundCommand);
        doReturn(this.mockGit).when(this.spyGitService).cloneRepo(Mockito.any(String.class), Mockito.any(File.class));
        assertNotNull(this.spyGitService.getRepository(new GitDetails(null, "mybranch", "foo"), false));
    }

    @Test()
    public void throwsFirstExceptionIfTagAndBranchFail() throws GitAPIException {
        when(mockCheckoutCommand.setName("refs/remotes/origin/mytag")).thenReturn(this.mockBranchNotFoundCommand);
        when(mockCheckoutCommand.setName("mytag")).thenReturn(this.mockTagNotFoundCommand);
        doReturn(this.mockGit).when(this.spyGitService).cloneRepo(Mockito.any(String.class), Mockito.any(File.class));
        boolean thrown = false;
        try {
            this.spyGitService.getRepository(new GitDetails(null, "mytag", "foo"), false);
        } catch (Exception e) {
            // Make sure it's not this.tagNotFoundException
            thrown = (e == this.branchNotFoundException);
        }
        assertTrue(thrown);
    }
}
