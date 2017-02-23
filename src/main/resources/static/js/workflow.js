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
        svgPanZoom('#graph', {
            zoomEnabled: true,
            controlIconsEnabled: true,
            customEventsHandler: eventHandler,
            preventMouseEventsDefault: false
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

        // When a table row is hovered over, highlight
        var tableBodyRow = $("tr").not('thead tr');
        tableBodyRow.hover(function() {
            getGraphBox(this).addClass("hover");
        }, function() {
            getGraphBox(this).removeClass("hover");
        });

        // When a table row is selected
        tableBodyRow.click(function() {
            getGraphBox(this).toggleClass("selected");
            $(this).toggleClass("selected");
        });

        /**
         * Gets the corresponding table row for a graph box
         * @param trElement The graph box element
         * @return The table row(s)
         */
        function getTableRow(gbElement) {
            // Title of the CWL element
            var elementTitle = $(gbElement).find("title").html();

            // Find corresponding table row and return
            return $("tr").filter(function() {
                return $(this).find("td:first").html() == elementTitle;
            });
        }

        // When a graph box is hovered over or clicked, highlight
        $(document).on({
            click: function() {
                getTableRow(this).toggleClass("selected");
                $(this).find("path").toggleClass("selected");
            },
            mouseenter: function() {
                getTableRow(this).addClass("hover");
                $(this).find("path").addClass("hover");
            },
            mouseleave: function() {
                getTableRow(this).removeClass("hover");
                $(this).find("path").removeClass("hover");
            }
        }, ".node");

    });