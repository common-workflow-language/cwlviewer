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

import org.apache.commons.io.FilenameUtils;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Class for the purpose of a Spring Framework Async method
 * being in a different class to where it is called
 *
 * This allows the proxy to kick in and run it asynchronously
 */
@Component
@EnableAsync
public class ROBundleFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String applicationName;
    private final String applicationURL;
    private final int singleFileSizeLimit;
    private final Path storageLocation;
    private final WorkflowRepository workflowRepository;
    private final GitHubService githubService;

    @Autowired
    public ROBundleFactory(@Value("${applicationName}") String applicationName,
                           @Value("${applicationURL}") String applicationURL,
                           @Value("${graphvizStorage}") Path graphvizStorage,
                           @Value("${singleFileSizeLimit}") int singleFileSizeLimit,
                           GitHubService gitHubService,
                           WorkflowRepository workflowRepository) {
        this.applicationName = applicationName;
        this.applicationURL = applicationURL;
        this.storageLocation = graphvizStorage;
        this.workflowRepository = workflowRepository;
        this.githubService = gitHubService;
        this.singleFileSizeLimit = singleFileSizeLimit;
    }

    /**
     * Creates a new Workflow Research Object Bundle from a Github URL
     * and saves it to a file
     * @param githubInfo Details of the Github repository
     * @throws IOException Any API errors which may have occurred
     */
    @Async
    public void workflowROFromGithub(GithubDetails githubInfo)
            throws IOException, InterruptedException {
        logger.info("Creating Research Object Bundle");

        // Get the whole containing folder, not just the workflow itself
        GithubDetails roDetails = new GithubDetails(githubInfo.getOwner(), githubInfo.getRepoName(),
                githubInfo.getBranch(), FilenameUtils.getPath(githubInfo.getPath()));

        // Create a new Research Object Bundle with Github contents
        ROBundle bundle = new ROBundle(githubService, roDetails,
                applicationName, applicationURL, singleFileSizeLimit);

        // Save the bundle to the storage location in properties
        Path bundleLocation = bundle.saveToFile(storageLocation);

        // Add the path to the bundle to the bundle
        Workflow workflow = workflowRepository.findByRetrievedFrom(githubInfo);

        // Chance that this thread could be created before workflow model is saved
        int attempts = 5;
        while (attempts > 0 && workflow == null) {
            // Delay this thread by 0.5s and try again until success or too many attempts
            Thread.sleep(1000L);
            workflow = workflowRepository.findByRetrievedFrom(githubInfo);
            attempts--;
        }

        if (workflow == null) {
            // If workflow is still null we can't find the workflow model
            logger.error("Workflow model could not be found when adding RO Bundle path");
        } else {
            // Add RO Bundle to associated workflow model
            workflow.setRoBundle(bundleLocation.toString());
            workflowRepository.save(workflow);
            logger.info("Finished saving Research Object Bundle");
        }
    }
}