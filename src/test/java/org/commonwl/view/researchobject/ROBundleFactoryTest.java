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

import org.commonwl.view.git.GitDetails;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowRepository;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

/**
 * Test the separate async method to call the ROBundle constructor
 */
public class ROBundleFactoryTest {

    /**
     * Simulate creation of a valid workflow
     */
    @Test
    public void bundleForValidWorkflow() throws Exception {

        Workflow validWorkflow = new Workflow("Valid Workflow", "Doc for Valid Workflow",
                new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
        validWorkflow.setRetrievedFrom(Mockito.mock(GitDetails.class));

        // Mocked path to a RO bundle
        ROBundleService mockROBundleService = Mockito.mock(ROBundleService.class);
        when(mockROBundleService.saveToFile(anyObject()))
                .thenReturn(Paths.get("test/path/to/check/for.zip"));

        // Test method retries multiple times to get workflow model before success
        WorkflowRepository mockRepository = Mockito.mock(WorkflowRepository.class);
        when(mockRepository.findByRetrievedFrom(anyObject()))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(validWorkflow);

        // Create factory under test
        ROBundleFactory factory = new ROBundleFactory(mockROBundleService, mockRepository);

        // Attempt to add RO to workflow
        factory.createWorkflowRO(validWorkflow);

        assertEquals(Paths.get("test/path/to/check/for.zip"), 
                Paths.get(validWorkflow.getRoBundlePath()));

    }

}