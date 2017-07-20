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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

import static java.util.Collections.singleton;
import static org.apache.jena.ext.com.google.common.io.Files.createTempDir;

/**
 * Handles Git related functionality
 */
@Service
public class GitService {

    // Location to check out git repositories into
    private Path gitStorage;

    // Whether submodules are also cloned
    private boolean cloneSubmodules;

    @Autowired
    public GitService(@Value("${gitStorage}") Path gitStorage,
                      @Value("${gitAPI.cloneSubmodules}") boolean cloneSubmodules) {
        this.gitStorage = gitStorage;
        this.cloneSubmodules = cloneSubmodules;
    }

    /**
     * Clone a repository into a local directory
     * @param gitDetails The details of the Git repository
     */
    public Git cloneRepository(GitDetails gitDetails)
            throws GitAPIException {
        return Git.cloneRepository()
                .setCloneSubmodules(cloneSubmodules)
                .setURI(gitDetails.getRepoUrl())
                .setDirectory(createTempDir())
                .setBranchesToClone(singleton("refs/heads/" + gitDetails.getBranch()))
                .setBranch("refs/heads/" + gitDetails.getBranch())
                .call();
    }


}
