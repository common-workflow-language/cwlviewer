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
import org.commonwl.view.researchobject.HashableAgent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.apache.jena.ext.com.google.common.io.Files.createTempDir;

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
     * @param reuseDir Whether the cached repository can be used
     * @returns The git object for the repository
     */
    public Git getRepository(GitDetails gitDetails, boolean reuseDir)
            throws GitAPIException {
        Git repo = null;
        while (repo == null) {
            try {
                if (reuseDir) {
                    // Base dir from configuration, name from hash of repository URL
                    File baseDir = new File(gitStorage.toString());
                    String baseName = DigestUtils.sha1Hex(GitDetails.normaliseUrl(gitDetails.getRepoUrl()));

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
                } else {
                    // Another thread is already using the existing folder
                    // Must create another temporary one
                    repo = Git.cloneRepository()
                            .setCloneSubmodules(cloneSubmodules)
                            .setURI(gitDetails.getRepoUrl())
                            .setDirectory(createTempDir())
                            .setCloneAllBranches(true)
                            .call();
                }

                // Checkout the specific branch or commit ID
                if (repo != null) {
                    // Create a new local branch if it does not exist and not a commit ID
                    String branchOrCommitId = gitDetails.getBranch();
                    if (!ObjectId.isId(branchOrCommitId)) {
                        branchOrCommitId = "refs/remotes/origin/" + branchOrCommitId;
                    }
                    repo.checkout()
                            .setName(branchOrCommitId)
                            .call();
                }
            } catch (RefNotFoundException ex) {
                // Attempt slashes in branch fix
                GitDetails correctedForSlash = transferPathToBranch(gitDetails);
                if (correctedForSlash != null) {
                    gitDetails = correctedForSlash;
                } else {
                    throw ex;
                }
            } catch (IOException ex) {
                logger.error("Could not open existing Git repository for '"
                        + gitDetails.getRepoUrl() + "'", ex);
            }
        }

        return repo;
    }

    /**
     * Gets the commit ID of the HEAD for the given repository
     * @param repo The Git repository
     * @return The commit ID of the HEAD for the repository
     * @throws IOException If the HEAD is detached
     */
    public String getCurrentCommitID(Git repo) throws IOException {
        return repo.getRepository().findRef("HEAD").getObjectId().getName();
    }

    /**
     * Gets a set of authors for a path in a given repository
     * @param repo The git repository
     * @param path The path to get commits for
     * @return An iterable of commits
     * @throws GitAPIException Any API errors which may occur
     * @throws URISyntaxException Error constructing mailto link
     */
    public Set<HashableAgent> getAuthors(Git repo, String path) throws GitAPIException, URISyntaxException {
        Iterable<RevCommit> logs = repo.log().addPath(path).call();
        Set<HashableAgent> fileAuthors = new HashSet<>();
        for (RevCommit rev : logs) {
            // Use author first with backup of committer
            PersonIdent author = rev.getAuthorIdent();
            if (author == null) {
                author = rev.getCommitterIdent();
            }
            // Create a new agent and add as much detail as possible
            if (author != null) {
                HashableAgent newAgent = new HashableAgent();
                String name = author.getName();
                if (name != null && name.length() > 0) {
                    newAgent.setName(author.getName());
                }
                String email = author.getEmailAddress();
                if (email != null && email.length() > 0) {
                    newAgent.setUri(new URI("mailto:" + author.getEmailAddress()));
                }
                fileAuthors.add(newAgent);
            }
        }
        return fileAuthors;
    }

    /**
     * Transfers part of the path to the branch to fix / in branch names
     * @param githubInfo The current Github info possibly with
     *                   part of the branch name in the path
     * @return A potentially corrected set of Github details,
     *         or null if there are no slashes in the path
     */
    public GitDetails transferPathToBranch(GitDetails githubInfo) {
        String path = githubInfo.getPath();
        String branch = githubInfo.getBranch();

        int firstSlash = path.indexOf("/");
        if (firstSlash > 0) {
            branch += "/" + path.substring(0, firstSlash);
            path = path.substring(firstSlash + 1);
            return new GitDetails(githubInfo.getRepoUrl(), branch,
                    path);
        } else {
            return null;
        }
    }

}
