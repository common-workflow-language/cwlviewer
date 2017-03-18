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

package org.commonwl.view.docker;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DockerServiceTest {

    private DockerService dockerService;

    /**
     * New instance of DockerService
     */
    @Before
    public void setUp() throws Exception {
        dockerService = new DockerService();
    }

    /**
     * Test conversion from docker pull tag to dockerhub URL
     */
    @Test
    public void getStandardDockerHubURL() throws Exception {
        String test = DockerService.getDockerHubURL("stain/cwlviewer");
        assertEquals("https://hub.docker.com/r/stain/cwlviewer", test);
    }

    /**
     * Second valid example
     */
    @Test
    public void getStandardDockerHubURL2() throws Exception {
        String test = DockerService.getDockerHubURL("rabix/lobSTR");
        assertEquals("https://hub.docker.com/r/rabix/lobSTR", test);
    }

    /**
     * Example from the official repository
     */
    @Test
    public void getOfficialRepoDockerHubURL() throws Exception {
        String test = DockerService.getDockerHubURL("ubuntu");
        assertEquals("https://hub.docker.com/r/_/ubuntu", test);
    }

    /**
     * Invalid tag should fail to get URL
     */
    @Test
    public void getInvalidDockerHubURL() throws Exception {
        String test = DockerService.getDockerHubURL("clearly/not/a/valid/tag");
        assertNull(test);
    }

}