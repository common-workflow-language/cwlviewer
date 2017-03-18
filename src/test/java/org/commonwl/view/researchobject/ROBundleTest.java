package org.commonwl.view.researchobject;

import org.apache.commons.io.FileUtils;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Manifest;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.eclipse.egit.github.core.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class ROBundleTest {

    /**
     * Use a temporary directory for testing
     */
    @Rule
    public TemporaryFolder roBundleFolder = new TemporaryFolder();

    /**
     * Generate a Research Object bundle from lobstr-v1 and check it
     */
    @Test
    public void generateAndSaveROBundle() throws Exception {

        // Get mock Github service
        GitHubService mockGithubService = getMock();

        // Create new RO bundle
        GithubDetails lobSTRv1Details = new GithubDetails("common-workflow-language", "workflows",
                "933bf2a1a1cce32d88f88f136275535da9df0954", "workflows/lobSTR");
        ROBundle bundle = new ROBundle(mockGithubService, lobSTRv1Details, "CWL Viewer",
                "https://view.commonwl.org", 5242880);

        // Check bundle exists
        assertNotNull(bundle.getBundle());

        // Check basic manifest metadata
        Manifest manifest = bundle.getBundle().getManifest();
        assertEquals("CWL Viewer", manifest.getCreatedBy().getName());
        assertEquals("https://view.commonwl.org", manifest.getCreatedBy().getUri().toString());
        assertEquals("Mark Robinson", manifest.getAuthoredBy().get(0).getName());
        assertEquals(10, manifest.getAggregates().size());

        // Save and check it exists in the temporary folder
        bundle.saveToFile(roBundleFolder.getRoot().toPath());
        File[] fileList =  roBundleFolder.getRoot().listFiles();
        assertTrue(fileList.length == 1);
        for (File ro : fileList) {
            assertTrue(ro.getName().endsWith(".zip"));
            Bundle savedBundle = Bundles.openBundle(ro.toPath());
            assertNotNull(savedBundle);
        }

    }

    /**
     * Get a mock Github service redirecting file downloads to file system
     * and providing translation from the file system to Github API returns
     * @return The constructed mock object
     */
    private GitHubService getMock() throws Exception {

        GitHubService mockGithubService = Mockito.mock(GitHubService.class);
        Answer fileAnswer = new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GithubDetails details = (GithubDetails) args[0];
                File workflowFile = new File("src/test/resources/cwl/lobstr-v1/"
                        + details.getPath().replace("workflows/lobSTR/", ""));
                return FileUtils.readFileToString(workflowFile);
            }
        };
        when(mockGithubService.downloadFile(anyObject())).thenAnswer(fileAnswer);
        when(mockGithubService.downloadFile(anyObject(), anyObject())).thenAnswer(fileAnswer);

        Answer contentsAnswer = new Answer<List<RepositoryContents>>() {
            @Override
            public List<RepositoryContents> answer(InvocationOnMock invocation) throws Throwable {
                File[] fileList = new File("src/test/resources/cwl/lobstr-v1/").listFiles();

                // Add all files from lobstr-v1 directory
                List<RepositoryContents> returnList = new ArrayList<>();
                for (File thisFile : fileList) {
                    if (thisFile.isFile()) {
                        RepositoryContents singleFile = new RepositoryContents();
                        singleFile.setType(GitHubService.TYPE_FILE);
                        singleFile.setName(thisFile.getName());
                        singleFile.setPath("workflows/lobSTR/" + thisFile.getName());
                        returnList.add(singleFile);
                    }
                }

                return returnList;
            }
        };
        when(mockGithubService.getContents(anyObject())).thenAnswer(contentsAnswer);

        Answer commitsAnswer = new Answer<List<RepositoryCommit>>() {
            @Override
            public List<RepositoryCommit> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GithubDetails details = (GithubDetails) args[0];

                // Make up a commit for the file and return it
                List<RepositoryCommit> commitList = new ArrayList<>();
                RepositoryCommit commit = new RepositoryCommit();

                User author = new User();
                author.setName("Mark Robinson");
                author.setHtmlUrl("https://github.com/MarkRobbo");
                commit.setAuthor(author);
                commit.setSha("933bf2a1a1cce32d88f88f136275535da9df0954");
                commit.setCommit(new Commit().setAuthor(new CommitUser().setName("Mark Robinson")));
                commitList.add(commit);

                return commitList;
            }
        };
        when(mockGithubService.getCommits(anyObject())).thenAnswer(commitsAnswer);

        return mockGithubService;
    }

}