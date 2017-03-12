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

package org.commonwl.viewer.github;

import org.apache.commons.io.IOUtils;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.ContentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles Github related functionality including API usage
 * Is mainly a wrapper for org.eclipse.egit.github.core
 */
@Service
public class GitHubService {

    // Github API specific strings
    public static final String TYPE_DIR = "dir";
    public static final String TYPE_FILE = "file";

    // Github API services
    private final ContentsService contentsService;
    private final CommitService commitService;

    // URL validation for directory links
    private final String GITHUB_CWL_REGEX = "^https?:\\/\\/github\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:tree|blob)\\/([^/]+)(?:\\/(.+\\.cwl))$";
    private final Pattern githubCwlPattern = Pattern.compile(GITHUB_CWL_REGEX);

    @Autowired
    public GitHubService(@Value("${githubAPI.authentication}") String authSetting,
                         @Value("${githubAPI.oauthToken}") String token,
                         @Value("${githubAPI.username}") String username,
                         @Value("${githubAPI.password}") String password) {
        GitHubClient client = new GitHubClient();
        if (authSetting.equals("basic")) {
            client.setCredentials(username, password);
        } else if (authSetting.equals("oauth")) {
            client.setOAuth2Token(token);
        }
        this.contentsService = new ContentsService(client);
        this.commitService = new CommitService(client);
    }

    /**
     * Extract the details of a Github cwl file URL using a regular expression
     * @param url The Github URL to a cwl file
     * @return A list with the groups of the regex match, [owner, repo, branch, path]
     */
    public GithubDetails detailsFromCwlURL(String url) {
        Matcher m = githubCwlPattern.matcher(url);
        if (m.find()) {
            return new GithubDetails(m.group(1), m.group(2), m.group(3), m.group(4));
        }
        return null;
    }

    /**
     * Get contents of a Github path from the API
     * @param githubInfo The information to access the repository
     * @return A list of details for the file(s) or false if there is an API error
     * @throws IOException Any API errors which may have occurred
     */
    public List<RepositoryContents> getContents(GithubDetails githubInfo) throws IOException {
        return contentsService.getContents(new RepositoryId(githubInfo.getOwner(), githubInfo.getRepoName()),
                githubInfo.getPath(), githubInfo.getBranch());
    }

    /**
     * Download a single file from a Github repository
     * @param githubInfo The information to access the repository
     * @param sha The commit ID to download a specific version of a file
     * @return A string with the contents of the file
     * @throws IOException Any API errors which may have occurred
     */
    public String downloadFile(GithubDetails githubInfo, String sha) throws IOException {
        // Download the file and return the contents
        // rawgit.com used to download individual files from git with the correct media type
        String url = String.format("https://cdn.rawgit.com/%s/%s/%s/%s", githubInfo.getOwner(),
                githubInfo.getRepoName(), sha, githubInfo.getPath());
        URL downloadURL = new URL(url);
        InputStream download = downloadURL.openStream();
        try {
            return IOUtils.toString(download);
        } finally {
            IOUtils.closeQuietly(download);
        }
    }

    /**
     * Download a single file from a repository
     * @param githubInfo The information to access the repository
     * @return A string with the contents of the file
     * @throws IOException Any API errors which may have occurred
     */
    public String downloadFile(GithubDetails githubInfo) throws IOException {
        return downloadFile(githubInfo, githubInfo.getBranch());
    }

    /**
     * Gets the latest commit sha hash for a file on a branch
     * If hash is given as a branch, this will return the same hash if valid
     * @param githubInfo The information to access the repository
     * @return A sha hash for the latest commit on a file
     * @throws IOException Any API errors which may have occurred
     */
    public String getCommitSha(GithubDetails githubInfo) throws IOException {
        RepositoryId repo = new RepositoryId(githubInfo.getOwner(), githubInfo.getRepoName());
        return commitService.getCommits(repo, githubInfo.getBranch(), githubInfo.getPath()).get(0).getSha();
    }

    /**
     * Get a list of commits
     * @param githubInfo The information to access the repository
     * @return A list of unique contributors
     * @throws IOException Any API errors which may have occurred
     */
    public List<RepositoryCommit> getCommits(GithubDetails githubInfo) throws IOException {
        RepositoryId repo = new RepositoryId(githubInfo.getOwner(), githubInfo.getRepoName());
        return commitService.getCommits(repo, githubInfo.getBranch(), githubInfo.getPath());
    }
}
