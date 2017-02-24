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
        jquery: 'jquery/dist/jquery.min',
        'bootstrap.modal': 'bootstrap/js/modal',
        'svg-pan-zoom': 'svg-pan-zoom/dist/svg-pan-zoom.min',
        'hammerjs': 'hammerjs/hammer.min'
    },
    shim: {
        'bootstrap.modal': {
            deps: ['jquery']
        }
    }
});

/**
 * Make the graph pannable and zoomable
 */
require(['jquery', 'bootstrap.modal', 'svg-pan-zoom', 'hammerjs'],
    function ($, modal, svgPanZoom, hammerjs) {

        // Custom hammer event handler to add mobile support
        // Based on example in svg-pan-zoom/demo/mobile.html
        var eventHandler = {
            haltEventListeners: ['touchstart', 'touchend', 'touchmove', 'touchleave', 'touchcancel'],
            init: function(options) {

                var instance = options.instance;
                var initialScale = 1;
                var pannedX = 0;
                var pannedY = 0;

                // Init Hammer
                // Listen only for pointer and touch events
                this.hammer = Hammer(options.svgElement, {
                    inputClass: Hammer.SUPPORT_POINTER_EVENTS ? Hammer.PointerEventInput : Hammer.TouchInput
                });

                // Enable pinch
                this.hammer.get('pinch').set({
                    enable: true
                });

                // Handle double tap
                this.hammer.on('doubletap', function(ev){
                    instance.zoomIn()
                });

                // Handle pan
                this.hammer.on('panstart panmove', function(ev){
                    // On pan start reset panned variables
                    if (ev.type === 'panstart') {
                        pannedX = 0;
                        pannedY = 0;
                    }
                    // Pan only the difference
                    instance.panBy({x: ev.deltaX - pannedX, y: ev.deltaY - pannedY});
                    pannedX = ev.deltaX;
                    pannedY = ev.deltaY;
                });

                // Handle pinch
                this.hammer.on('pinchstart pinchmove', function(ev){
                    // On pinch start remember initial zoom
                    if (ev.type === 'pinchstart') {
                        initialScale = instance.getZoom()
                        instance.zoom(initialScale * ev.scale)
                    }
                    instance.zoom(initialScale * ev.scale)
                });

                // Prevent moving the page on some devices when panning over SVG
                options.svgElement.addEventListener('touchmove', function(e){
                    e.preventDefault();
                });

            }, destroy: function(){
                this.hammer.destroy()
            }
        };

        // Enable svg-pan-zoom on the graph
        var graph = svgPanZoom('#graph', {
            zoomEnabled: true,
            controlIconsEnabled: true,
            customEventsHandler: eventHandler
        });

        // Resizing window also resizes the graph
        $(window).resize(function(){
            graph.resize();
            graph.fit();
            graph.center();
        });

        // Enable svg-pan-zoom on fullscreen modal when opened
        $('#fullScreenGraphModal').on('shown.bs.modal', function (e) {
            // Timeout allows for modal to show
            setTimeout(function() {
                var fullGraph = svgPanZoom('#graphFullscreen', {
                    zoomEnabled: true,
                    controlIconsEnabled: true,
                    customEventsHandler: eventHandler
                });

                // Set to same zoom/pan as other graph
                fullGraph.zoom(graph.getZoom());
                fullGraph.pan(graph.getPan());

                // Link the two graphs panning and zooming
                fullGraph.setOnZoom(function(level){
                    graph.zoom(level);
                    graph.pan(fullGraph.getPan());
                });

                fullGraph.setOnPan(function(point){
                    graph.pan(point);
                });

                // Resizing window also resizes the graph
                $(window).resize(function(){
                    fullGraph.resize();
                    fullGraph.fit();
                    fullGraph.center();
                });
            }, 100);
        });
    });

/**
 * Handle the dot graph modal and related features
 */
require(['jquery', 'bootstrap.modal'],
    function ($, modal) {
        /**
         * DOT graph modal textarea automatically focuses when opened
         */
        $('#dotGraph').on('shown.bs.modal', function () {
            $('#dot').focus();
        });

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
