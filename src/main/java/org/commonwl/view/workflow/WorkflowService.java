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
import org.commonwl.view.cwl.CWLToolStatus;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.git.GitService;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.commonwl.view.researchobject.ROBundleNotFoundException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;

@Service
public class WorkflowService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GitService gitService;
    private final CWLService cwlService;
    private final WorkflowRepository workflowRepository;
    private final QueuedWorkflowRepository queuedWorkflowRepository;
    private final ROBundleFactory ROBundleFactory;
    private final GraphVizService graphVizService;
    private final CWLToolRunner cwlToolRunner;
    private final int cacheDays;

    @Autowired
    public WorkflowService(GitService gitService,
                           CWLService cwlService,
                           WorkflowRepository workflowRepository,
                           QueuedWorkflowRepository queuedWorkflowRepository,
                           ROBundleFactory ROBundleFactory,
                           GraphVizService graphVizService,
                           CWLToolRunner cwlToolRunner,
                           @Value("${cacheDays}") int cacheDays) {
        this.gitService = gitService;
        this.cwlService = cwlService;
        this.workflowRepository = workflowRepository;
        this.queuedWorkflowRepository = queuedWorkflowRepository;
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
        return workflowRepository.findAllByOrderByRetrievedOnDesc(pageable);
    }

    /**
     * Gets a page of all workflows from the database
     * @param searchString The string to search for
     * @param pageable The details of the page to be requested
     * @return The resulting page of the workflow entries
     */
    public Page<Workflow> searchPageOfWorkflows(String searchString, Pageable pageable) {
        return workflowRepository.findByLabelContainingOrDocContainingIgnoreCase(searchString, searchString, pageable);
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
     * Get a queued workflow from the database by its ID
     * @param id The ID of the queued workflow
     * @return The model for the queued workflow
     */
    public QueuedWorkflow getQueuedWorkflow(String id) {
        return queuedWorkflowRepository.findOne(id);
    }

    /**
     * Get a queued workflow from the database
     * @param githubInfo Github information for the workflow
     * @return The queued workflow model
     */
    public QueuedWorkflow getQueuedWorkflow(GitDetails githubInfo) {
        QueuedWorkflow queued = queuedWorkflowRepository.findByRetrievedFrom(githubInfo);

        // Slash in branch fix
        boolean slashesInPath = true;
        while (queued == null && slashesInPath) {
            GitDetails correctedForSlash = transferPathToBranch(githubInfo);
            if (correctedForSlash != null) {
                githubInfo = correctedForSlash;
                queued = queuedWorkflowRepository.findByRetrievedFrom(githubInfo);
            } else {
                slashesInPath = false;
            }
        }

        return queued;
    }

    /**
     * Get a workflow from the database, refreshing it if cache has expired
     * @param gitInfo Git information for the workflow
     * @return The workflow model associated with gitInfo
     */
    public Workflow getWorkflow(GitDetails gitInfo) {
        // Check database for existing workflows from this repository
        Workflow workflow = workflowRepository.findByRetrievedFrom(gitInfo);

        // Slash in branch fix
        boolean slashesInPath = true;
        while (workflow == null && slashesInPath) {
            GitDetails correctedForSlash = transferPathToBranch(gitInfo);
            if (correctedForSlash != null) {
                gitInfo = correctedForSlash;
                workflow = workflowRepository.findByRetrievedFrom(gitInfo);
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
                    createQueuedWorkflow(workflow.getRetrievedFrom());
                    workflow = null;
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
     * @param gitDetails The origin details of the workflow
     * @return The file containing the RO bundle
     * @throws ROBundleNotFoundException If the RO bundle was not found
     */
    public File getROBundle(GitDetails gitDetails) throws ROBundleNotFoundException {
        // Get workflow from database
        Workflow workflow = getWorkflow(gitDetails);

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
     * Builds a new queued workflow from Git
     * @param gitInfo Git information for the workflow
     * @return A queued workflow model
     * @throws GitAPIException Git errors
     * @throws WorkflowNotFoundException Workflow was not found within the repository
     * @throws IOException Other file handling exceptions
     */
    public QueuedWorkflow createQueuedWorkflow(GitDetails gitInfo)
            throws GitAPIException, WorkflowNotFoundException, IOException {

        // Clone repository to temporary folder
        Git repo = null;
        while (repo == null) {
            try {
                repo = gitService.getRepository(gitInfo);
            } catch (RefNotFoundException ex) {
                // Attempt slashes in branch fix
                GitDetails correctedForSlash = transferPathToBranch(gitInfo);
                if (correctedForSlash != null) {
                    gitInfo = correctedForSlash;
                } else {
                    throw ex;
                }
            }
        }
        File localPath = repo.getRepository().getWorkTree();
        String latestCommit = gitService.getCurrentCommitID(repo);

        Path pathToWorkflowFile = localPath.toPath().resolve(gitInfo.getPath()).normalize().toAbsolutePath();
        // Prevent path traversal attacks
        if (!pathToWorkflowFile.startsWith(localPath.toPath().normalize().toAbsolutePath())) {
            throw new WorkflowNotFoundException();
        }

        File workflowFile = new File(pathToWorkflowFile.toString());
        Workflow basicModel = cwlService.parseWorkflowNative(workflowFile);

        // Set origin details
        basicModel.setRetrievedOn(new Date());
        basicModel.setRetrievedFrom(gitInfo);
        basicModel.setLastCommit(latestCommit);

        // Save the queued workflow to database
        QueuedWorkflow queuedWorkflow = new QueuedWorkflow();
        queuedWorkflow.setTempRepresentation(basicModel);
        queuedWorkflowRepository.save(queuedWorkflow);

        // ASYNC OPERATIONS
        // Parse with cwltool and update model
        try {
            cwlToolRunner.createWorkflowFromQueued(queuedWorkflow, workflowFile);
        } catch (Exception e) {
            logger.error("Could not update workflow with cwltool", e);
        }

        // Return this model to be displayed
        return queuedWorkflow;
    }

    /**
     * Retry the running of cwltool to create a new workflow
     * @param queuedWorkflow The workflow to use to update
     */
    public void retryCwltool(QueuedWorkflow queuedWorkflow) {
        queuedWorkflow.setMessage(null);
        queuedWorkflow.setCwltoolStatus(CWLToolStatus.RUNNING);
        queuedWorkflowRepository.save(queuedWorkflow);
        try {
            GitDetails gitDetails = queuedWorkflow.getTempRepresentation().getRetrievedFrom();
            Git repo = gitService.getRepository(gitDetails);
            File localPath = repo.getRepository().getWorkTree();
            Path pathToWorkflowFile = localPath.toPath().resolve(gitDetails.getPath()).normalize().toAbsolutePath();
            cwlToolRunner.createWorkflowFromQueued(queuedWorkflow, new File(pathToWorkflowFile.toString()));
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
            ROBundleFactory.createWorkflowRO(workflow);
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
        if (workflow.getRoBundlePath() != null) {
            File roBundle = new File(workflow.getRoBundlePath());
            if (roBundle.delete()) {
                logger.debug("Deleted Research Object Bundle");
            } else {
                logger.debug("Failed to delete Research Object Bundle");
            }
        }

        // Delete cached graphviz images if they exist
        graphVizService.deleteCache(workflow.getID());

        // Remove the workflow from the database
        workflowRepository.delete(workflow);

        // Remove any queued repositories pointing to the workflow
        queuedWorkflowRepository.deleteByRetrievedFrom(workflow.getRetrievedFrom());
    }

    /**
     * Check for cache expiration based on time and commit sha
     * @param workflow The cached workflow model
     * @return Whether or not there are new commits
     */
    private boolean cacheExpired(Workflow workflow) {
        // If this is a branch and not added by commit ID
        if (!workflow.getRetrievedFrom().getBranch().equals(workflow.getLastCommit())) {
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
                    logger.info("Time has expired for caching, checking commits...");
                    Git repo = gitService.getRepository(workflow.getRetrievedFrom());
                    String currentHead = gitService.getCurrentCommitID(repo);
                    logger.info("Current: " + workflow.getLastCommit() + ", HEAD: " + currentHead);

                    // Reset date in database if there are still no changes
                    boolean expired = !workflow.getLastCommit().equals(currentHead);
                    if (!expired) {
                        workflow.setRetrievedOn(new Date());
                        workflowRepository.save(workflow);
                    }

                    // Return whether the cache has expired
                    return expired;
                }
            } catch(Exception ex){
                // Default to no expiry if there was an API error
                logger.error("API Error when checking for latest commit ID for caching", ex);
            }
        }
        return false;
    }

    /**
     * Transfers part of the path to the branch to fix / in branch names
     * @param githubInfo The current Github info possibly with
     *                   part of the branch name in the path
     * @return A potentially corrected set of Github details,
     *         or null if there are no slashes in the path
     */
    private GitDetails transferPathToBranch(GitDetails githubInfo) {
        String path = githubInfo.getPath();
        String branch = githubInfo.getBranch();

        int firstSlash = path.indexOf("/");
        if (firstSlash > 0) {
            branch += "/" + path.substring(0, firstSlash);
            path = path.substring(firstSlash + 1);
            return new GitDetails(githubInfo.getRepoUrl(), branch,
                    path);
        } else {
            return null;
        }
    }
}
