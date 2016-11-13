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

package org.researchobject.domain;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Agent;
import org.apache.taverna.robundle.manifest.Manifest;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.User;
import org.researchobject.services.GitHubService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Workflow Research Object Bundle
 */
public class ROBundle {

    private GitHubService githubService;

    private Bundle bundle;
    private GithubDetails githubInfo;

    /**
     * Creates a new research object bundle for a workflow from a Github repository
     * @param githubInfo The information necessary to access the Github directory associated with the RO
     * @throws IOException Any API errors which may have occurred
     */
    public ROBundle(GitHubService githubService, GithubDetails githubInfo,
                    String appName, String appURL) throws IOException {
        // TODO: Add back file size checking on individual files as well as whole bundle

        // Create a new RO bundle
        this.bundle = Bundles.createBundle();
        this.githubInfo = githubInfo;
        this.githubService = githubService;

        Manifest manifest = bundle.getManifest();

        // Simplified attribution for RO bundle
        try {
            Agent cwlViewer = new Agent(appName);
            cwlViewer.setUri(new URI(appURL));
            manifest.setCreatedBy(cwlViewer);

            // Github author attribution
            // TODO: way to add all the contributors somehow
            // TODO: set the aggregates details according to the github information
            User authorDetails = githubService.getUser(githubInfo.getOwner());

            List<Agent> authorList = new ArrayList<>(1);
            Agent author = new Agent(authorDetails.getName());
            author.setUri(new URI(authorDetails.getHtmlUrl()));

            // This tool supports putting your ORCID in the blog field of github as a URL
            // eg http://orcid.org/0000-0000-0000-0000
            String authorBlog = authorDetails.getBlog();
            if (authorBlog.startsWith("http://orcid.org/")) {
                author.setOrcid(new URI(authorBlog));
            }

            authorList.add(author);
            manifest.setAuthoredBy(authorList);

        } catch (URISyntaxException ex) {
            System.out.println("Incorrect URI Syntax");
        }

        // Make a directory in the RO bundle to store the files
        Path bundleFiles = bundle.getRoot().resolve("workflow");
        Files.createDirectory(bundleFiles);

        // Add the files from the Github repo to this workflow
        List<RepositoryContents> repoContents = githubService.getContents(githubInfo);
        addFiles(repoContents, bundleFiles);
    }

    /**
     * Add files to this bundle from a list of Github repository contents
     * @param repoContents The contents of the Github repository
     * @param path The path in the Research Object to add the files
     */
    private void addFiles(List<RepositoryContents> repoContents, Path path) throws IOException {

        // Loop through repo contents and add them
        for (RepositoryContents repoContent : repoContents) {

            // Parse subdirectories if they exist
            if (repoContent.getType().equals("dir")) {

                // Get the contents of the subdirectory
                GithubDetails githubSubdir = new GithubDetails(githubInfo.getOwner(),
                        githubInfo.getRepoName(), githubInfo.getBranch(), repoContent.getPath());
                List<RepositoryContents> subdirectory = githubService.getContents(githubSubdir);

                // Create a new folder in the RO for it
                Path subdirPath = path.resolve(repoContent.getName());
                Files.createDirectory(subdirPath);

                // Add the files in the subdirectory to this new folder
                addFiles(subdirectory, subdirPath);

            // Otherwise this is a file so add to the bundle
            } else if (repoContent.getType().equals("file")) {

                // Get the content of this file from Github
                GithubDetails githubFile = new GithubDetails(githubInfo.getOwner(),
                        githubInfo.getRepoName(), githubInfo.getBranch(), repoContent.getPath());
                String fileContent = githubService.downloadFile(githubFile);

                // Save file to research object bundle
                Path newFilePort = path.resolve(repoContent.getName());
                Bundles.setStringValue(newFilePort, fileContent);

            }
        }
    }

    /**
     * Save the Research Object bundle to disk
     */
    public void saveToTempFile() throws IOException {
        // Save the Research Object Bundle
        Path zip = Files.createTempFile("bundle", ".zip");
        Bundles.closeAndSaveBundle(bundle, zip);
    }

}
