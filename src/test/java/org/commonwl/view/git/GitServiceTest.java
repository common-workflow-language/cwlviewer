package org.commonwl.view.git;

import org.eclipse.jgit.api.Git;
import org.junit.Test;

import java.io.File;

/**
 * Created by mark on 08/08/17.
 */
public class GitServiceTest {

    @Test
    public void name() throws Exception {
        GitService gitService = new GitService(new File("/tmp").toPath(), true);

        Git test = Git.open(new File("/home/mark/workflows/.git"));
        String content = gitService.getFile(test.getRepository(), "aab378267a528c67f634e421be40ab6f19f3b078");

        System.out.println(content);
    }

}
