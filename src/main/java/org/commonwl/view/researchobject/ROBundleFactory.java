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
import org.apache.taverna.robundle.Bundle;
import org.commonwl.view.git.GitDetails;
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

    private final WorkflowRepository workflowRepository;
    private final ROBundleService roBundleService;

    @Autowired
    public ROBundleFactory(ROBundleService roBundleService,
                           WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
        this.roBundleService = roBundleService;
    }

    /**
     * Creates a new Workflow Research Object Bundle from Git details
     * and saves it to a file
     * @param workflow The workflow to generate a RO bundle for
     * @throws IOException Any API errors which may have occurred
     */
    @Async
    public void createWorkflowRO(Workflow workflow)
            throws IOException, InterruptedException {
        logger.info("Creating Research Object Bundle");

        // Get the whole containing folder, not just the workflow itself
        GitDetails githubInfo = workflow.getRetrievedFrom();
        GitDetails roDetails = new GitDetails(githubInfo.getRepoUrl(), githubInfo.getBranch(),
                FilenameUtils.getPath(githubInfo.getPath()));

        // Create a new Research Object Bundle
        Bundle bundle = roBundleService.createBundle(workflow, roDetails);

        // Save the bundle to the storage location in properties
        Path bundleLocation = roBundleService.saveToFile(bundle);

        // Add RO Bundle to associated workflow model
        workflow.setRoBundlePath(bundleLocation.toString());
        workflowRepository.save(workflow);
        logger.info("Finished saving Research Object Bundle");
    }
}