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

package org.commonwl.viewer.domain;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Agent;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.apache.taverna.robundle.manifest.Proxy;
import org.commonwl.viewer.services.GitHubService;
import org.eclipse.egit.github.core.RepositoryContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Workflow Research Object Bundle
 */
public class ROBundle {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private GitHubService githubService;

    private Bundle bundle;
    private GithubDetails githubInfo;
    private String commitSha;
    private Agent thisApp;
    private int singleFileSizeLimit;
    private Set<HashableAgent> authors = new HashSet<HashableAgent>();

    // Pattern for extracting version from a cwl file
    private final String CWL_VERSION_REGEX = "cwlVersion:\\s*\"?(?:cwl:)?([^\\s\"]+)\"?";
    private final Pattern cwlVersionPattern = Pattern.compile(CWL_VERSION_REGEX);

    /**
     * Creates a new research object bundle for a workflow from a Github repository
     * @param githubInfo The information necessary to access the Github directory associated with the RO
     * @throws IOException Any API errors which may have occurred
     */
    public ROBundle(GitHubService githubService, GithubDetails githubInfo, String commitSha,
                    String appName, String appURL, int singleFileSizeLimit) throws IOException {
        // File size limits
        this.singleFileSizeLimit = singleFileSizeLimit;

        // Create a new RO bundle
        this.bundle = Bundles.createBundle();
        this.githubInfo = githubInfo;
        this.githubService = githubService;
        this.commitSha = commitSha;

        Manifest manifest = bundle.getManifest();

        // Simplified attribution for RO bundle
        try {
            // Tool attribution in createdBy
            thisApp = new Agent(appName);
            thisApp.setUri(new URI(appURL));
            manifest.setCreatedBy(thisApp);

            // Retrieval Info
            // TODO: Make this importedBy/On/From
            manifest.setRetrievedBy(thisApp);
            manifest.setRetrievedOn(manifest.getCreatedOn());
            manifest.setRetrievedFrom(new URI("https://github.com/" + githubInfo.getOwner() + "/"
                    + githubInfo.getRepoName() + "/tree/" + commitSha + "/" + githubInfo.getPath()));

        } catch (URISyntaxException ex) {
            logger.error("Error creating URI for RO Bundle", ex);
        }

        // Make a directory in the RO bundle to store the files
        Path bundleFiles = bundle.getRoot().resolve("workflow");
        Files.createDirectory(bundleFiles);

        // Add the files from the Github repo to this workflow
        List<RepositoryContents> repoContents = githubService.getContents(githubInfo);
        addFiles(repoContents, bundleFiles);

        // Add combined authors
        manifest.setAuthoredBy(new ArrayList<Agent>(authors));
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

                try {
                    // Raw URI of the bundle
                    GithubDetails githubFile = new GithubDetails(githubInfo.getOwner(),
                            githubInfo.getRepoName(), githubInfo.getBranch(), repoContent.getPath());
                    URI rawURI = new URI("https://raw.githubusercontent.com/" + githubFile.getOwner() + "/" +
                            githubFile.getRepoName() + "/" + commitSha + "/" + githubFile.getPath());

                    // Variable to store file contents and aggregation
                    String fileContent = null;
                    PathMetadata aggregation;

                    // Download or externally link if oversized
                    if (repoContent.getSize() <= singleFileSizeLimit) {
                        // Get the content of this file from Github
                        fileContent = githubService.downloadFile(githubFile, commitSha);

                        // Save file to research object bundle
                        Path bundleFilePath = path.resolve(repoContent.getName());
                        Bundles.setStringValue(bundleFilePath, fileContent);

                        // Set retrieved information for this file in the manifest
                        aggregation = bundle.getManifest().getAggregation(bundleFilePath);
                        aggregation.setRetrievedFrom(rawURI);
                        aggregation.setRetrievedBy(thisApp);
                        aggregation.setRetrievedOn(aggregation.getCreatedOn());
                    } else {
                        logger.info("File " + repoContent.getName() + " is too large to download - " +
                                FileUtils.byteCountToDisplaySize(repoContent.getSize()) + "/" +
                                FileUtils.byteCountToDisplaySize(singleFileSizeLimit) +
                                ", linking externally to RO bundle");

                        // Set information for this file in the manifest
                        aggregation = bundle.getManifest().getAggregation(rawURI);
                        Proxy bundledAs = new Proxy();
                        bundledAs.setURI();
                        bundledAs.setFolder(path);
                        aggregation.setBundledAs(bundledAs);
                    }

                    // Special handling for cwl files
                    if (FilenameUtils.getExtension(repoContent.getName()).equals("cwl")) {
                        // Correct mime type (no official standard for yaml)
                        aggregation.setMediatype("text/x-yaml");

                        // Add conformsTo for version extracted from regex
                        if (fileContent != null) {
                            Matcher m = cwlVersionPattern.matcher(fileContent);
                            if (m.find()) {
                                aggregation.setConformsTo(new URI("https://w3id.org/cwl/" + m.group(1)));
                            }
                        }
                    }

                    // Add authors from github commits to the file
                    Set<HashableAgent> fileAuthors = githubService.getContributors(githubFile, commitSha);
                    authors.addAll(fileAuthors);
                    aggregation.setAuthoredBy(new ArrayList<Agent>(fileAuthors));

                } catch (URISyntaxException ex) {
                    logger.error("Error creating URI for RO Bundle", ex);
                }
            }
        }
    }

    /**
     * Save the Research Object bundle to disk
     * @param directory The directory in which the RO will be saved
     * @return The path to the research object
     * @throws IOException Any errors in saving
     */
    public Path saveToFile(Path directory) throws IOException {
        String fileName = "bundle-" + java.util.UUID.randomUUID() + ".zip";
        Path bundleLocation = Files.createFile(directory.resolve(fileName));
        Bundles.closeAndSaveBundle(bundle, bundleLocation);
        return bundleLocation;
    }

}
