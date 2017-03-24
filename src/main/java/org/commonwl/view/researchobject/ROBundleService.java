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

package org.commonwl.view.researchobject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Agent;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.apache.taverna.robundle.manifest.Proxy;
import org.commonwl.viewer.services.GitHubService;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
 * Service handling Research Object Bundles
 */
@Service
public class ROBundleService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Services
    private GitHubService githubService;

    // Configuration variables
    private Agent appAgent;
    private int singleFileSizeLimit;
    private Path bundleStorage;

    // Pattern for extracting version from a cwl file
    private final String CWL_VERSION_REGEX = "cwlVersion:\\s*\"?(?:cwl:)?([^\\s\"]+)\"?";
    private final Pattern cwlVersionPattern = Pattern.compile(CWL_VERSION_REGEX);

    /**
     * Creates an instance of this service which handles Research Object Bundles
     * @param bundleStorage The configured storage location for bundles
     * @param appName The name of the application from properties, for attribution
     * @param appURL The URL of the application from properties, for attribution
     * @param singleFileSizeLimit The file size limit for the RO bundle
     * @param githubService The service for handling Github functionality
     * @throws URISyntaxException Error in creating URI for appURL
     */
    @Autowired
    public ROBundleService(@Value("${bundleStorage}") Path bundleStorage,
                           @Value("${applicationName}") String appName,
                           @Value("${applicationURL}") String appURL,
                           @Value("${singleFileSizeLimit}") int singleFileSizeLimit,
                           GitHubService githubService) throws URISyntaxException {
        this.bundleStorage = bundleStorage;
        this.appAgent = new Agent(appName);
        appAgent.setUri(new URI(appURL));
        this.singleFileSizeLimit = singleFileSizeLimit;
        this.githubService = githubService;
    }

    /**
     * Creates a new research object bundle for a workflow from a Github repository
     * @param githubInfo The information to access the repository
     * @return The constructed bundle
     */
    public Bundle newBundleFromGithub(GithubDetails githubInfo) throws IOException {

        // Create a new RO bundle
        Bundle bundle = Bundles.createBundle();
        Manifest manifest = bundle.getManifest();

        // Simplified attribution for RO bundle
        try {
            // Tool attribution in createdBy
            manifest.setCreatedBy(appAgent);

            // Retrieval Info
            // TODO: Make this importedBy/On/From
            manifest.setRetrievedBy(appAgent);
            manifest.setRetrievedOn(manifest.getCreatedOn());
            manifest.setRetrievedFrom(new URI(githubInfo.getURL()));

        } catch (URISyntaxException ex) {
            logger.error("Error creating URI for RO Bundle", ex);
        }

        // Make a directory in the RO bundle to store the files
        Path bundleFiles = bundle.getRoot().resolve("workflow");
        Files.createDirectory(bundleFiles);

        // Add the files from the Github repo to this workflow
        List<RepositoryContents> repoContents = githubService.getContents(githubInfo);
        Set<HashableAgent> authors = new HashSet<HashableAgent>();
        addFilesToBundle(bundle, githubInfo, repoContents, bundleFiles, authors);

        // Add combined authors
        manifest.setAuthoredBy(new ArrayList<>(authors));

        // Return the completed bundle
        return bundle;

    }

    /**
     * Save the Research Object bundle to disk
     * @param roBundle The bundle to be saved
     * @return The path to the research object
     * @throws IOException Any errors in saving
     */
    public Path saveToFile(Bundle roBundle) throws IOException {
        String fileName = "bundle-" + java.util.UUID.randomUUID() + ".zip";
        Path bundleLocation = Files.createFile(bundleStorage.resolve(fileName));
        Bundles.closeAndSaveBundle(roBundle, bundleLocation);
        return bundleLocation;
    }

    /**
     * Add files to this bundle from a list of Github repository contents
     * @param bundle The RO bundle to add files/directories to
     * @param githubInfo The information used to access the repository
     * @param repoContents The contents of the Github repository
     * @param path The path in the Research Object to add the files
     */
    private void addFilesToBundle(Bundle bundle, GithubDetails githubInfo,
                                  List<RepositoryContents> repoContents, Path path,
                                  Set<HashableAgent> authors) throws IOException {

        // Loop through repo contents and add them
        for (RepositoryContents repoContent : repoContents) {

            // Parse subdirectories if they exist
            if (repoContent.getType().equals(GitHubService.TYPE_DIR)) {

                // Get the contents of the subdirectory
                GithubDetails githubSubdir = new GithubDetails(githubInfo.getOwner(),
                        githubInfo.getRepoName(), githubInfo.getBranch(), repoContent.getPath());
                List<RepositoryContents> subdirectory = githubService.getContents(githubSubdir);

                // Create a new folder in the RO for it
                Path subdirPath = path.resolve(repoContent.getName());
                Files.createDirectory(subdirPath);

                // Add the files in the subdirectory to this new folder
                addFilesToBundle(bundle, githubInfo, subdirectory, subdirPath, authors);

                // Otherwise this is a file so add to the bundle
            } else if (repoContent.getType().equals(GitHubService.TYPE_FILE)) {

                try {
                    // Raw URI of the bundle
                    GithubDetails githubFile = new GithubDetails(githubInfo.getOwner(),
                            githubInfo.getRepoName(), githubInfo.getBranch(), repoContent.getPath());

                    // Where to store the new file in bundle
                    Path bundleFilePath = path.resolve(repoContent.getName());

                    // Get commits
                    List<RepositoryCommit> commitsOnFile = githubService.getCommits(githubFile);
                    String commitSha = commitsOnFile.get(0).getSha();

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
                    Set<HashableAgent> fileAuthors = new HashSet<>();
                    for (RepositoryCommit commit : commitsOnFile) {
                        User author = commit.getAuthor();
                        CommitUser commitAuthor = commit.getCommit().getAuthor();

                        // If there is author information for this commit in some form
                        if (author != null || commitAuthor != null) {
                            // Create a new agent and add as much detail as possible
                            HashableAgent newAgent = new HashableAgent();
                            if (author != null) {
                                newAgent.setUri(new URI(author.getHtmlUrl()));
                            }
                            if (commitAuthor != null) {
                                newAgent.setName(commitAuthor.getName());
                            }
                            fileAuthors.add(newAgent);
                        }
                    }
                    authors.addAll(fileAuthors);
                    aggregation.setAuthoredBy(new ArrayList<Agent>(fileAuthors));

                    // Set retrieved information for this file in the manifest
                    aggregation.setRetrievedFrom(rawURI);
                    aggregation.setRetrievedBy(appAgent);
                    aggregation.setRetrievedOn(aggregation.getCreatedOn());

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
