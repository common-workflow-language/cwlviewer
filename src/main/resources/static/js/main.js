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
        'jquery': 'jquery/dist/jquery.min'
    }
});

/**
 * Suggestions on the main page
 */
require(['jquery'],
    function ($) {
        $(".example").click(function(e) {
            $("#url").val($(this).attr("href")).trigger("change");
            e.preventDefault();
        });
    });

/**
 * Validation for form
 */
require(['jquery'],
    function ($) {
        var generalPattern = "\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:(?:tree|blob)\\/([^/]+)\\/?(.*)?)?$";
        var githubPattern = new RegExp("^https?:\\/\\/github\\.com" + generalPattern);
        var gitlabPattern = new RegExp("^https?:\\/\\/gitlab\\.com" + generalPattern);
        var gitPattern = new RegExp("^((git|ssh|http(s)?)|(git@[\\w\\.]+))(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?$");

        /**
         * Show extra details in form if generic git repository
         */
        $("#url").on('change keyup paste', function () {
            var input = $(this).val();
            if (gitPattern.test(input)) {
                $("#extraInputs").fadeIn();
            } else {
                $("#extraInputs").fadeOut();
            }
        });

        /**
         * Clear warnings when fields change
         */
        $("input").keyup(function(e) {
            // Fix for enter key being detected as a change
            if (e.keyCode != 13) {
                var field = $(this);
                field.parent().removeClass("has-error");
                field.next().text("");
            }
        });

        /**
         * Validate form before submit
         */
        $('#add').submit(function() {
            var input = $("#url").val();
            if (gitPattern.test(input)) {
                var success = false;
                if (input.startsWith("ssh") || input.startsWith("git@")) {
                    addWarning("url", "SSH is not supported as a protocol, please provide a HTTPS URL to clone");
                } else {
                    success = true;
                    if (!$("#branch").val()) {
                        addWarning("branch", "You must provide a branch name for the workflow");
                        success = false;
                    }
                    if (!$("#path").val()) {
                        addWarning("path", "You must provide a path to the workflow or a directory of workflows");
                        success = false;
                    }
                }
                return success;
            } else if (!githubPattern.test(input) && !gitlabPattern.test(input)) {
                addWarning("url", "Must be a URL to a workflow or directory of workflows on Gitlab or Github, or a Git repository URL");
                return false;
            }
        });

        /**
         * Adds warning state and message to the a field
         * @param id The ID of the field
         * @param message The message to be displayed on the form element
         */
        function addWarning(id, message) {
            var field = $("#" + id);
            field.parent().addClass("has-error");
            field.next().text(message);
        }

        $("#url").trigger("change");
    });