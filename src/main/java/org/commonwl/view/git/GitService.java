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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.commonwl.view.researchobject.HashableAgent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Handles Git related functionality */
@Service
public class GitService {

  // Location to check out git repositories into
  private final Path gitStorage;

  // Whether submodules are also cloned
  private final boolean cloneSubmodules;

  @Autowired
  public GitService(
      @Value("${gitStorage}") Path gitStorage,
      @Value("${gitAPI.cloneSubmodules}") boolean cloneSubmodules) {
    this.gitStorage = gitStorage;
    this.cloneSubmodules = cloneSubmodules;
  }

  /**
   * Gets a repository, cloning into a local directory or
   *
   * @param gitDetails The details of the Git repository
   * @param reuseDir Whether the cached repository can be used
   * @return The git object for the repository
   */
  public Git getRepository(GitDetails gitDetails, boolean reuseDir)
      throws GitAPIException, IOException {
    Git repo;
    if (reuseDir) {
      // Base dir from configuration, name from hash of repository URL
      String baseName = DigestUtils.sha1Hex(GitDetails.normaliseUrl(gitDetails.getRepoUrl()));

      // Check if folder already exists
      Path repoDir = gitStorage.resolve(baseName);
      if (Files.isReadable(repoDir) && Files.isDirectory(repoDir)) {
        try {
          repo = Git.open(repoDir.toFile());
          repo.fetch().call();
        } catch (RepositoryNotFoundException ex) {
          repo = cloneRepo(gitDetails.getRepoUrl(), repoDir.toFile());
        }
      } else {
        // Create a folder and clone repository into it
        Files.createDirectory(repoDir);
        try {
          repo = cloneRepo(gitDetails.getRepoUrl(), repoDir.toFile());
        } catch (CheckoutConflictException ex) {
          repo = cloneRepo(gitDetails.getRepoUrl(), createTempDir());
        }
      }
    } else {
      // Another thread is already using the existing folder
      // Must create another temporary one
      repo = cloneRepo(gitDetails.getRepoUrl(), createTempDir());
    }

    // Checkout the specific branch or commit ID
    if (repo != null) {
      // Create a new local branch if it does not exist and not a commit ID
      String branchOrCommitId = gitDetails.getBranch();
      final boolean isId = ObjectId.isId(branchOrCommitId);
      if (!isId) {
        branchOrCommitId = "refs/remotes/origin/" + branchOrCommitId;
      }
      try {
        repo.checkout().setName(branchOrCommitId).call();
      } catch (Exception ex) {
        // Maybe it was a tag
        if (!isId && ex instanceof RefNotFoundException) {
          final String tag = gitDetails.getBranch();
          try {
            repo.checkout().setName(tag).call();
          } catch (Exception ex2) {
            // Throw the first exception, to keep the same behavior as before.
            throw ex;
          }
        } else {
          throw ex;
        }
      }
    }

    return repo;
  }

  /**
   * Gets the commit ID of the HEAD for the given repository
   *
   * @param repo The Git repository
   * @return The commit ID of the HEAD for the repository
   * @throws IOException If the HEAD is detached
   */
  public String getCurrentCommitID(Git repo) throws IOException {
    return repo.getRepository().findRef("HEAD").getObjectId().getName();
  }

  /**
   * Gets a set of authors for a path in a given repository
   *
   * @param repo The git repository
   * @param path The path to get commits for
   * @return An iterable of commits
   * @throws GitAPIException Any API errors which may occur
   * @throws URISyntaxException Error constructing mailto link
   */
  public Set<HashableAgent> getAuthors(Git repo, String path)
      throws GitAPIException, URISyntaxException {
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
   *
   * @param githubInfo The current Github info possibly with part of the branch name in the path
   * @return A potentially corrected set of Github details, or null if there are no slashes in the
   *     path
   */
  public GitDetails transferPathToBranch(GitDetails githubInfo) {
    String path = githubInfo.getPath();
    String branch = githubInfo.getBranch();

    int firstSlash = path.indexOf("/");
    if (firstSlash > 0) {
      branch += "/" + path.substring(0, firstSlash);
      path = path.substring(firstSlash + 1);
      GitDetails newDetails = new GitDetails(githubInfo.getRepoUrl(), branch, path);
      newDetails.setPackedId(githubInfo.getPackedId());
      return newDetails;
    } else {
      return null;
    }
  }

  /**
   * Clones a Git repository
   *
   * @param repoUrl the url of the Git repository
   * @param directory the directory to clone the repo into
   * @return a Git instance
   * @throws GitAPIException if any error occurs cloning the repo
   */
  protected Git cloneRepo(String repoUrl, File directory) throws GitAPIException {
    return Git.cloneRepository()
        .setCloneSubmodules(cloneSubmodules)
        .setURI(repoUrl)
        .setDirectory(directory)
        .setCloneAllBranches(true)
        .call();
  }

  protected File createTempDir() throws IOException {
    Path repoDir = gitStorage.resolve(String.valueOf(UUID.randomUUID()));
    Files.createDirectory(repoDir);
    return repoDir.toFile();
  }
}
