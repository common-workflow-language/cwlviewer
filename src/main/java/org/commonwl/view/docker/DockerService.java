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

package org.commonwl.view.docker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles DockerHub functionality
 */
public class DockerService {

    // URL validation for docker pull id
    private static final String DOCKERHUB_ID_REGEX = "^([0-9a-z]{4,30})(?:\\/([a-zA-Z0-9_-]+))?(?:\\:[a-zA-Z0-9_-]+)?$";
    private static final Pattern dockerhubPattern = Pattern.compile(DOCKERHUB_ID_REGEX);

    /**
     * Get a DockerHub URL from a dockerPull ID
     * @param dockerPull The repository and ID as a string
     * @return A docker hub link
     */
    public static String getDockerHubURL(String dockerPull) {
        Matcher m = dockerhubPattern.matcher(dockerPull);
        if (m.find()) {
            if (m.group(2) == null) {
                return "https://hub.docker.com/r/_/" + m.group(1);
            } else {
                return "https://hub.docker.com/r/" + m.group(1) + "/" + m.group(2);
            }
        }
        return null;
    }

}
