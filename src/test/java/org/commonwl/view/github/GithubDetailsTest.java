package org.commonwl.view.github;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GithubDetailsTest {

    /**
     * Branch getter, should default to "master" if null
     */
    @Test
    public void getBranch() throws Exception {
        GithubDetails details1 = new GithubDetails("owner", "repoName", "testbranch", "path/within/workflow.cwl");
        assertEquals("testbranch", details1.getBranch());

        // Null branch should default to master
        GithubDetails details2 = new GithubDetails("owner", "repoName", null, null);
        assertEquals("master", details2.getBranch());
    }

    /**
     * Path getter, should default to / if null
     */
    @Test
    public void getPath() throws Exception {
        GithubDetails details1 = new GithubDetails("owner", "repoName", "branch", "subdir/");
        assertEquals("subdir/", details1.getPath());

        GithubDetails details2 = new GithubDetails("owner", "repoName", "branch", "test/directory/structure.cwl");
        assertEquals("test/directory/structure.cwl", details2.getPath());

        GithubDetails details3 = new GithubDetails("owner", "repoName", null, null);
        assertEquals("/", details3.getPath());
    }

    /**
     * Construct a URL from Github details
     */
    @Test
    public void getURL() throws Exception {
        GithubDetails details1 = new GithubDetails("owner", "repoName", "branch", "path/within/structure.cwl");
        assertEquals("https://github.com/owner/repoName/tree/branch/path/within/structure.cwl", details1.getURL());
        assertEquals("https://github.com/owner/repoName/tree/overrideBranch/path/within/structure.cwl",
                details1.getURL("overrideBranch"));

        GithubDetails details2 = new GithubDetails("owner", "repoName", "branch", null);
        assertEquals("https://github.com/owner/repoName/tree/branch", details2.getURL());
        assertEquals("https://github.com/owner/repoName/tree/differentBranch", details2.getURL("differentBranch"));

    }

}