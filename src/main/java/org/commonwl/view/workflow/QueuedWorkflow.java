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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.commonwl.view.cwl.CWLToolStatus;
import org.commonwl.view.util.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A workflow pending completion of cwltool */
@JsonIgnoreProperties(value = {"id", "tempRepresentation", "workflowList"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Entity
@Table(name = "queued_workflow")
public class QueuedWorkflow extends BaseEntity implements Serializable {

  // ID for database
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  // Very barebones workflow to build loading thumbnail and overview
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private Workflow tempRepresentation;

  // List of packed workflows for packed workflows
  // TODO: Refactor so this is not necessary
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<WorkflowOverview> workflowList;

  // Cwltool details
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private CWLToolStatus cwltoolStatus = CWLToolStatus.RUNNING;

  @Column(columnDefinition = "TEXT")
  private String cwltoolVersion = "";

  @Column(columnDefinition = "TEXT")
  private String message;

  public UUID getId() {
    return id;
  }

  public Workflow getTempRepresentation() {
    return tempRepresentation;
  }

  public void setTempRepresentation(Workflow tempRepresentation) {
    this.tempRepresentation = tempRepresentation;
  }

  public CWLToolStatus getCwltoolStatus() {
    return cwltoolStatus;
  }

  public void setCwltoolStatus(CWLToolStatus cwltoolStatus) {
    this.cwltoolStatus = cwltoolStatus;
  }

  public String getCwltoolVersion() {
    return cwltoolVersion;
  }

  public void setCwltoolVersion(String cwltoolVersion) {
    this.cwltoolVersion = cwltoolVersion;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<WorkflowOverview> getWorkflowList() {
    return workflowList;
  }

  public void setWorkflowList(List<WorkflowOverview> workflowList) {
    this.workflowList = workflowList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueuedWorkflow that = (QueuedWorkflow) o;
    return Objects.equals(id, that.id)
        && Objects.equals(tempRepresentation, that.tempRepresentation)
        && Objects.equals(workflowList, that.workflowList)
        && cwltoolStatus == that.cwltoolStatus
        && Objects.equals(cwltoolVersion, that.cwltoolVersion)
        && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, tempRepresentation, workflowList, cwltoolStatus, cwltoolVersion, message);
  }
}
