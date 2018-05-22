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

import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowPermalinkController;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    /**
     * Ordered list of formats as presented on Workflow page and supported for
     * content negotiation.
     * 
     * @see Workflow#getPermalink(Format)
     * @see WorkflowPermalinkController
     *
     */
    public static enum Format {
        // Browser
        html(MediaType.TEXT_HTML),
        // API
        json(MediaType.APPLICATION_JSON),
        // RDF
        turtle("text/turtle"), jsonld("application/ld+json"), rdfxml("application/rdf+xml"),
        // Images
        svg("image/svg+xml"), png(MediaType.IMAGE_PNG), dot("text/vnd+graphviz"),
        // Archives
        zip("application/zip"), ro("application/vnd.wf4ever.robundle+zip"),
        // raw redirects
        yaml("text/x-yaml"), raw(MediaType.APPLICATION_OCTET_STREAM);

        private final MediaType mediaType;

        Format(MediaType mediaType) {
            this.mediaType = mediaType;
        }

        Format(String mediaType) {
            this.mediaType = parseMediaType(mediaType);
        }

        public MediaType mediaType() {
            return mediaType;
        }
    }

    /**
     * Allows the use of the format query parameter to be used instead of the Accept
     * HTTP header
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        ContentNegotiationConfigurer c = configurer.favorParameter(true);
        for (Format f : Format.values()) {
            c = c.mediaType(f.name(), f.mediaType());
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
	registry.addMapping("/**").exposedHeaders("Location");  // .setMaxAge(Long.MAX_VALUE)
    }
}
