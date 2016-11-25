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
        jquery: '/bower_components/jquery/dist/jquery.min'
    }
});

/**
 * Main rendering code for the graphs on a workflow page
 */
require(['jquery', 'renderer'],
    function ($, renderer) {
        // Dot graph as a string
        dotGraph = 'digraph G { color="#eeeeee"; bgcolor="#eeeeee"; subgraph cluster_c0 {a0 -> a1 -> a2 -> a3;} subgraph cluster_c1 {b0 -> b1 -> b2 -> b3;} x -> a0; x -> b0; a1 -> b3; b1 -> a3; } ';

        // Initialise graph
        renderer.init("#graph");

        // Update stage with new dot source
        renderer.render(dotGraph);

        /**
         * Download the rendered graph as a png
         */
        $('#download-graph').click(function (event) {
            // Get the image data
            var img = renderer.stage.getImage(false);

            // Once it is loaded
            img.onload = function () {
                // Set hidden download link href to contents and click it
                var downloadLink = $("#download");
                downloadLink.attr("href", img.src);
                downloadLink[0].click();
            };

            // Stop default button action
            event.preventDefault();
        });
    });