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
        'jquery.svg': 'jquery-svg/jquery.svg.min',
        'jquery.svgdom': 'jquery-svg/jquery.svgdom.min',
        'bootstrap.modal': 'bootstrap/js/modal',
        'bootstrap.tooltip': 'bootstrap/js/tooltip',
        'bootstrap.dropdown': 'bootstrap/js/dropdown',
        'svg-pan-zoom': 'svg-pan-zoom/dist/svg-pan-zoom.min',
        'hammerjs': 'hammerjs/hammer.min'
    },
    shim: {
        'jquery.svg': ['jquery'],
        'jquery.svgdom': ['jquery'],
        'bootstrap.modal': ['jquery'],
        'bootstrap.tooltip': ['jquery'],
        'bootstrap.dropdown': ['jquery']
    }
});

/**
 * Make the graph pannable and zoomable
 */
require(['jquery', 'bootstrap.modal', 'svg-pan-zoom', 'hammerjs', 'jquery.svg'],
    function ($, modal, svgPanZoom, Hammer) {
        /**
         * Custom hammer event handler to add mobile support
         * Based on example in svg-pan-zoom/demo/mobile.html
         */
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
                        initialScale = instance.getZoom();
                        instance.zoom(initialScale * ev.scale);
                    }
                    instance.zoom(initialScale * ev.scale)
                });

                // Handle double click
                this.hammer.on('doubletap', function(e){
                    goToSubworkflow(e.target.closest(".node"));
                });

                // Prevent moving the page on some devices when panning over SVG
                options.svgElement.addEventListener('touchmove', function(e){
                    e.preventDefault();
                });

            }, destroy: function(){
                this.hammer.destroy()
            }
        };

        // Loading from external URL needs to be done to enable events
        $("#graph").svg({
            loadURL: $("#graph").attr("data-svgurl"),
            onLoad: enablePanZoom
        });

        /**
         * Enable svg-pan-zoom on the graph
         */
        function enablePanZoom() {
            var graph = svgPanZoom('#graph svg', {
                zoomEnabled: true,
                dblClickZoomEnabled: false,
                controlIconsEnabled: true,
                customEventsHandler: eventHandler
            });

            // Add deselect to reset control
            $("#svg-pan-zoom-reset-pan-zoom").click(function() {
                $("polygon.selected, tr.selected").removeClass("selected");
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
        }

        /**
         * endsWith function to check suffix
         */
        if (typeof String.prototype.endsWith !== 'function') {
            String.prototype.endsWith = function(suffix) {
                return this.indexOf(suffix, this.length - suffix.length) !== -1;
            };
        }

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
                return $(this).text().endsWith(elementTitle);
            }).siblings("polygon");
        }

        /**
         * When a table row is hovered over/clicked, highlight
         */
        $("tr").not('thead tr').on({
            click: function(event) {
                var matchingGraphBox = getGraphBox(this);
                if (!event.ctrlKey) {
                    $("polygon.selected, tr.selected").not($(this))
                        .not(matchingGraphBox).removeClass("selected");
                }
                matchingGraphBox.toggleClass("selected");
                $(this).toggleClass("selected");
            },
            mouseenter: function() {
                getGraphBox(this).addClass("hover");
            },
            mouseleave: function() {
                getGraphBox(this).removeClass("hover");
            }
        });

        /**
         * Gets the corresponding table row for a graph box
         * @param gbElement The graph box element
         * @return The table row(s)
         */
        function getTableRow(gbElement) {
            // Title of the CWL element
            var elementTitle = $(gbElement).find("title").html();

            // Find corresponding table row and return
            return $("tr").filter(function() {
                return elementTitle.endsWith($(this).find("td:first").html());
            });
        }

        /**
         * Keep track of whether the graph is being dragged to
         * be able to disable click events during pan
         */
        var draggingGraph = false;
        $(document).on({
            mousedown: function() {
                draggingGraph = false;
            },
            mousemove: function() {
                draggingGraph = true;
            }
        }, "#graph");

        /**
         * Follow the link to a subworkflow if one exists
         * @param node The node of the visualisation
         */
        function goToSubworkflow(node) {
            var matchingTableRow = getTableRow(node);
            var subworkflowLink = $(matchingTableRow).find("a.subworkflow");
            if (subworkflowLink.length > 0) {
                location.href = subworkflowLink.attr("href");
            }
        }

        /**
         * When a graph box is hovered over/clicked, highlight
         */
        $(document).on({
            click: function(event) {
                if (!draggingGraph) {
                    var thisPolygon = $(this).find("polygon");
                    var matchingTableRow = getTableRow(this);
                    if (!event.ctrlKey) {
                        $("polygon.selected, tr.selected").not(thisPolygon)
                            .not(matchingTableRow).removeClass("selected");
                    }
                    matchingTableRow.toggleClass("selected");
                    thisPolygon.toggleClass("selected");
                }
            },
            dblclick: function() {
                // Follow link to subworkflow if possible
                goToSubworkflow(this);
            },
            mouseenter: function() {
                getTableRow(this).addClass("hover");
                $(this).find("polygon").addClass("hover");
            },
            mouseleave: function() {
                getTableRow(this).removeClass("hover");
                $(this).find("polygon").removeClass("hover");
            }
        }, ".node");

        /**
         * Stores an in+outlist style graph structure
         * Generated at time of first use
         */
        var graphModel = {};

        /**
         * Generates the graph model
         */
        function generateGraphModel() {
            var nodes = $(".node");

            // Add all node titles to the model
            nodes.each(function() {
                graphModel[$(this).find("title").parent().attr("id")] = {
                    inList: [],
                    outList: []
                }
            });

            // Add all links to the model
            $(".edge").each(function() {
                var edgeText = $(this).find("title").text();
                var toFrom = edgeText.split("->");
                var to = nodes.filter(function() {
                    return $(this).find("title").text() === toFrom[0];
                }).attr("id");
                var from = nodes.filter(function() {
                    return $(this).find("title").text() === toFrom[1];
                }).attr("id");
                graphModel[from].inList.push(to);
                graphModel[to].outList.push(from);
            });
        }

        /**
         * Recursively select all the parents or children of a node
         */
        function expandSelection(root, listName) {
            var rootID = root.attr("id");
            for (var i = 0; i < graphModel[rootID][listName].length; i++) {
                var next = $("#" + graphModel[rootID][listName][i]);
                if (!next.find("polygon").hasClass("selected")) {
                    next.find("polygon").addClass("selected");
                    getTableRow(next).addClass("selected");
                    expandSelection(next, listName);
                }
            }
        }

        $("#selectChildren").click(function() {
            if ($.isEmptyObject(graphModel)) {
                generateGraphModel();
            }
            $("polygon.selected").each(function() {
                expandSelection($(this).parent(), "outList");
            });
        });

        $("#selectParents").click(function() {
            if ($.isEmptyObject(graphModel)) {
                generateGraphModel();
            }
            $("polygon.selected").each(function() {
                expandSelection($(this).parent(), "inList");
            });
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
            var src = "data:text/vnd.graphviz;charset=utf-8," + encodeURIComponent(dotGraph);

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
        /**
         * AJAX function to add download link to page if generated
         */
        function getDownloadLink() {
            $.ajax({
                type: 'HEAD',
                url: $('#download').attr('href'),
                dataType: "json",
                success: function () {
                    // Hide generating, show link
                    $("#generating").addClass("hide");
                    $("#generated").removeClass("hide");
                },
                error: function () {
                    // Show generating, hide link
                    $("#generated").addClass("hide");
                    $("#generating").removeClass("hide");

                    // Retry in 5 seconds if still not generated
                    setTimeout(function () {
                        getDownloadLink();
                    }, 5000)
                }
            });
        }

        getDownloadLink();
    });

/**
 * Bootstrap tooltips
 */
require(['jquery', 'bootstrap.tooltip', 'bootstrap.dropdown'],
    function ($) {
        // Alterative notation as only a single data-toggle attribute is allowed
        $('[data-tooltip="true"]').tooltip();
    });