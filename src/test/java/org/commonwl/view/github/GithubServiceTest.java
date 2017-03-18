package org.commonwl.view.github;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GithubServiceTest {


    /**
     * Create a service to test
     */
    @Autowired
    private GitHubService githubService;

    /**
     * Details can be extracted from a full Github CWL file URL
     */
    @Test
    public void detailsFromFileURLFull() throws Exception {

        GithubDetails details = githubService.detailsFromFileURL("https://github.com/nlesc-sherlock/deeplearning/blob/master/CWLworkflow/pipeline.cwl");
        assertNotNull(details);
        assertEquals("nlesc-sherlock", details.getOwner());
        assertEquals("deeplearning", details.getRepoName());
        assertEquals("master", details.getBranch());
        assertEquals("CWLworkflow/pipeline.cwl", details.getPath());

    }

    /**
     * Github CWL file URL at the repository root
     */
    @Test
    public void detailsFromFileURLAtBase() throws Exception {

        GithubDetails details = githubService.detailsFromFileURL("https://github.com/genome/arvados_trial/blob/master/pipeline.cwl");
        assertNotNull(details);
        assertEquals("genome", details.getOwner());
        assertEquals("arvados_trial", details.getRepoName());
        assertEquals("master", details.getBranch());
        assertEquals("pipeline.cwl", details.getPath());

    }

    /**
     * Details can be extracted from a full Github directory URL
     */
    @Test
    public void detailsFromDirURLFull() throws Exception {

        GithubDetails details = githubService.detailsFromDirURL("https://github.com/common-workflow-language/workflows/tree/visu/workflows/compile");
        assertNotNull(details);
        assertEquals("common-workflow-language", details.getOwner());
        assertEquals("workflows", details.getRepoName());
        assertEquals("visu", details.getBranch());
        assertEquals("workflows/compile", details.getPath());

    }

    /**
     * No path included in the directory URL
     */
    @Test
    public void detailsFromDirURLNoPath() throws Exception {

        GithubDetails details = githubService.detailsFromDirURL("https://github.com/OBF/GSoC/tree/d46ce365f1a10c4c4d6b0caed51c6f64b84c2f63");
        assertNotNull(details);
        assertEquals("OBF", details.getOwner());
        assertEquals("GSoC", details.getRepoName());
        assertEquals("d46ce365f1a10c4c4d6b0caed51c6f64b84c2f63", details.getBranch());
        assertEquals("/", details.getPath());

    }

    /**
     * No branch or path included in the directory URL
     */
    @Test
    public void detailsFromDirURLNoBranchPath() throws Exception {

        GithubDetails details = githubService.detailsFromDirURL("https://github.com/common-workflow-language/cwlviewer");
        assertNotNull(details);
        assertEquals("common-workflow-language", details.getOwner());
        assertEquals("cwlviewer", details.getRepoName());
        assertEquals("master", details.getBranch());
        assertEquals("/", details.getPath());

    }

}