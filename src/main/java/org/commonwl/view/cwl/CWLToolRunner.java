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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import org.apache.jena.query.QueryException;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.git.GitLicenseException;
import org.commonwl.view.git.GitSemaphore;
import org.commonwl.view.git.GitService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.commonwl.view.util.FileUtils;
import org.commonwl.view.workflow.QueuedWorkflow;
import org.commonwl.view.workflow.QueuedWorkflowRepository;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

/** Replace existing workflow with the one given by cwltool */
@Component
@EnableAsync
public class CWLToolRunner {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final WorkflowRepository workflowRepository;
  private final QueuedWorkflowRepository queuedWorkflowRepository;
  private final CWLService cwlService;
  private final ROBundleFactory roBundleFactory;
  private final String cwlToolVersion;
  private final GitSemaphore gitSemaphore;
  private final GitService gitService;

  @Autowired
  public CWLToolRunner(
      WorkflowRepository workflowRepository,
      QueuedWorkflowRepository queuedWorkflowRepository,
      CWLService cwlService,
      CWLTool cwlTool,
      ROBundleFactory roBundleFactory,
      GitSemaphore gitSemaphore,
      GitService gitService) {
    this.workflowRepository = workflowRepository;
    this.queuedWorkflowRepository = queuedWorkflowRepository;
    this.cwlService = cwlService;
    this.cwlToolVersion = cwlTool.getVersion();
    this.roBundleFactory = roBundleFactory;
    this.gitSemaphore = gitSemaphore;
    this.gitService = gitService;
  }

  @Async
  public void createWorkflowFromQueued(QueuedWorkflow queuedWorkflow)
      throws IOException, InterruptedException {

    Workflow tempWorkflow = queuedWorkflow.getTempRepresentation();
    GitDetails gitInfo = tempWorkflow.getRetrievedFrom();
    final String repoUrl = gitInfo.getRepoUrl();
    // Parse using cwltool and replace in database
    Git repo = null;
    try {
      boolean safeToAccess = gitSemaphore.acquire(repoUrl);
      repo = gitService.getRepository(gitInfo, safeToAccess);
      Path localPath = repo.getRepository().getWorkTree().toPath();
      Path workflowFile = localPath.resolve(gitInfo.getPath()).normalize().toAbsolutePath();
      Workflow newWorkflow =
          cwlService.parseWorkflowWithCwltool(tempWorkflow, workflowFile, localPath);

      // Success
      newWorkflow.setRetrievedFrom(tempWorkflow.getRetrievedFrom());
      newWorkflow.setRetrievedOn(new Date());
      newWorkflow.setLastCommit(tempWorkflow.getLastCommit());
      newWorkflow.setCwltoolVersion(cwlToolVersion);

      workflowRepository.save(newWorkflow);

      // Generate RO bundle
      roBundleFactory.createWorkflowRO(newWorkflow);

      // Mark success on queue
      queuedWorkflow.setCwltoolStatus(CWLToolStatus.SUCCESS);

    } catch (QueryException ex) {
      logger.error("Jena query exception for workflow " + queuedWorkflow.getId(), ex);
      queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
      queuedWorkflow.setMessage("An error occurred when executing a query on the SPARQL store");
      FileUtils.deleteGitRepository(repo);
    } catch (CWLValidationException | GitLicenseException ex) {
      String message = ex.getMessage();
      logger.error(
          "Workflow " + queuedWorkflow.getId() + " from " + gitInfo.toSummary() + " : " + message,
          ex);
      queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
      queuedWorkflow.setMessage(message);
      FileUtils.deleteGitRepository(repo);
    } catch (TransportException ex) {
      String message = ex.getMessage();
      logger.error(
          "Workflow retrieval error while processing "
              + queuedWorkflow.getId()
              + " from "
              + gitInfo.toSummary()
              + " : "
              + message,
          ex);
      queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
      if (message.contains(
          "Authentication is required but no CredentialsProvider has been registered")) {
        queuedWorkflow.setMessage(
            "Unable to retrieve the Git repository: it may be private, misnamed, or removed. "
                + message);
      } else {
        queuedWorkflow.setMessage(message);
      }
      FileUtils.deleteGitRepository(repo);
    } catch (MissingObjectException ex) {
      String message = ex.getMessage();
      logger.error(
          "Workflow retrieval error while processing "
              + queuedWorkflow.getId()
              + " from "
              + gitInfo.toSummary()
              + " : "
              + message,
          ex);
      queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
      queuedWorkflow.setMessage("Unable to retrieve a needed Git object: " + message);
      FileUtils.deleteGitRepository(repo);
    } catch (Exception ex) {
      logger.error(
          "Unexpected error processing workflow "
              + queuedWorkflow.getId()
              + " from "
              + gitInfo.toSummary()
              + " : "
              + ex.getMessage(),
          ex);
      queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
      queuedWorkflow.setMessage(
          "Whoops! Cwltool ran successfully, but an unexpected "
              + "error occurred in CWLViewer!\n"
              + ex.getMessage()
              + "\nHelp us by reporting it at https://github.com/common-workflow-language/cwlviewer/issues/new/choose\n");
      FileUtils.deleteGitRepository(repo);
    } finally {
      gitSemaphore.release(repoUrl);
      FileUtils.deleteTemporaryGitRepository(repo);
      queuedWorkflowRepository.save(queuedWorkflow);
    }
  }
}
