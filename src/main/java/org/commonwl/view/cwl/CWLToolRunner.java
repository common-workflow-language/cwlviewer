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

import org.apache.jena.query.QueryException;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.commonwl.view.workflow.QueuedWorkflow;
import org.commonwl.view.workflow.QueuedWorkflowRepository;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

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
    private final ROBundleFactory roBundleFactory;
    private final String cwlToolVersion;

    @Autowired
    public CWLToolRunner(WorkflowRepository workflowRepository,
                         QueuedWorkflowRepository queuedWorkflowRepository,
                         CWLService cwlService,
                         CWLTool cwlTool,
                         ROBundleFactory roBundleFactory) {
        this.workflowRepository = workflowRepository;
        this.queuedWorkflowRepository = queuedWorkflowRepository;
        this.cwlService = cwlService;
        this.cwlToolVersion = cwlTool.getVersion();
        this.roBundleFactory = roBundleFactory;
    }

    @Async
    public void createWorkflowFromQueued(QueuedWorkflow queuedWorkflow, Path workflowFile, Path workTree)
            throws IOException, InterruptedException {

        Workflow tempWorkflow = queuedWorkflow.getTempRepresentation();

        // Parse using cwltool and replace in database
        try {
            Workflow newWorkflow = cwlService.parseWorkflowWithCwltool(
                    tempWorkflow,
                    workflowFile,
                    workTree);

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
            logger.error("Jena query exception ", ex);
            queuedWorkflow.setCwltoolStatus(CWLToolStatus.ERROR);
            queuedWorkflow.setMessage("An error occurred when executing a query on the SPARQL store");
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
