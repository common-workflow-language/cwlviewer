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

package org.commonwl.view.cwl;

import org.commonwl.view.git.GitDetails;
import org.commonwl.view.git.GitService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.commonwl.view.workflow.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

import static org.commonwl.view.workflow.WorkflowService.transferPathToBranch;

/**
 * Replace existing workflow with the one given by cwltool
 */
@Component
@EnableAsync
public class CWLToolRunner {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final WorkflowRepository workflowRepository;
    private final QueuedWorkflowRepository queuedWorkflowRepository;
    private final CWLService cwlService;
    private final GitService gitService;
    private final ROBundleFactory roBundleFactory;
    private final String cwlToolVersion;

    @Autowired
    public CWLToolRunner(WorkflowRepository workflowRepository,
                         QueuedWorkflowRepository queuedWorkflowRepository,
                         CWLService cwlService,
                         GitService gitService,
                         CWLTool cwlTool,
                         ROBundleFactory roBundleFactory) {
        this.workflowRepository = workflowRepository;
        this.queuedWorkflowRepository = queuedWorkflowRepository;
        this.cwlService = cwlService;
        this.gitService = gitService;
        this.cwlToolVersion = cwlTool.getVersion();
        this.roBundleFactory = roBundleFactory;
    }

    @Async
    public void cloneRepoAndParse(QueuedWorkflow queuedWorkflow) {
        GitDetails gitInfo = queuedWorkflow.getTempRepresentation().getRetrievedFrom();
        try {
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
            Workflow nativeModel = cwlService.parseWorkflowNative(workflowFile);

            // Set origin details
            nativeModel.setRetrievedOn(new Date());
            nativeModel.setRetrievedFrom(gitInfo);
            nativeModel.setLastCommit(latestCommit);

            // Save the queued workflow to database
            queuedWorkflow.setTempRepresentation(nativeModel);
            queuedWorkflowRepository.save(queuedWorkflow);

            // Parse with cwltool and update model
            try {
                createWorkflowFromQueued(queuedWorkflow, workflowFile);
            } catch (Exception e) {
                logger.error("Could not update workflow with cwltool", e);
            }

        } catch (GitAPIException ex) {
            queuedWorkflow.setMessage("Git error: " + ex.getMessage());
            queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
        } catch (IOException ex) {
            queuedWorkflow.setMessage("An error occurred accessing files checked " +
                    "out from the Git repository");
            queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
        } catch(WorkflowNotFoundException ex) {
            queuedWorkflow.setMessage("The given path \"" + gitInfo.getPath() +
                    "\" did not resolve to a location within the repository");
            queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
        } finally {
            queuedWorkflowRepository.save(queuedWorkflow);
        }

    }

    @Async
    public void createWorkflowFromQueued(QueuedWorkflow queuedWorkflow, File workflowFile)
            throws IOException, InterruptedException {

        queuedWorkflow.setCwltoolStatus(CWLToolStatus.RUNNING);
        queuedWorkflowRepository.save(queuedWorkflow);

        Workflow tempWorkflow = queuedWorkflow.getTempRepresentation();

        // Parse using cwltool and replace in database
        try {
            Workflow newWorkflow = cwlService.parseWorkflowWithCwltool(
                    tempWorkflow,
                    workflowFile);

            // Success
            newWorkflow.setRetrievedFrom(tempWorkflow.getRetrievedFrom());
            newWorkflow.setRetrievedOn(new Date());
            newWorkflow.setLastCommit(tempWorkflow.getLastCommit());
            newWorkflow.setPackedWorkflowID(tempWorkflow.getPackedWorkflowID());
            newWorkflow.setCwltoolVersion(cwlToolVersion);

            workflowRepository.save(newWorkflow);

            // Generate RO bundle
            roBundleFactory.createWorkflowRO(newWorkflow);

            // Mark success on queue
            queuedWorkflow.setCwltoolStatus(CWLToolStatus.SUCCESS);

        } catch (CWLValidationException ex) {
            logger.error(ex.getMessage(), ex);
            queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
            queuedWorkflow.setMessage(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error", ex);
            queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
            queuedWorkflow.setMessage("Whoops! Cwltool ran successfully, but an unexpected " +
                    "error occurred in CWLViewer!\n" +
                    "Help us by reporting it on Gitter or a Github issue\n");
        } finally {
            queuedWorkflowRepository.save(queuedWorkflow);
        }

    }

}
