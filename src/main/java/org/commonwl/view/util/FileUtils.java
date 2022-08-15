package org.commonwl.view.util;

import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;

/**
 * File utilities for CWL Viewer.
 *
 * <p>Uses other utilities, such as Apache Commons IO's {@code FileUtils}, but
 * with refinements specific for CWL Viewer (e.g. handling Git repositories).</p>
 */
public class FileUtils {

    private FileUtils() {}

    public static void deleteGitRepository(Git repo) throws IOException {
        if (
                repo != null &&
                repo.getRepository() != null &&
                repo.getRepository().getDirectory() != null &&
                repo.getRepository().getDirectory().exists()
        ) {
            // This is literally the git directory, i.e. /some/hierarchy/repository/.git,
            // but we want to delete its parent directory.
            File gitDirectory = repo.getRepository().getDirectory();
            org.apache.commons.io.FileUtils.forceDelete(gitDirectory.getParentFile());
        }
    }
}
