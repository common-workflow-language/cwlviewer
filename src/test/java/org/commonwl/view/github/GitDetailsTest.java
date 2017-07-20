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

package org.commonwl.view.github;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GitDetailsTest {

    /**
     * Branch getter, should default to "master" if null
     */
    @Test
    public void getBranch() throws Exception {
        GitDetails details1 = new GitDetails("owner", "repoName", "testbranch", "path/within/workflow.cwl");
        assertEquals("testbranch", details1.getBranch());

        // Null branch should default to master
        GitDetails details2 = new GitDetails("owner", "repoName", null, null);
        assertEquals("master", details2.getBranch());
    }

    /**
     * Path getter, should default to / if null
     */
    @Test
    public void getPath() throws Exception {
        GitDetails details1 = new GitDetails("owner", "repoName", "branch", "subdir/");
        assertEquals("subdir/", details1.getPath());

        GitDetails details2 = new GitDetails("owner", "repoName", "branch", "test/directory/structure.cwl");
        assertEquals("test/directory/structure.cwl", details2.getPath());

        GitDetails details3 = new GitDetails("owner", "repoName", null, null);
        assertEquals("/", details3.getPath());
    }

    /**
     * Construct a URL from Github details
     */
    @Test
    public void getURL() throws Exception {
        GitDetails details1 = new GitDetails("owner", "repoName", "branch", "path/within/structure.cwl");
        assertEquals("https://github.com/owner/repoName/tree/branch/path/within/structure.cwl", details1.getURL());
        assertEquals("https://github.com/owner/repoName/tree/overrideBranch/path/within/structure.cwl",
                details1.getURL("overrideBranch"));

        GitDetails details2 = new GitDetails("owner", "repoName", "branch", null);
        assertEquals("https://github.com/owner/repoName/tree/branch", details2.getURL());
        assertEquals("https://github.com/owner/repoName/tree/differentBranch", details2.getURL("differentBranch"));

    }

}