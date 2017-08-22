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

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.springframework.http.MediaType.parseMediaType;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    /**
     * Allows the use of the format query parameter to be used
     * instead of the Accept HTTP header
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(true).favorParameter(false).useJaf(true)
            .mediaType("html", MediaType.TEXT_HTML)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("turtle", parseMediaType("text/turtle"))
            .mediaType("jsonld", parseMediaType("application/ld+json"))
            .mediaType("rdfxml", parseMediaType("application/rdf+xml"))
            .mediaType("svg", parseMediaType("image/svg+xml"))
            .mediaType("png", MediaType.IMAGE_PNG)
            .mediaType("ro", parseMediaType("application/vnd.wf4ever.robundle+zip"))
            .mediaType("zip", parseMediaType("application/zip"))
            .mediaType("dot", parseMediaType("text/vnd+graphviz"))
            .mediaType("yaml", parseMediaType("text/x-yaml"))
            .mediaType("raw", MediaType.APPLICATION_OCTET_STREAM);
    }
}