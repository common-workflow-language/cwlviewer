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

import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Replace existing workflow with the one given by cwltool
 */
@Component
@EnableAsync
public class CWLToolRunner {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final WorkflowRepository workflowRepository;
    private final CWLService cwlService;
    private final GraphVizService graphVizService;
    private final String cwlToolVersion;

    @Autowired
    public CWLToolRunner(WorkflowRepository workflowRepository,
                         CWLService cwlService,
                         CWLTool cwlTool,
                         GraphVizService graphVizService) {
        this.workflowRepository = workflowRepository;
        this.cwlService = cwlService;
        this.graphVizService = graphVizService;
        this.cwlToolVersion = cwlTool.getVersion();
    }

    @Async
    public void updateModelWithCwltool(GithubDetails githubInfo,
                                       String latestCommit,
                                       String packedWorkflowID)
            throws IOException, InterruptedException {

        Workflow workflow = workflowRepository.findByRetrievedFrom(githubInfo);

        // Chance that this thread could be done before workflow model is saved
        int attempts = 5;
        while (attempts > 0 && workflow == null) {
            // Delay this thread by 0.5s and try again until success or too many attempts
            Thread.sleep(1000L);
            workflow = workflowRepository.findByRetrievedFrom(githubInfo);
            attempts--;
        }

        // Parse using cwltool and replace in database
        try {

            Workflow newWorkflow = cwlService.parseWorkflowWithCwltool(
                    githubInfo, latestCommit, packedWorkflowID);

            workflowRepository.delete(workflow);
            graphVizService.deleteCache(workflow.getID());

            newWorkflow.setId(workflow.getID());
            newWorkflow.setRetrievedOn(workflow.getRetrievedOn());
            newWorkflow.setRetrievedFrom(githubInfo);
            newWorkflow.setLastCommit(latestCommit);
            newWorkflow.setCwltoolStatus(Workflow.Status.SUCCESS);
            newWorkflow.setCwltoolVersion(cwlToolVersion);

            workflowRepository.save(newWorkflow);

        } catch (CWLValidationException ex) {
            logger.error(ex.getMessage(), ex);
            workflow.setCwltoolStatus(Workflow.Status.ERROR);
            workflow.setCwltoolLog(ex.getMessage());
            workflowRepository.save(workflow);
        } catch (Exception ex) {
            logger.error("Unexpected error", ex);
            workflow.setCwltoolStatus(Workflow.Status.ERROR);
            workflow.setCwltoolLog("Whoops! Cwltool ran successfully, but an unexpected " +
                    "error occurred in CWLViewer!\n" +
                    "Help us by reporting it on Gitter or a Github issue\n");
            workflowRepository.save(workflow);
        }

    }

}
