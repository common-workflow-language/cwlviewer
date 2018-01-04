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

package org.commonwl.view;

import static org.springframework.http.MediaType.parseMediaType;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    /**
     * Ordered list of formats as presented on Workflow page - must match the
     * .mediaType() strings below.
     *
     */
    public static enum formats {
        html, json, turtle, jsonld, rdfxml, svg, png, dot, zip, ro, yaml, raw
    }

    /**
     * Allows the use of the format query parameter to be used
     * instead of the Accept HTTP header
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorParameter(true)
                // Browser
                .mediaType("html", MediaType.TEXT_HTML)
                // API
            .mediaType("json", MediaType.APPLICATION_JSON)
                // RDF
            .mediaType("turtle", parseMediaType("text/turtle"))
            .mediaType("jsonld", parseMediaType("application/ld+json"))
            .mediaType("rdfxml", parseMediaType("application/rdf+xml"))
                // Images
            .mediaType("svg", parseMediaType("image/svg+xml"))
            .mediaType("png", MediaType.IMAGE_PNG)
                .mediaType("dot", parseMediaType("text/vnd+graphviz"))
                // Archives
                .mediaType("zip", parseMediaType("application/zip"))
            .mediaType("ro", parseMediaType("application/vnd.wf4ever.robundle+zip"))
                // raw redirects
            .mediaType("yaml", parseMediaType("text/x-yaml"))
            .mediaType("raw", MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
	registry.addMapping("/**");  // .setMaxAge(Long.MAX_VALUE)
    }
}
