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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RDFServiceTest {

    /**
     * Create a service to test
     */
    @Autowired
    private RDFService rdfService;

    /**
     * Test extracting step names from full URIs
     */
    @Test
    public void stepNameFromURI() throws Exception {

        // PACKED - with and without input
        String baseURL = "https://rawgit.com/common-workflow-language/workflows/master/workflows/make-to-cwl/dna.cwl#main";
        assertEquals("combine_sequences", rdfService.stepNameFromURI(baseURL,
                baseURL + "/combine_sequences"));
        assertEquals("combine_sequences", rdfService.stepNameFromURI(baseURL,
                baseURL + "/combine_sequences/catout"));

        // UNPACKED - with and without input
        baseURL = "https://raw.githubusercontent.com/KnowEnG-Research/cwl-specification/master/code/workflow.cwl";
        assertEquals("clean_p", rdfService.stepNameFromURI(baseURL,
                baseURL + "#clean_p"));
        assertEquals("clean_p", rdfService.stepNameFromURI(baseURL,
                baseURL + "#clean_p/spreadsheet_format"));

    }

    /**
     * Test formatting default values
     */
    @Test
    public void formatDefault() throws Exception {
        assertEquals("\\\"-bg\\\"", rdfService.formatDefault("-bg"));
        assertEquals("10000", rdfService.formatDefault("10000^^http://www.w3.org/2001/XMLSchema#integer"));
        assertEquals("true", rdfService.formatDefault("true^^http://www.w3.org/2001/XMLSchema#boolean"));
    }

    /**
     * Test extracting a label from name
     */
    @Test
    public void labelFromName() throws Exception {
        assertEquals("trim_method", rdfService.labelFromName("trim_method"));
        assertEquals("outfile", rdfService.labelFromName("https://cdn.rawgit.com/common-workflow-language/workflows/549c973ccc01781595ce562dea4cedc6c9540fe0/workflows/make-to-cwl/dna.cwl#main/outfile"));
    }

}
