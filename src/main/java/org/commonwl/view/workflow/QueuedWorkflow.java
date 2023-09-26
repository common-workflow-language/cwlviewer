package org.commonwl.view.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.commonwl.view.cwl.CWLToolStatus;
import org.commonwl.view.util.BaseEntity;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

/** A workflow pending completion of cwltool */
@JsonIgnoreProperties(value = {"id", "tempRepresentation", "workflowList"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Entity
@Table(name = "queued_workflow")
public class QueuedWorkflow extends BaseEntity implements Serializable {

  // ID for database
  @Id
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "uuid2")
  @Column(length = 36, nullable = false, updatable = false)
  public String id;

  // Very barebones workflow to build loading thumbnail and overview
  @Column(columnDefinition = "jsonb")
  @Type(value = JsonType.class)
  @Convert(disableConversion = true)
  private Workflow tempRepresentation;

  // List of packed workflows for packed workflows
  // TODO: Refactor so this is not necessary
  @Column(columnDefinition = "jsonb")
  @Type(value = JsonType.class)
  @Convert(disableConversion = true)
  private List<WorkflowOverview> workflowList;

  // Cwltool details
  @Column(columnDefinition = "jsonb")
  @Type(value = JsonType.class)
  @Convert(disableConversion = true)
  private CWLToolStatus cwltoolStatus = CWLToolStatus.RUNNING;

  @Column(columnDefinition = "TEXT")
  private String cwltoolVersion = "";

  @Column(columnDefinition = "TEXT")
  private String message;

  public String getId() {
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
