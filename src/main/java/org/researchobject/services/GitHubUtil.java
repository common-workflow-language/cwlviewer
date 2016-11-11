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

package org.researchobject.services;

import org.apache.commons.io.IOUtils;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles Github related functionality including API usage
 */
@Service
public class GitHubUtil {

    // Github API services
    private final ContentsService contentsService;
    private final UserService userService;

    // URL validation for directory links
    private final String GITHUB_DIR_REGEX = "^https:\\/\\/github\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:tree\\/([^/]+)\\/(.*))?$";
    private final Pattern githubDirPattern = Pattern.compile(GITHUB_DIR_REGEX);

    @Autowired
    public GitHubUtil(@Value("${githubAPI.authentication}") boolean authEnabled,
                      @Value("${githubAPI.username}") String username,
                      @Value("${githubAPI.password}") String password) {
        GitHubClient client = new GitHubClient();
        if (authEnabled) {
            client.setCredentials(username, password);
        }
        this.contentsService = new ContentsService(client);
        this.userService = new UserService(client);
    }

    /**
     * Extract the details of a Github directory URL using a regular expression
     * @param url The Github directory URL
     * @return A list with the groups of the regex match, [owner, repo, branch, path]
     */
    public List<String> detailsFromDirURL(String url) {
        List<String> matchGroups = new ArrayList<String>(4);
        Matcher m = githubDirPattern.matcher(url);
        if (m.find()) {
            for (int i=1; i < 5; i++) {
                matchGroups.add(m.group(i));
            }
        }
        return matchGroups;
    }

    /**
     * Get contents of a Github path from the API
     * @param owner The owner of the Github repository
     * @param repoName The Github repository
     * @param branch The branch of the repository to view the file(s)
     * @param path The path within the repository
     * @return A list of details for the file(s) or false if there is an API error
     * @throws IOException Any API errors which may have occured
     */
    public List<RepositoryContents> getContents(String owner, String repoName, String branch, String path) throws IOException {
        return contentsService.getContents(new RepositoryId(owner, repoName), path, branch);
    }

    /**
     * Get the details of a user from the Github API
     * @param username The username of the user we want to get data about
     * @return A user object containing the API information
     * @throws IOException Any API errors which may have occured
     */
    public User getUser(String username) throws IOException {
        return userService.getUser(username);
    }

    /**
     * Download a single file from a Github repository
     * @param owner The owner of the Github repository
     * @param repoName The Github repository
     * @param branch The branch of the repository to view the file(s)
     * @param path The path within the repository
     * @return A string with the contents of the file
     * @throws IOException Any API errors which may have occured
     */
    public String downloadFile(String owner, String repoName, String branch, String path) throws IOException {

        // Default to the master branch
        if (branch == null || branch.isEmpty()) {
            // TODO: get default branch name for this rather than assuming master
            branch = "master";
        }

        // Download the file and return the contents
        // rawgit.com used to download individual files from git with the correct media type
        URL downloadURL = new URL("https://cdn.rawgit.com/" + owner + "/" + repoName + "/" + branch + "/" + path);
        InputStream download = downloadURL.openStream();
        try {
            return IOUtils.toString(download);
        } finally {
            IOUtils.closeQuietly(download);
        }

    }
}
