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

package org.commonwl.viewer.workflow;

import org.commonwl.viewer.cwl.CWLService;
import org.commonwl.viewer.github.GitHubService;
import org.commonwl.viewer.github.GithubDetails;
import org.commonwl.viewer.graphviz.GraphVizService;
import org.commonwl.viewer.researchobject.ROBundleFactory;
import org.commonwl.viewer.researchobject.ROBundleNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

@Service
public class WorkflowService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GitHubService githubService;
    private final CWLService cwlService;
    private final WorkflowRepository workflowRepository;
    private final ROBundleFactory ROBundleFactory;
    private final GraphVizService graphVizService;
    private final int cacheDays;

    @Autowired
    public WorkflowService(GitHubService githubService,
                           CWLService cwlService,
                           WorkflowRepository workflowRepository,
                           ROBundleFactory ROBundleFactory,
                           GraphVizService graphVizService,
                           @Value("${cacheDays}") int cacheDays) {
        this.githubService = githubService;
        this.cwlService = cwlService;
        this.workflowRepository = workflowRepository;
        this.ROBundleFactory = ROBundleFactory;
        this.graphVizService = graphVizService;
        this.cacheDays = cacheDays;
    }

    /**
     * Gets a page of all workflows from the database
     * @param pageable The details of the page to be requested
     * @return The resulting page of the workflow entries
     */
    public Page<Workflow> getPageOfWorkflows(Pageable pageable) {
        return workflowRepository.findAllByOrderByRetrievedOnDesc(pageable);
    }

    /**
     * Get a workflow from the database by its ID
     * @param id The ID of the workflow
     * @return The model for the workflow
     */
    public Workflow getWorkflow(String id) {
        return workflowRepository.findOne(id);
    }

    /**
     * Get a workflow from the database, refreshing it if cache has expired
     * @param githubInfo Github information for the workflow
     * @return The workflow model associated with githubInfo
     */
    public Workflow getWorkflow(GithubDetails githubInfo) {
        // Check database for existing workflows from this repository
        Workflow workflow = workflowRepository.findByRetrievedFrom(githubInfo);

        // Cache update
        if (workflow != null) {
            // Delete the existing workflow if the cache has expired
            if (cacheExpired(workflow)) {
                // Update by trying to add a new workflow
                Workflow newWorkflow = createWorkflow(workflow.getRetrievedFrom());

                // Only replace workflow if it could be successfully parsed
                if (newWorkflow == null) {
                    logger.error("Could not parse updated workflow " + workflow.getID());
                } else {
                    // Delete the existing workflow
                    removeWorkflow(workflow);

                    // Save new workflow
                    workflowRepository.save(newWorkflow);
                    workflow = newWorkflow;
                }
            }
        }

        return workflow;
    }

    /**
     * Get the RO bundle for a Workflow, triggering re-download if it does not exist
     * @param id The ID of the workflow
     * @return The file containing the RO bundle
     * @throws ROBundleNotFoundException If the RO bundle was not found
     */
    public File getROBundle(String id) throws ROBundleNotFoundException {
        // Get workflow from database
        Workflow workflow = getWorkflow(id);

        // If workflow does not exist or the bundle doesn't yet
        if (workflow == null || workflow.getRoBundle() == null) {
            throw new ROBundleNotFoundException();
        }

        // 404 error with retry if the file on disk does not exist
        File bundleDownload = new File(workflow.getRoBundle());
        if (!bundleDownload.exists()) {
            // Clear current RO bundle link and create a new one (async)
            workflow.setRoBundle(null);
            workflowRepository.save(workflow);
            generateROBundle(workflow);
            throw new ROBundleNotFoundException();
        }

        return bundleDownload;
    }

    /**
     * Builds a new workflow from Github
     * @param githubInfo Github information for the workflow
     * @return The constructed model for the Workflow
     */
    public Workflow createWorkflow(GithubDetails githubInfo) {
        try {
            // Get the sha hash from a branch reference
            String latestCommit = githubService.getCommitSha(githubInfo);

            // Get the workflow model using the cwl service
            Workflow workflowModel = cwlService.parseWorkflow(githubInfo, latestCommit);

            if (workflowModel != null) {
                // Set origin details
                workflowModel.setRetrievedOn(new Date());
                workflowModel.setRetrievedFrom(githubInfo);
                workflowModel.setLastCommit(latestCommit);

                // Create a new research object bundle for the workflow
                // This is Async so cannot just call constructor, needs intermediate as per Spring framework
                generateROBundle(workflowModel);

                // Save to database
                workflowRepository.save(workflowModel);

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
    private void generateROBundle(Workflow workflow) {
        try {
            ROBundleFactory.workflowROFromGithub(githubService, workflow.getRetrievedFrom());
        } catch (Exception ex) {
            logger.error("Error creating RO Bundle", ex);
        }
    }

    /**
     * Removes a workflow and its research object bundle
     * @param workflow The workflow to be deleted
     */
    private void removeWorkflow(Workflow workflow) {
        // Delete the Research Object Bundle from disk
        File roBundle = new File(workflow.getRoBundle());
        if (roBundle.delete()) {
            logger.debug("Deleted Research Object Bundle");
        } else {
            logger.debug("Failed to delete Research Object Bundle");
        }

        // Delete cached graphviz images if they exist
        graphVizService.deleteCache(workflow.getID());

        // Remove the workflow from the database
        workflowRepository.delete(workflow);
    }

    /**
     * Check for cache expiration based on time and commit sha
     * @param workflow The cached workflow model
     * @return Whether or not there are new commits
     */
    private boolean cacheExpired(Workflow workflow) {
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
