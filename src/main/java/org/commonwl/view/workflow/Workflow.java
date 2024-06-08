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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tbouron.SpdxLicense;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import org.commonwl.view.WebConfig;
import org.commonwl.view.WebConfig.Format;
import org.commonwl.view.cwl.CWLElement;
import org.commonwl.view.cwl.CWLStep;
import org.commonwl.view.git.GitDetails;
import org.commonwl.view.util.BaseEntity;
import org.commonwl.view.util.LicenseUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.format.annotation.DateTimeFormat;

/** Representation of a workflow */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(
    value = {"id", "roBundlePath", "roBundleLink"},
    ignoreUnknown = true)
@Entity
@Table(
    name = "workflow",
    indexes = {
      @Index(columnList = "retrievedFrom", unique = true),
      @Index(columnList = "retrievedOn")
    })
public class Workflow extends BaseEntity implements Serializable {

  // ID for database
  @Id
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "uuid2")
  @Column(length = 36, nullable = false, updatable = false)
  public String id;

  // Metadata
  @Column(columnDefinition = "jsonb")
  @Type(type = "json")
  @Convert(disableConversion = true)
  private GitDetails retrievedFrom;

  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss z")
  private Date retrievedOn;

  // The last commit from the branch at the time of fetching
  // Used for caching purposes
  @Column(columnDefinition = "TEXT")
  private String lastCommit;

  // A String which represents the path to a RO bundle
  // Path types cannot be stored using Spring Data, unfortunately
  @Column(columnDefinition = "TEXT")
  private String roBundlePath;

  // Contents of the workflow
  @Column(columnDefinition = "TEXT")
  private String label;

  @Column(columnDefinition = "TEXT")
  private String doc;

  @Column(columnDefinition = "jsonb")
  @Type(type = "json")
  @Convert(disableConversion = true)
  private Map<String, CWLElement> inputs;

  @Column(columnDefinition = "jsonb")
  @Type(type = "json")
  @Convert(disableConversion = true)
  private Map<String, CWLElement> outputs;

  @Column(columnDefinition = "jsonb")
  @Type(type = "json")
  @Convert(disableConversion = true)
  private Map<String, CWLStep> steps;

  // Currently only DockerRequirement is parsed for this
  @Column(columnDefinition = "TEXT")
  private String dockerLink;

  @Column(columnDefinition = "TEXT")
  private String cwltoolVersion = "";

  // DOT graph of the contents
  @Column(columnDefinition = "TEXT")
  private String visualisationDot;

  private static final String PERMANENT_LINK_BASE_URL = "https://w3id.org/cwl/view";

  @Column(columnDefinition = "TEXT")
  private String licenseLink;

  public Workflow(
      String label,
      String doc,
      Map<String, CWLElement> inputs,
      Map<String, CWLElement> outputs,
      Map<String, CWLStep> steps,
      String dockerLink,
      String licenseLink) {
    this.label = label;
    this.doc = doc;
    this.inputs = inputs;
    this.outputs = outputs;
    this.steps = steps;
    this.dockerLink = dockerLink;
    this.licenseLink = licenseLink;
  }

  public Workflow(
      String label,
      String doc,
      Map<String, CWLElement> inputs,
      Map<String, CWLElement> outputs,
      Map<String, CWLStep> steps) {
    this(label, doc, inputs, outputs, steps, null, null);
  }

  public Workflow() {
    this(null, null, null, null, null, null, null);
  }

  public String getID() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDoc() {
    return doc;
  }

  public void setDoc(String doc) {
    this.doc = doc;
  }

  public Map<String, CWLElement> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, CWLElement> inputs) {
    this.inputs = inputs;
  }

  public Map<String, CWLElement> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, CWLElement> outputs) {
    this.outputs = outputs;
  }

  public Map<String, CWLStep> getSteps() {
    return steps;
  }

  public void setSteps(Map<String, CWLStep> steps) {
    this.steps = steps;
  }

  public String getRoBundlePath() {
    return roBundlePath;
  }

  public void setRoBundlePath(String roBundlePath) {
    this.roBundlePath = roBundlePath;
  }

  public GitDetails getRetrievedFrom() {
    return retrievedFrom;
  }

  public void setRetrievedFrom(GitDetails retrievedFrom) {
    this.retrievedFrom = retrievedFrom;
  }

  public Date getRetrievedOn() {
    return retrievedOn;
  }

  public void setRetrievedOn(Date retrievedOn) {
    this.retrievedOn = retrievedOn;
  }

  public String getLastCommit() {
    return lastCommit;
  }

  public void setLastCommit(String lastCommit) {
    this.lastCommit = lastCommit;
  }

  public String getDockerLink() {
    return dockerLink;
  }

  public void setDockerLink(String dockerLink) {
    this.dockerLink = dockerLink;
  }

  public String getCwltoolVersion() {
    return cwltoolVersion;
  }

  public void setCwltoolVersion(String cwltoolVersion) {
    this.cwltoolVersion = cwltoolVersion;
  }

  public String getVisualisationDot() {
    return visualisationDot;
  }

  public void setVisualisationDot(String visualisationDot) {
    this.visualisationDot = visualisationDot;
  }

  // The following are here for Jackson message converter for the REST API
  // Include links to related resources

  public String getVisualisationXdot() {
    return retrievedFrom.getInternalUrl().replaceFirst("/workflows", "/graph/xdot");
  }

  public String getVisualisationPng() {
    return retrievedFrom.getInternalUrl().replaceFirst("/workflows", "/graph/png");
  }

  public String getVisualisationSvg() {
    return retrievedFrom.getInternalUrl().replaceFirst("/workflows", "/graph/svg");
  }

  public String getRoBundle() {
    if (roBundlePath != null) {
      return getRoBundleLink();
    } else {
      return null;
    }
  }

  public String getRoBundleLink() {
    return retrievedFrom.getInternalUrl().replaceFirst("/workflows", "/robundle");
  }

  /**
   * Permalink for this workflow. including packed part, but no format
   *
   * @return the permalink
   */
  public String getPermalink() {
    return getPermalink(null);
  }

  /**
   * Permalink for a particular representation of this workflow. Note that resolving the permalink
   * will use the official deployment of the CWL Viewer.
   *
   * @see <a href="https://w3id.org/cwl/view">https://w3id.org/cwl/view</a>
   * @param format Format of representation, or <code>null</code> for format-neutral permalink that
   *     supports content negotiation.
   * @return A Permalink identifying the representation of this workflow.
   */
  public String getPermalink(WebConfig.Format format) {

    String packedPart = "";
    String formatPartSep = "?";
    String formatPart = "";
    if (retrievedFrom.getPackedId() != null && !Format.raw.equals(format)) {
      // No need for ?part= for ?format=raw
      packedPart = "?part=" + retrievedFrom.getPackedId();
      formatPartSep = "&";
    }
    if (format != null) {
      formatPart = formatPartSep + "format=" + format.name();
    }
    return PERMANENT_LINK_BASE_URL
        + "/git/"
        + lastCommit
        + "/"
        + retrievedFrom.getPath()
        + packedPart
        + formatPart;
  }

  /**
   * RDF identifier, uses #hash for parts
   *
   * @return the RDF identifier
   */
  @JsonProperty(value = "@id", index = 0)
  public String getIdentifier() {
    String packedPart =
        (retrievedFrom.getPackedId() != null) ? "#" + retrievedFrom.getPackedId() : "";
    return PERMANENT_LINK_BASE_URL
        + "/git/"
        + lastCommit
        + "/"
        + retrievedFrom.getPath()
        + packedPart;
  }

  public boolean isPacked() {
    return retrievedFrom.getPackedId() != null;
  }

  public String getLicenseLink() {
    return licenseLink;
  }

  public void setLicenseLink(String licenseLink) {
    this.licenseLink = licenseLink;
  }

  public String getLicenseName() {
    if (licenseLink == null) {
      return null;
    }
    if (licenseLink.startsWith(LicenseUtils.SPDX_LICENSES_PREFIX)) {
      return SpdxLicense.fromId(licenseLink.replace(LicenseUtils.SPDX_LICENSES_PREFIX, "")).name;
    }
    return licenseLink;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Workflow workflow = (Workflow) o;
    return Objects.equals(id, workflow.id)
        && Objects.equals(retrievedFrom, workflow.retrievedFrom)
        && Objects.equals(retrievedOn, workflow.retrievedOn)
        && Objects.equals(lastCommit, workflow.lastCommit)
        && Objects.equals(roBundlePath, workflow.roBundlePath)
        && Objects.equals(label, workflow.label)
        && Objects.equals(doc, workflow.doc)
        && Objects.equals(inputs, workflow.inputs)
        && Objects.equals(outputs, workflow.outputs)
        && Objects.equals(steps, workflow.steps)
        && Objects.equals(dockerLink, workflow.dockerLink)
        && Objects.equals(cwltoolVersion, workflow.cwltoolVersion)
        && Objects.equals(visualisationDot, workflow.visualisationDot)
        && Objects.equals(licenseLink, workflow.licenseLink);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        retrievedFrom,
        retrievedOn,
        lastCommit,
        roBundlePath,
        label,
        doc,
        inputs,
        outputs,
        steps,
        dockerLink,
        cwltoolVersion,
        visualisationDot,
        licenseLink);
  }
}
