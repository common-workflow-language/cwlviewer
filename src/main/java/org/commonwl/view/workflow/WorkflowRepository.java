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

import java.util.List;
import org.commonwl.view.git.GitDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Stores and retrieved workflow objects from the database
 *
 * <p>See Spring Data JPA docs: <a
 * href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/">...</a>
 */
@Repository
public interface WorkflowRepository extends CrudRepository<Workflow, String> {

  /**
   * Finds a workflow model in the database based on where it was retrieved from
   *
   * @param retrievedFrom Details of where the workflow is from
   * @return The workflow model
   */
  Workflow findByRetrievedFrom(GitDetails retrievedFrom);

  /**
   * Finds a workflow model in the database based on a commit ID and path
   *
   * @param commitId The latest commit ID of the workflow
   * @param path The path to the workflow within the repository
   * @return The workflow model
   */
  @Query(
      value =
          "SELECT w.* FROM workflow w WHERE w.last_commit = ?1 AND w.retrieved_from ->> 'path' = ?2",
      nativeQuery = true)
  List<Workflow> findByCommitAndPath(String commitId, String path);

  /**
   * Find all known workflow models in the database for a commit ID
   *
   * @param commitId The latest commit ID of the workflow
   * @return The workflow model
   */
  @Query("SELECT w FROM Workflow w WHERE w.lastCommit = ?1")
  List<Workflow> findByCommit(String commitId);

  /**
   * Paged request to get workflows of a specific status
   *
   * @param pageable The details of the page to be retrieved
   * @return The requested page of workflows
   */
  Page<Workflow> findAllByOrderByRetrievedOnDesc(Pageable pageable);

  /**
   * Finds successful workflows where a string is within the label or doc
   *
   * @param label The string to search for in the label
   * @param doc The string to search for in the doc
   * @param pageable The details of the page to be retrieved
   */
  Page<Workflow> findByLabelContainingOrDocContainingIgnoreCase(
      String label, String doc, Pageable pageable);
}
