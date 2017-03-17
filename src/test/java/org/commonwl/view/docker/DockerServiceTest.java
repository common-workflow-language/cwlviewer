package org.commonwl.view.docker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DockerServiceTest {

    /**
     * Test conversion from docker pull tag to dockerhub URL
     */
    @Test
    public void getDockerHubURL() throws Exception {

        String test1 = DockerService.getDockerHubURL("stain/cwlviewer");
        assertEquals("https://hub.docker.com/r/stain/cwlviewer", test1);

        String test2 = DockerService.getDockerHubURL("rabix/lobSTR");
        assertEquals("https://hub.docker.com/r/rabix/lobSTR", test2);

        String test3 = DockerService.getDockerHubURL("ubuntu");
        assertEquals("https://hub.docker.com/r/_/ubuntu", test3);

    }

}