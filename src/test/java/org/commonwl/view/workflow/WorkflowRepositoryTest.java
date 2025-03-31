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
package org.commonwl.view.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.commonwl.view.git.GitDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@TestPropertySource(locations = "classpath:it-application.properties")
@DataJpaTest(showSql = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(initializers = PostgreSQLContextInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class WorkflowRepositoryTest {

  @Autowired WorkflowRepository repository;

  @Test
  public void deletedWorkflowByRetrievedFromTest() {

    assertNotNull(repository);

    // create stub workflow
    GitDetails gitDetails =
        new GitDetails("https://github.com/common-workflow-language/cwlviewer/", "main", "/");
    gitDetails.setPackedId("test_packedId");

    Workflow workflow = new Workflow();
    workflow.setRetrievedFrom(gitDetails);

    // save workflow
    repository.saveAndFlush(workflow);

    List<Workflow> all = repository.findAll();
    assertNotNull(all);
    assertEquals(1, all.size());

    // retrieve saved workflow by git details
    Workflow retrievedWorkflowAfterSave =
        repository.findByRetrievedFrom(workflow.getRetrievedFrom());
    assertNotNull(retrievedWorkflowAfterSave);

    // delete saved workflow by git details
    repository.delete(workflow);

    // retrieve deleted workflow by workflow git details
    Workflow retrievedWorkflowAfterDelete =
        repository.findByRetrievedFrom(workflow.getRetrievedFrom());
    assertNull(retrievedWorkflowAfterDelete);
  }
}
