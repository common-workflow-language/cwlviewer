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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.commonwl.view.util.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Interacts with the Python reference implementation of the common workflow language */
@Service
public class CWLTool {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private String cwlToolVersion;

  /**
   * Get the RDF representation of a CWL file
   *
   * @param url The URL of the CWL file
   * @return The RDF representing the CWL file
   * @throws CWLValidationException cwltool errors
   */
  public String getRDF(String url) throws CWLValidationException {
    return runCwltoolOnWorkflow("--print-rdf", url);
  }

  /**
   * Get the packed version of a CWL workflow
   *
   * @param url The URL of the CWL file
   * @return The packed version of the workflow
   * @throws CWLValidationException cwltool errors
   */
  public String getPackedVersion(String url) throws CWLValidationException {
    return runCwltoolOnWorkflow("--pack", url);
  }

  /**
   * Gets the version of cwltool being used
   *
   * @return The version number
   */
  public String getVersion() {
    if (cwlToolVersion != null) {
      return cwlToolVersion;
    }
    Process process = null;
    try {
      // Run cwltool --version
      String[] command = {"cwltool", "--version"};
      ProcessBuilder cwlToolProcess = new ProcessBuilder(command);
      process = cwlToolProcess.start();

      // Get input stream
      InputStream is = process.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);

      String line;
      String cwlToolVersion;
      if ((line = br.readLine()) != null) {
        cwlToolVersion = line.substring(line.indexOf(' ') + 1);
      } else {
        cwlToolVersion = "<error getting cwltool version>";
      }
      return cwlToolVersion;

    } catch (IOException ex) {
      return "<error getting cwltool version>";
    } finally {
      if (process != null) {
        process.destroyForcibly();
      }
    }
  }

  /**
   * Runs cwltool on a workflow with a given argument
   *
   * @param argument The argument for cwltool
   * @param workflowUrl The url of the workflow
   * @return The standard output of cwltool
   * @throws CWLValidationException Errors from cwltool
   */
  private String runCwltoolOnWorkflow(String argument, String workflowUrl)
      throws CWLValidationException {
    Process process = null;

    try {
      // Run command
      String[] command = {
        "cwltool",
        "--disable-color",
        "--non-strict",
        "--quiet",
        "--enable-dev",
        "--enable-ext",
        "--skip-schemas",
        argument,
        workflowUrl
      };
      ProcessBuilder cwlToolProcess = new ProcessBuilder(command);
      process = cwlToolProcess.start();

      // Read output from the process using threads
      StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream());
      StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
      errorGobbler.start();
      inputGobbler.start();

      // Wait for process to complete
      int exitCode = process.waitFor();
      if (exitCode == 0) {
        inputGobbler.join();
        process.destroyForcibly();
        return inputGobbler.getContent();
      } else {
        errorGobbler.join();
        process.destroyForcibly();
        throw new CWLValidationException(errorGobbler.getContent());
      }
    } catch (IOException | InterruptedException e) {
      logger.error("Error running cwltool process", e);
      throw new CWLValidationException("Error running cwltool process");
    } finally {
      if (process != null) {
        process.destroyForcibly();
      }
    }
  }
}
