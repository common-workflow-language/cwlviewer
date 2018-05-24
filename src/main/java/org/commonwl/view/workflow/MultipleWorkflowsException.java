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

package org.commonwl.view.workflow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.commonwl.view.WebConfig.Format;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when multiple workflows exist for the request
 */
@ResponseStatus(value = HttpStatus.MULTIPLE_CHOICES)
public class MultipleWorkflowsException extends RuntimeException {

    private final Collection<Workflow> matches;

    public MultipleWorkflowsException(Workflow match) {
        this(Collections.singleton(match));
    }

    public MultipleWorkflowsException(Collection<Workflow> matches) {
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("MultipleWorkflowsException, but empty list of workflows");
        }
        this.matches = matches;
    }

    // Always CRLF in text/uri-list
    private final String CRLF = "\r\n";

    public String getRawPermalink() {
        // all raw URIs should be the same without ?part=
        return matches.stream().findAny().get().getPermalink(Format.raw);
    }

    /**
     * Generate a text/uri-list of potential representations/redirects
     * 
     * @see https://www.iana.org/assignments/media-types/text/uri-list
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("## Multiple workflow representations found");
        sb.append(CRLF);
        sb.append("# ");
        sb.append(CRLF);

        sb.append("# ");
        sb.append(Format.raw.mediaType());
        sb.append(CRLF);
        sb.append(getRawPermalink());
        sb.append(CRLF);

        Set<String> seen = new HashSet<>();
        // For each workflow, link to each remaining format
        for (Workflow w : matches) {
            if (!seen.add(w.getIdentifier())) {
                // Skip permalink duplicates
                continue;
            }
            sb.append("#");
            sb.append(CRLF);
            sb.append("# ");
            sb.append(w.getIdentifier());
            sb.append(CRLF);
            for (Format f : Format.values()) {
                if (f == Format.raw) {
                    // Already did that one above
                    continue;
                }
                sb.append("#   ");
                sb.append(f.mediaType());
                sb.append(CRLF);
                sb.append(w.getPermalink(f));
                sb.append(CRLF);
            }
        }
        return sb.toString();
    }

}