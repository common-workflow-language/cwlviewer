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

package org.commonwl.view.git;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Object to manage concurrent access to Git repositories
 */
@Component
public class GitSemaphore {

    private static Map<String, Integer> currentRepos = new HashMap<>();

    /**
     * Note that a thread will be accessing the repository
     * @param repoUrl The url of the repository
     * @return Whether the resource can be accessed safely
     * (no other threads are using it)
     */
    public synchronized boolean acquire(String repoUrl) {
        if (currentRepos.containsKey(repoUrl)) {
            currentRepos.put(repoUrl, currentRepos.get(repoUrl) + 1);
            return false;
        } else {
            currentRepos.put(repoUrl, 1);
            return true;
        }
    }

    /**
     * Release use of the shared resource
     * @param repoUrl The url of the repository
     */
    public synchronized void release(String repoUrl) {
        if (currentRepos.containsKey(repoUrl)) {
            int threadCountUsing = currentRepos.get(repoUrl);
            if (threadCountUsing > 1) {
                currentRepos.put(repoUrl, threadCountUsing - 1);
            } else {
                currentRepos.remove(repoUrl);
            }
        }
    }

}
