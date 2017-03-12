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
            if (m.group(1).isEmpty()) {
                return "https://hub.docker.com/r/_/" + m.group(2);
            } else {
                return "https://hub.docker.com/r/" + m.group(1) + "/" + m.group(2);
            }
        }
        return null;
    }

}
