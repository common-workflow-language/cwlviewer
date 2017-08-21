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

import static org.junit.Assert.assertEquals;

public class GitServiceTest {

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
}
