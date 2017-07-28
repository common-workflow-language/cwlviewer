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

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Handles Git related functionality
 */
@Service
public class GitService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
     * Gets a repository, cloning into a local directory or
     * @param gitDetails The details of the Git repository
     * @returns The git object for the repository
     */
    public Git getRepository(GitDetails gitDetails)
            throws GitAPIException {
        Git repo = null;
        try {
            // Base dir from configuration, name from hash of repository URL
            File baseDir = new File(gitStorage.toString());
            String baseName = DigestUtils.shaHex(GitDetails.normaliseUrl(gitDetails.getRepoUrl()));

            // Check if folder already exists
            File repoDir = new File(baseDir, baseName);
            if (repoDir.exists() && repoDir.isDirectory()) {
                    repo = Git.open(repoDir);
                    repo.fetch().call();
            } else {
                // Create a folder and clone repository into it
                if (repoDir.mkdir()) {
                    repo = Git.cloneRepository()
                            .setCloneSubmodules(cloneSubmodules)
                            .setURI(gitDetails.getRepoUrl())
                            .setDirectory(repoDir)
                            .setCloneAllBranches(true)
                            .call();
                }
            }

            // Checkout the specific branch or commit ID
            if (repo != null) {
                repo.checkout()
                        .setName(gitDetails.getBranch())
                        .call();
                if (repo.getRepository().getFullBranch() != null) {
                    repo.pull().call();
                }
            }
        } catch (IOException ex) {
            logger.error("Could not open existing Git repository for '"
                    + gitDetails.getRepoUrl() + "'", ex);
        }

        return repo;
    }


}
