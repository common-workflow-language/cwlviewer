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
    baseUrl: '/js',
    paths: {
        d3: '/bower_components/d3/d3',
        'dot-checker': '/bower_components/graphviz-d3-renderer/dist/dot-checker',
        'layout-worker': '/bower_components/graphviz-d3-renderer/dist/layout-worker',
        worker: '/bower_components/requirejs-web-workers/src/worker',
        renderer: '/bower_components/graphviz-d3-renderer/dist/renderer',
        jquery: '/bower_components/jquery/dist/jquery.min',
        'bootstrap.modal': '/bower_components/bootstrap/js/modal'

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
require(['jquery', 'bootstrap.modal', 'renderer'],
    function ($, modal, renderer) {
        // Load dot graph from the page
        var dotGraph = $("#dot").val();

        // Initialise graph
        renderer.init({
            element: "#graph",
            zoom: true
        });

        // Update stage with new dot source
        renderer.render(dotGraph);

        // Fade the loading and show graph when graph is drawn
        renderer.renderHandler(function() {
            $("#loading").fadeOut();
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