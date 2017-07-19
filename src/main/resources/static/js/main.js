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

/**
 * RequireJS configuration with all possible dependencies
 */
requirejs.config({
    baseUrl: '/bower_components',
    paths: {
        'jquery': 'jquery/dist/jquery.min',
    }
});

/**
 * Suggestions on the main page
 */
require(['jquery'],
    function ($) {
        $(".example").click(function(e) {
            $("#url").val($(this).attr("href"));
            e.preventDefault();
        });
    });

/**
 * Validation for URL
 */
require(['jquery'],
    function ($) {
        $('#url').on('input', function() {
            var generalPattern = "\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:tree|blob)\\/([^/]+)(?:\\/(.+\\.cwl))$";
            var githubPattern = "^https?:\\/\\/github\\.com" + generalPattern;
            var gitlabPattern = "^https?:\\/\\/gitlab\\.com" + generalPattern;
            var gitPattern = "((git|ssh|http(s)?)|(git@[\\w\\.]+))(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?";

            var input = $(this).text();
            if (input.search(githubPattern) > 0 || input.search(gitlabPattern) > 0) {
                $("#extraInputs").fadeOut();
                $(this).closest(".form-group").addClass("has-success");
            } else if (input.search(gitPattern) > 0) {
                $("#extraInputs").fadeIn();
            }

            //$(this).closest(".form-group").addClass("has-warning");
            //$(".validateMessage").text("Git repository URL must be HTTP or HTTPS");
        });
    });