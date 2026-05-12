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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.commonwl.view.util.LicenseUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Configuration
public class GitConfig {

  @Bean
  @Scope(WebApplicationContext.SCOPE_APPLICATION)
  public Map<String, String> licenseVocab() {
    RestTemplate restTemplate = new RestTemplate();
    ObjectNode jsonLicenses =
        restTemplate.getForObject(LicenseUtils.SPDX_LICENSES_JSON_URL, ObjectNode.class);
    if (jsonLicenses == null) {
      throw new GitLicenseException(
          "Failed to load SPDX licenses from " + LicenseUtils.SPDX_LICENSES_JSON_URL);
    }
    Map<String, String> licenseMap = new HashMap<>();
    for (JsonNode license : jsonLicenses.withArray("licenses")) {
      String spdxURL = LicenseUtils.SPDX_LICENSES_PREFIX + license.get("licenseId").asString();
      for (JsonNode alias : license.withArray("seeAlso")) {
        licenseMap.put(
            StringUtils.stripEnd(alias.asString().replace("http://", "https://"), "/"), spdxURL);
      }
    }
    return licenseMap;
  }
}
