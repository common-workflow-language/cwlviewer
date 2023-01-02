package org.commonwl.view.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.commonwl.view.util.LicenseUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

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
      String spdxURL = license.get("reference").asText();
      for (JsonNode alias : license.withArray("seeAlso")) {
        licenseMap.put(
            StringUtils.stripEnd(alias.asText().replace("http://", "https://"), "/"), spdxURL);
      }
    }
  }
}
