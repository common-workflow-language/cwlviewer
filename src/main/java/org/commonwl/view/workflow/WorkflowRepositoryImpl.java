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

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import org.commonwl.view.git.GitDetails;
import org.hibernate.query.Query;

public class WorkflowRepositoryImpl implements WorkflowRepositoryCustom {

  private static final String QUERY_FIND_BY_RETRIEVED_FROM =
      "SELECT w.* FROM workflow w WHERE w.retrieved_from = :retrievedFrom";

  @PersistenceContext(type = PersistenceContextType.EXTENDED)
  EntityManager entityManager;

  @Override
  public Workflow findByRetrievedFrom(GitDetails retrievedFrom) {
    final Query<?> query =
        entityManager
            .createNativeQuery(QUERY_FIND_BY_RETRIEVED_FROM, Workflow.class)
            .unwrap(Query.class);

    if (query == null) {
      return null;
    }

    query.setParameter("retrievedFrom", retrievedFrom, new JsonType(GitDetails.class));
    return (Workflow) query.uniqueResult();
  }
}
