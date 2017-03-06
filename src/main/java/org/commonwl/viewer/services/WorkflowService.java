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

package org.commonwl.viewer.services;

import org.commonwl.viewer.domain.CWLCollection;
import org.commonwl.viewer.domain.GithubDetails;
import org.commonwl.viewer.domain.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

@Service
public class WorkflowService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GitHubService githubService;
    private final WorkflowRepository workflowRepository;
    private final ROBundleFactory ROBundleFactory;
    private final int cacheDays;
    private final String graphvizStorage;
    private final int singleFileSizeLimit;
    private final int totalFileSizeLimit;

    @Autowired
    public WorkflowService(GitHubService githubService,
                           WorkflowRepository workflowRepository,
                           ROBundleFactory ROBundleFactory,
                           @Value("${cacheDays}") int cacheDays,
                           @Value("${graphvizStorage}") String graphvizStorage,
                           @Value("${singleFileSizeLimit}") int singleFileSizeLimit,
                           @Value("${totalFileSizeLimit}") int totalFileSizeLimit) {
        this.githubService = githubService;
        this.workflowRepository = workflowRepository;
        this.ROBundleFactory = ROBundleFactory;
        this.cacheDays = cacheDays;
        this.graphvizStorage = graphvizStorage;
        this.singleFileSizeLimit = singleFileSizeLimit;
        this.totalFileSizeLimit = totalFileSizeLimit;
    }

    /**
     * Builds a new workflow from cwl files fetched from Github
     * @param githubInfo Github information for the workflow
     * @return The constructed model for the Workflow
     */
    public Workflow newWorkflowFromGithub(GithubDetails githubInfo) {

        try {
            // Get the sha hash from a branch reference
            String latestCommit = githubService.getCommitSha(githubInfo);

            // Set up CWL utility to collect the documents
            CWLCollection cwlFiles = new CWLCollection(githubService, githubInfo, latestCommit,
                    singleFileSizeLimit, totalFileSizeLimit);

            // Get the workflow model
            Workflow workflowModel = cwlFiles.getWorkflow();
            if (workflowModel != null) {
                // Set origin details
                workflowModel.setRetrievedOn(new Date());
                workflowModel.setRetrievedFrom(githubInfo);
                workflowModel.setLastCommit(latestCommit);

                // Create a new research object bundle for the workflow
                // This is Async so cannot just call constructor, needs intermediate as per Spring framework
                generateROBundle(workflowModel);

                // Return this model to be displayed
                return workflowModel;

            } else {
                logger.error("No workflow could be found");
            }
        } catch (Exception ex) {
            logger.error("Error creating workflow", ex);
        }

        return null;
    }

    /**
     * Generates the RO bundle for a Workflow and adds it to the model
     * @param workflow The workflow model to create a Research Object for
     */
    public void generateROBundle(Workflow workflow) {
        try {
            ROBundleFactory.workflowROFromGithub(githubService, workflow.getRetrievedFrom(), workflow.getLastCommit());
        } catch (Exception ex) {
            logger.error("Error creating RO Bundle", ex);
        }
    }

    /**
     * Removes a workflow and its research object bundle
     * @param workflow The workflow to be deleted
     */
    public void removeWorkflow(Workflow workflow) {
        // Delete the Research Object Bundle from disk
        File roBundle = new File(workflow.getRoBundle());
        if (roBundle.delete()) {
            logger.debug("Deleted Research Object Bundle");
        } else {
            logger.debug("Failed to delete Research Object Bundle");
        }

        // Delete cached graphviz images if they exist
        File graphvizSvg = new File(graphvizStorage + "/" + workflow.getID() + ".svg");
        graphvizSvg.delete();
        File graphvizPng = new File(graphvizStorage + "/" + workflow.getID() + ".png");
        graphvizPng.delete();

        // Remove the workflow from the database
        workflowRepository.delete(workflow);
    }

    /**
     * Check for cache expiration based on time and commit sha
     * @param workflow The cached workflow model
     * @return Whether or not there are new commits
     */
    public boolean cacheExpired(Workflow workflow) {
        try {
            // Calculate expiration
            Calendar expireCal = Calendar.getInstance();
            expireCal.setTime(workflow.getRetrievedOn());
            expireCal.add(Calendar.DATE, cacheDays);
            Date expirationDate = expireCal.getTime();

            // Check cached retrievedOn date
            if (expirationDate.before(new Date())) {
                // Cache expiry time has elapsed
                // Check current head of the branch with the cached head
                logger.debug("Time has expired for caching, checking commits...");
                String currentHead = githubService.getCommitSha(workflow.getRetrievedFrom());
                logger.debug("Current: " + workflow.getLastCommit() + ", HEAD: " + currentHead);

                // Reset date in database if there are still no changes
                boolean expired = !workflow.getLastCommit().equals(currentHead);
                if (!expired) {
                    workflow.setRetrievedOn(new Date());
                    workflowRepository.save(workflow);
                }

                // Return whether the cache has expired
                return expired;
            } else {
                // Cache expiry time has not elapsed yet
                return false;
            }
        } catch (Exception ex) {
            // Default to no expiry if there was an API error
            return false;
        }
    }
}
