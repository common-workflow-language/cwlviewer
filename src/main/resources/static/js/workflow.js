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
        d3: 'd3/d3',
        'dot-checker': 'graphviz-d3-renderer/dist/dot-checker',
        'layout-worker': 'graphviz-d3-renderer/dist/layout-worker',
        worker: 'requirejs-web-workers/src/worker',
        renderer: 'graphviz-d3-renderer/dist/renderer',
        jquery: 'jquery/dist/jquery.min',
        'bootstrap.modal': 'bootstrap/js/modal',
        'svg-pan-zoom': 'svg-pan-zoom/dist/svg-pan-zoom.min'
    },
    shim: {
        'bootstrap.modal': {
            deps: ['jquery']
        }
    }
});

/**
 * Main rendering code for the graphs on a workflow page
 */
require(['jquery', 'bootstrap.modal', 'renderer', 'svg-pan-zoom'],
    function ($, modal, renderer, svgPanZoom) {
        // Load dot graph from the page
        var dotGraph = $("#dot").val();

        // Initialise graph
        renderer.init("#graph");

        // Update stage with new dot source
        renderer.render(dotGraph);

        // Fade the loading and show graph when graph is drawn
        renderer.renderHandler(function() {
            $("#loading").fadeOut();

            // Enable svg-pan-zoom on the graph after load transition
            setTimeout(function() {
                svgPanZoom('svg', {
                    zoomEnabled: true,
                    controlIconsEnabled: true
                });
            }, 1000);
        });

        /**
         * Download the rendered graph as a png
         */
        $('#download-graph').click(function (event) {
            // Get the image data
            var img = renderer.stage.getImage(false);

            // Once it is loaded
            img.onload = function () {
                // Set hidden download link href to contents and click it
                var downloadLink = $("#download-link-graph");
                downloadLink.attr("href", img.src);
                downloadLink[0].click();
            };

            // Stop default button action
            event.preventDefault();
        });

        /**
         * DOT graph modal textarea automatically focuses when opened
         */
        $('#dotGraph').on('shown.bs.modal', function () {
            $('#dot').focus();
        })

        /**
         * DOT graph textarea focus selects all
         */
        $("#dot").focus(function() {
            $(this).select();
        });

        /**
         * Downloading of the DOT graph as a .gv file
         */
        $('#download-dot').click(function (event) {
            // Generate download link src
            var dotGraph = $("#dot").val();
            var src = "data:text/plain;charset=utf-8," + encodeURIComponent(dotGraph);

            // Set hidden download link href to contents and click it
            var downloadLink = $("#download-link-dot");
            downloadLink.attr("href", src);
            downloadLink[0].click();

            // Stop default button action
            event.preventDefault();
        });
    });

/**
 * Code for including the link to the Research Object Bundle download
 * without refresh once generated
 */
require(['jquery'],
    function ($) {
        // AJAX function to add download link to page if generated
        function getDownloadLink() {
            $.ajax({
                type: 'HEAD',
                url: $('#download').attr('href'),
                dataType: "json",
                success: function (data) {
                    $("#generating").addClass("hide");
                    $("#generated").removeClass("hide");
                },
                error: function () {
                    // Retry in 5 seconds if still not generated
                    setTimeout(function () {
                        getDownloadLink();
                    }, 5000)
                }
            });
        }

        // If ajaxRequired exists on the page the RO bundle link is not generated
        // at time of page load
        if ($("#ajaxRequired").length) {
            getDownloadLink();
        }
    });

/**
 * Highlighting step in graph when table row is
 * hovered over or vice-versa
 */
require(['jquery'],
    function ($) {

        /**
         * Gets the corresponding graph box for a table row
         * @param trElement The table row element
         * @return The graph box element(s)
         */
        function getGraphBox(trElement) {
            // Title of the CWL element
            var elementTitle = $(trElement).find("td:first").html();

            // Find corresponding graph box and return
            return $("title").filter(function() {
                return $(this).text() == elementTitle;
            }).siblings("path");
        }

        // When a table row is selected
        $( "tr" ).hover(function() {
            // Highlight, keeping previous in an attribute
            var graphBox = getGraphBox(this);
            graphBox.attr("data-prevfill", graphBox.css("fill"));
            graphBox.css("fill", "rgba(0, 255, 0, 0.11)");
        }, function() {
            // Unhighlight
            var graphBox = getGraphBox(this);
            graphBox.css("fill", graphBox.attr("data-prevfill"));
        });
    });