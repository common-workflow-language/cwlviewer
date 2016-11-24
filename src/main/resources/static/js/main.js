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

requirejs.config({
    baseUrl: 'js',
    paths: {
        d3: '/bower_components/d3/d3',
        'dot-checker': '/bower_components/graphviz-d3-renderer/dist/dot-checker',
        'layout-worker': '/bower_components/graphviz-d3-renderer/dist/layout-worker',
        worker: '/bower_components/requirejs-web-workers/src/worker',
        renderer: '/bower_components/graphviz-d3-renderer/dist/renderer'
    }
});

require(['renderer'],
    function (renderer) {
        dotSource = 'digraph G { bgcolor="#eeeeee"; subgraph cluster_c0 {a0 -> a1 -> a2 -> a3;} subgraph cluster_c1 {b0 -> b1 -> b2 -> b3;} x -> a0; x -> b0; a1 -> b3; b1 -> a3; } ';
        zoomFunc = renderer.init({
            element: '#visualisation',
            extend: [0.1, 10],
        });

        // update stage with new dot source
        renderer.render(dotSource);
    });