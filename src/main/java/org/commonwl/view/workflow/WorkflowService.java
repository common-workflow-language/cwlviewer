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

package org.commonwl.view.workflow;

import org.commonwl.view.cwl.CWLService;
import org.commonwl.view.cwl.CWLToolRunner;
import org.commonwl.view.cwl.CWLValidationException;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.commonwl.view.researchobject.ROBundleNotFoundException;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.client.RequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class WorkflowService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GitHubService githubService;
    private final CWLService cwlService;
    private final WorkflowRepository workflowRepository;
    private final ROBundleFactory ROBundleFactory;
    private final GraphVizService graphVizService;
    private final CWLToolRunner cwlToolRunner;
    private final int cacheDays;

    @Autowired
    public WorkflowService(GitHubService githubService,
                           CWLService cwlService,
                           WorkflowRepository workflowRepository,
                           ROBundleFactory ROBundleFactory,
                           GraphVizService graphVizService,
                           CWLToolRunner cwlToolRunner,
                           @Value("${cacheDays}") int cacheDays) {
        this.githubService = githubService;
        this.cwlService = cwlService;
        this.workflowRepository = workflowRepository;
        this.ROBundleFactory = ROBundleFactory;
        this.graphVizService = graphVizService;
        this.cwlToolRunner = cwlToolRunner;
        this.cacheDays = cacheDays;
    }

    /**
     * Gets a page of all workflows from the database
     * @param pageable The details of the page to be requested
     * @return The resulting page of the workflow entries
     */
    public Page<Workflow> getPageOfWorkflows(Pageable pageable) {
        return workflowRepository.findByCwltoolStatusOrderByRetrievedOnDesc(Workflow.Status.SUCCESS, pageable);
    }

    /**
     * Gets a page of all workflows from the database
     * @param searchString The string to search for
     * @param pageable The details of the page to be requested
     * @return The resulting page of the workflow entries
     */
    public Page<Workflow> searchPageOfWorkflows(String searchString, Pageable pageable) {
        return workflowRepository.findByCwltoolStatusAndLabelContainingOrDocContaining(Workflow.Status.SUCCESS, searchString, searchString, pageable);
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
     * Get a list of workflows from a directory in Github
     * @param githubInfo Github information for the workflow
     * @return The list of workflow names
     */
    public List<WorkflowOverview> getWorkflowsFromDirectory(GithubDetails githubInfo) throws IOException {
        List<WorkflowOverview> workflowsInDir = new ArrayList<>();
        for (RepositoryContents content : githubService.getContents(githubInfo)) {
            int eIndex = content.getName().lastIndexOf('.') + 1;
            if (eIndex > 0) {
                String extension = content.getName().substring(eIndex);
                if (extension.equals("cwl")) {
                    GithubDetails githubFile = new GithubDetails(githubInfo.getOwner(),
                            githubInfo.getRepoName(), githubInfo.getBranch(), content.getPath());
                    WorkflowOverview overview = cwlService.getWorkflowOverview(githubFile);
                    if (overview != null) {
                        workflowsInDir.add(overview);
                    }
                }
            }
        }
        return workflowsInDir;
    }

    /**
     * Get a workflow from the database, refreshing it if cache has expired
     * @param githubInfo Github information for the workflow
     * @return The workflow model associated with githubInfo
     */
    public Workflow getWorkflow(GithubDetails githubInfo) {
        // Check database for existing workflows from this repository
        Workflow workflow = workflowRepository.findByRetrievedFrom(githubInfo);

        // Slash in branch fix
        boolean slashesInPath = true;
        while (workflow == null && slashesInPath) {
            GithubDetails correctedForSlash = transferPathToBranch(githubInfo);
            if (correctedForSlash != null) {
                githubInfo = correctedForSlash;
                workflow = workflowRepository.findByRetrievedFrom(githubInfo);
            } else {
                slashesInPath = false;
            }
        }

        // Cache update
        if (workflow != null) {
            // Delete the existing workflow if the cache has expired
            if (cacheExpired(workflow)) {
                removeWorkflow(workflow);

                // Add the new workflow if it exists
                try {
                    workflow = createWorkflow(workflow.getRetrievedFrom());
                } catch (Exception e) {
                    // Add back the old workflow if it is broken now
                    logger.error("Could not parse updated workflow " + workflow.getID());
                    workflowRepository.save(workflow);
                }
            }
        }

        return workflow;
    }

    /**
     * Get the RO bundle for a Workflow, triggering re-download if it does not exist
     * @param githubDetails The origin details of the workflow
     * @return The file containing the RO bundle
     * @throws ROBundleNotFoundException If the RO bundle was not found
     */
    public File getROBundle(GithubDetails githubDetails) throws ROBundleNotFoundException {
        // Get workflow from database
        Workflow workflow = getWorkflow(githubDetails);

        // If workflow does not exist or the bundle doesn't yet
        if (workflow == null || workflow.getRoBundlePath() == null) {
            throw new ROBundleNotFoundException();
        }

        // 404 error with retry if the file on disk does not exist
        File bundleDownload = new File(workflow.getRoBundlePath());
        if (!bundleDownload.exists()) {
            // Clear current RO bundle link and create a new one (async)
            workflow.setRoBundlePath(null);
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
     * @throws IOException Github errors
     * @throws CWLValidationException cwltool errors
     */
    public Workflow createWorkflow(GithubDetails githubInfo)
            throws IOException, CWLValidationException {

        // Construct basic workflow model from cwl
        String latestCommit = null;
        while (latestCommit == null) {
            try {
                latestCommit = githubService.getCommitSha(githubInfo);
            } catch (RequestException ex) {
                if (ex.getStatus() == 404) {
                    // Slashes in branch fix
                    // Commits were not found. This can occur if the branch has a /
                    GithubDetails correctedForSlash = transferPathToBranch(githubInfo);
                    if (correctedForSlash != null) {
                        githubInfo = correctedForSlash;
                    } else {
                        throw ex;
                    }
                } else {
                    throw ex;
                }
            }
        }
        Workflow workflowModel = cwlService.parseWorkflowNative(githubInfo, latestCommit);

        // Set origin details
        workflowModel.setRetrievedOn(new Date());
        workflowModel.setRetrievedFrom(githubInfo);
        workflowModel.setLastCommit(latestCommit);

        // ASYNC OPERATIONS
        // Parse with cwltool and update model
        try {
            cwlToolRunner.updateModelWithCwltool(githubInfo, latestCommit,
                    workflowModel.getPackedWorkflowID());
        } catch (Exception e) {
            logger.error("Could not update workflow with cwltool", e);
        }

        // Create a new research object bundle for the workflow
        generateROBundle(workflowModel);
        // END ASYNC

        // Save to database
        workflowRepository.save(workflowModel);

        // Return this model to be displayed
        return workflowModel;

    }

    /**
     * Update a workflow by running cwltool
     * @param workflowModel The old workflow model, possibly incomplete
     * @param githubInfo The details of the github repository
     */
    public void updateWorkflow(Workflow workflowModel, GithubDetails githubInfo) {
        workflowModel.setCwltoolStatus(Workflow.Status.RUNNING);
        workflowRepository.save(workflowModel);
        try {
            String latestCommit = githubService.getCommitSha(githubInfo);
            cwlToolRunner.updateModelWithCwltool(githubInfo, latestCommit,
                    workflowModel.getPackedWorkflowID());
        } catch (Exception e) {
            logger.error("Could not update workflow with cwltool", e);
        }
    }

    /**
     * Generates the RO bundle for a Workflow and adds it to the model
     * @param workflow The workflow model to create a Research Object for
     */
    private void generateROBundle(Workflow workflow) {
        try {
            ROBundleFactory.workflowROFromGithub(workflow.getRetrievedFrom());
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
        File roBundle = new File(workflow.getRoBundlePath());
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

    /**
     * Transfers part of the path to the branch to fix / in branch names
     * @param githubInfo The current Github info possibly with
     *                   part of the branch name in the path
     * @return A potentially corrected set of Github details,
     *         or null if there are no slashes in the path
     */
    private GithubDetails transferPathToBranch(GithubDetails githubInfo) {
        String path = githubInfo.getPath();
        String branch = githubInfo.getBranch();

        int firstSlash = path.indexOf("/");
        if (firstSlash > 0) {
            branch += "/" + path.substring(0, firstSlash);
            path = path.substring(firstSlash + 1);
            return new GithubDetails(githubInfo.getOwner(),
                    githubInfo.getRepoName(), branch, path);
        } else {
            return null;
        }
    }
}
