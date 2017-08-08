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
        jquery: 'jquery/dist/jquery.min'
    }
});

/**
 * AJAX call to check progress on cwltool run
 */
require(['jquery'],
    function ($) {

        function handleSuccess() {
            $("#loadingSpinner").hide();
            $("#loadingSuccess").show();
            location.reload();
            $("#loadingWarning").html('<a href="javascript:window.location.reload(true)">Click here</a> if your browser did not refresh automatically');
        }

        function handleFail(error) {
            $("#loadingHeader").html('Failed to parse ' + $("#workflowName").text() + ' with <a href="https://github.com/common-workflow-language/cwltool" rel="noopener" target="_blank">cwltool</a>');
            $("#loadingOverview").html('<a id="cwllog" href="#">Show Details</a>');
            $("#loadingSpinner").fadeOut(function() {
                $("#loadingFail").fadeIn();
            });
            $("#loadingWarning").html('<a class="btn btn-default" role="button" href="javascript:window.location.reload(true)">' +
                '<span class="glyphicon glyphicon-refresh" aria-hidden="true"></span> Try Again' +
                '</a>');
            $("#errorMsg").text(error);
        }

        function checkForDone() {
            $.ajax({
                type: 'GET',
                url: '/queue/' + $('#workflowID').text(),
                dataType: "json",
                cache: false,
                success: function(response) {
                    if (response.cwltoolStatus == "RUNNING") {
                        // Retry in 3 seconds
                        setTimeout(function () {
                            checkForDone();
                        }, 3000);
                    } else if (response.cwltoolStatus == "ERROR") {
                        handleFail(response.message);
                    } else {
                        handleSuccess();
                    }
                },
                error: function(response) {
                    // Retry in 3 seconds
                    setTimeout(function () {
                        checkForDone();
                    }, 3000);
                }
            });
        }

        checkForDone();

        // Click to show the complete error log
        $(document).on("click", "#cwllog", function() {
            var showHide = $("#cwllog");
            if (showHide.text() == 'Show Details') {
                showHide.text('Hide Details');
                $(".loadingWorkflowContainer").fadeOut(function() {
                    $("#cwltooldetails").fadeIn();
                });
            } else {
                showHide.text('Show Details');
                $("#cwltooldetails").fadeOut(function() {
                    $(".loadingWorkflowContainer").fadeIn();
                });
            }
        });

    });