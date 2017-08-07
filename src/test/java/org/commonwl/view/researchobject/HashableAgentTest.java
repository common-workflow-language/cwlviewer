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

import org.junit.Test;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HashableAgentTest {

    /**
     * If the ORCID of two agents is the same, they are the same person
     * regardless of other information
     */
    @Test
    public void orcidsMakeAgentsEqual() throws Exception {

        HashableAgent firstOrcid = new HashableAgent("Mark Robindsason",
                new URI("http://orcid.org/0000-0002-8184-7507"),
                new URI("mark@example.org"));

        HashableAgent secondOrcid = new HashableAgent("Mark Robinson",
                new URI("http://orcid.org/0000-0002-8184-7507"),
                new URI("mark@example.com"));

        Set<HashableAgent> testSet = new HashSet<>();
        testSet.add(firstOrcid);
        testSet.remove(secondOrcid);
        testSet.add(secondOrcid);

        assertEquals(1, testSet.size());
        HashableAgent fromSet = testSet.iterator().next();
        assertEquals("Mark Robinson", fromSet.getName());
        assertEquals(new URI("mark@example.com"), fromSet.getUri());
        assertEquals(new URI("http://orcid.org/0000-0002-8184-7507"), fromSet.getOrcid());

    }

    /**
     * When no ORCID is present but emails are the same, the agents are
     * the same person regardless of name
     */
    @Test
    public void noOrcidEmailSameMakesAgentsEqual() throws Exception {

        HashableAgent firstEmail = new HashableAgent("Mark Robindsason",
                null,
                new URI("mark@example.com"));

        HashableAgent secondEmail = new HashableAgent("Mark Robinson",
                null,
                new URI("mark@example.com"));

        Set<HashableAgent> testSet = new HashSet<>();
        testSet.add(firstEmail);
        testSet.remove(secondEmail);
        testSet.add(secondEmail);

        assertEquals(1, testSet.size());
        HashableAgent fromSet = testSet.iterator().next();
        assertEquals("Mark Robinson", fromSet.getName());
        assertEquals(new URI("mark@example.com"), fromSet.getUri());
        assertNull(fromSet.getOrcid());

    }
}
