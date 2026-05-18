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

package org.commonwl.view.validation;

import org.commonwl.view.git.GitDetails;
import org.commonwl.view.workflow.WorkflowForm;
import org.springframework.validation.Errors;

/** Validate Git URLs. */
public interface GitUrlValidator {
  /**
   * Checks if the URL is supported or not.
   *
   * @param url URL
   * @return {@code true} if the URL is one of the supported Git flavours, {@code false} otherwise
   */
  boolean supports(String url);

  /**
   * Validate the form.
   *
   * @param form The web form
   * @param errors The errors object
   */
  void validate(WorkflowForm form, Errors errors);

  /**
   * Parse the Git URL returning a {@code GitDetails} object.
   *
   * @param url Git URL
   * @param form Workflow form
   * @return {@code GitDetails} object
   */
  GitDetails parse(String url, WorkflowForm form);
}
