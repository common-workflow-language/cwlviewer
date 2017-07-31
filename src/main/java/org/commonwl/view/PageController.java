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

package org.commonwl.view;

import org.commonwl.view.workflow.WorkflowForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    /**
     * Main page of the application
     * @param model The model for the home page where the workflow form is added
     * @return The view for this page
     */
    @GetMapping("/")
    public String homePage(Model model, @RequestParam(value = "url", required = false) String defaultURL) {
        model.addAttribute("workflowForm", new WorkflowForm(defaultURL));
        return "index";
    }

    /**
     * About page
     * @param model The model for the about page
     * @return The view for this page
     */
    @GetMapping("/about")
    public String about(Model model) {
        return "about";
    }

    /**
     * API documentation page
     * @param model The model for the API documentation page
     * @return The view for this page
     */
    @GetMapping("/apidocs")
    public String apiDocumentation(Model model) {
        return "apidocs";
    }

}
