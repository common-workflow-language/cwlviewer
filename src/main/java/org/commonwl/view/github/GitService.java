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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

import static java.util.Collections.singleton;

/**
 * Handles Git related functionality
 */
@Service
public class GitService {

    /**
     * Gets a map of commits and their references from a remote repository
     * @param gitDetails
     * @return
     * @throws GitAPIException
     */
    public Map<String, Ref> getCommits(GitDetails gitDetails) throws GitAPIException {
        return Git.lsRemoteRepository()
                .setHeads(true)
                .setTags(true)
                .setRemote(gitDetails.getRepoUrl())
                .callAsMap();
    }

    /**
     * Clone a repository into a local directory
     * @param gitDetails The details of the Git repository
     * @param dest The destination folder for the clone
     */
    public Git cloneRepository(GitDetails gitDetails, File dest)
            throws GitAPIException {
        return Git.cloneRepository()
                .setCloneSubmodules(true)
                .setURI(gitDetails.getRepoUrl())
                .setDirectory(dest)
                .setBranchesToClone(singleton("refs/heads/" + gitDetails.getBranch()))
                .setBranch("refs/heads/" + gitDetails.getBranch())
                .call();
    }


}
