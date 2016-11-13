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

package org.researchobject.services;

import org.researchobject.domain.GithubDetails;
import org.researchobject.domain.WorkflowRO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Class for the purpose of a Spring Framework Async method
 * being in a different class to where it is called
 *
 * This allows the proxy to kick in and run it asynchronously
 */
@Component
@EnableAsync
public class WorkflowROFactory {

    /**
     * Creates a new Workflow Research Object Bundle from a Github URL
     * and saves it to a file
     * @param githubService The service for Github API functionality
     * @param githubInfo Details of the Github repository
     * @param githubBasePath The path within the repository where the workflow files are
     * @throws IOException Any API errors which may have occured
     */
    @Async
    void workflowROFromGithub(GitHubService githubService,
                              GithubDetails githubInfo,
                              String githubBasePath) throws IOException {
        // TODO: Add the bundle link to the page when it is finished being created
        WorkflowRO bundle = new WorkflowRO(githubService, githubInfo, githubBasePath);
        bundle.saveToTempFile();
    }
}
