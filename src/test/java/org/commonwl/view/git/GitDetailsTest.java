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

import org.junit.Test;

import static org.commonwl.view.git.GitDetails.normaliseUrl;
import static org.junit.Assert.assertEquals;

public class GitDetailsTest {

    private static final GitDetails GITHUB_DETAILS = new GitDetails("https://github.com/owner/repoName.git", "branch", "path/within/structure.cwl");
    private static final GitDetails GITLAB_DETAILS = new GitDetails("https://gitlab.com/owner/repoName.git", "branch", "path/within/structure.cwl");
    private static final GitDetails BITBUCKET_DETAILS = new GitDetails("https://bitbucket.org/owner/repoName.git", "branch", "path/within/structure.cwl");
    private static final GitDetails GENERIC_DETAILS = new GitDetails("https://could.com/be/anything.git", "branch", "path/within/structure.cwl");
    private static final GitDetails PACKED_DETAILS = new GitDetails("https://could.com/be/anything.git", "branch", "path/within/structure/packed.cwl#testId");

    /**
     * Branch getter, should default to "master" if null
     */
    @Test
    public void getBranch() throws Exception {
        GitDetails details1 = new GitDetails("https://repo.url/repo.git", "testbranch", "path/within/workflow.cwl");
        assertEquals("testbranch", details1.getBranch());

        // Null branch should default to master
        GitDetails details2 = new GitDetails("https://repo.url/repo.git", null, null);
        assertEquals("master", details2.getBranch());
    }

    /**
     * Path getter, should default to / if null
     */
    @Test
    public void getPath() throws Exception {
        GitDetails details1 = new GitDetails("https://repo.url/repo.git", "branch", "subdir/");
        assertEquals("subdir/", details1.getPath());

        GitDetails details2 = new GitDetails("https://repo.url/repo.git", "branch", "test/directory/structure.cwl");
        assertEquals("test/directory/structure.cwl", details2.getPath());

        GitDetails details3 = new GitDetails("https://repo.url/repo.git", null, null);
        assertEquals("/", details3.getPath());
    }

    /**
     * Get the type of URLs from Git details
     */
    @Test
    public void getType() throws Exception {
        assertEquals(GitType.GITHUB, GITHUB_DETAILS.getType());
        assertEquals(GitType.GITLAB, GITLAB_DETAILS.getType());
        assertEquals(GitType.BITBUCKET, BITBUCKET_DETAILS.getType());
        assertEquals(GitType.GENERIC, GENERIC_DETAILS.getType());
    }

    /**
     * Construct a URL from Git details
     */
    @Test
    public void getUrl() throws Exception {
        assertEquals("https://github.com/owner/repoName/blob/branch/path/within/structure.cwl", GITHUB_DETAILS.getUrl());
        assertEquals("https://github.com/owner/repoName/blob/overrideBranch/path/within/structure.cwl",
                GITHUB_DETAILS.getUrl("overrideBranch"));
        assertEquals("https://gitlab.com/owner/repoName/blob/branch/path/within/structure.cwl", GITLAB_DETAILS.getUrl());
        assertEquals("https://could.com/be/anything.git", GENERIC_DETAILS.getUrl());
        assertEquals("https://bitbucket.org/owner/repoName/src/branch/path/within/structure.cwl", BITBUCKET_DETAILS.getUrl());
    }

    /**
     * Construct the internal link to a workflow from Git details
     */
    @Test
    public void getInternalUrl() throws Exception {
        assertEquals("/workflows/github.com/owner/repoName/blob/branch/path/within/structure.cwl", GITHUB_DETAILS.getInternalUrl());
        assertEquals("/workflows/gitlab.com/owner/repoName/blob/branch/path/within/structure.cwl", GITLAB_DETAILS.getInternalUrl());
        assertEquals("/workflows/bitbucket.org/owner/repoName.git/branch/path/within/structure.cwl", BITBUCKET_DETAILS.getInternalUrl());
        assertEquals("/workflows/could.com/be/anything.git/branch/path/within/structure.cwl", GENERIC_DETAILS.getInternalUrl());
        assertEquals("/workflows/could.com/be/anything.git/branch/path/within/structure/packed.cwl#testId", PACKED_DETAILS.getInternalUrl());
    }

    /**
     * Construct the raw URL to a workflow file from Git details
     */
    @Test
    public void getRawUrl() throws Exception {
        assertEquals("https://raw.githubusercontent.com/owner/repoName/branch/path/within/structure.cwl", GITHUB_DETAILS.getRawUrl());
        assertEquals("https://gitlab.com/owner/repoName/raw/branch/path/within/structure.cwl", GITLAB_DETAILS.getRawUrl());
        assertEquals("https://bitbucket.org/owner/repoName/raw/branch/path/within/structure.cwl", BITBUCKET_DETAILS.getRawUrl());

        // No raw URL for generic git repo
        assertEquals("https://could.com/be/anything.git", GENERIC_DETAILS.getRawUrl());
    }

    /**
     * Normalise a URL removing protocol and www.
     */
    @Test
    public void getNormaliseUrl() throws Exception {
        assertEquals("github.com/test/url/here", normaliseUrl("https://www.github.com/test/url/here"));
        assertEquals("github.com/test/url/here", normaliseUrl("ssh://www.github.com/test/url/here"));
        assertEquals("github.com/test/url/here.git", normaliseUrl("git://github.com/test/url/here.git"));
        assertEquals("github.com/test/url/here", normaliseUrl("http://www.github.com/test/url/here"));
        assertEquals("github.com/test/url/here", normaliseUrl("http://github.com/test/url/here"));
    }
}