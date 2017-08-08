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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
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

    // File size limit for loading in bytes
    private int singleFileSizeLimit;

    @Autowired
    public GitService(@Value("${gitStorage}") Path gitStorage,
                      @Value("${gitAPI.cloneSubmodules}") boolean cloneSubmodules,
                      @Value("${singleFileSizeLimit}") int singleFileSizeLimit) {
        this.gitStorage = gitStorage;
        this.cloneSubmodules = cloneSubmodules;
        this.singleFileSizeLimit = singleFileSizeLimit;
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
                String branch = repo.getRepository().getFullBranch();
                if (branch != null && !branch.startsWith(gitDetails.getBranch())) {
                    repo.pull().call();
                }
            }
        } catch (IOException ex) {
            logger.error("Could not open existing Git repository for '"
                    + gitDetails.getRepoUrl() + "'", ex);
        }

        return repo;
    }

    /**
     * Get the contents of a file from the Git repository
     * @param repository The Git repository
     * @param refOrCommitId The branch name or commit ID as a string
     * @return The contents of the file as a string
     * @throws IOException Any errors in retrieving the file
     */
    public String getFile(Repository repository, String refOrCommitId) throws IOException {
        String content;

        // Get the ObjectID from the ref or commit ID string
        if (!ObjectId.isId(refOrCommitId)) {
            refOrCommitId = "refs/remotes/origin/" + refOrCommitId;
        }
        ObjectId commitId = repository.resolve(refOrCommitId);

        // Walk over commits using defined filtering
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree = commit.getTree();

            // Try to find specific file in the repository
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create("README.md"));
                if (!treeWalk.next()) {
                    throw new IllegalStateException("Did not find expected file");
                }

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                content = new String(loader.getCachedBytes(singleFileSizeLimit));
            }
            revWalk.dispose();
        }

        return content;
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

}
