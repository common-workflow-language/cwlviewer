package org.commonwl.view.util;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.fs.BundleFileSystem;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * File utilities for CWL Viewer.
 *
 * <p>Uses other utilities, such as Apache Commons IO's {@code FileUtils}, but
 * with refinements specific for CWL Viewer (e.g. handling Git repositories).</p>
 *
 * @since 1.4.5
 */
public class FileUtils {

    private FileUtils() {}

    /**
     * Deletes the directory of a git repository. Note that the <code>Git</code> object
     * contains a repository with a directory, but this directory points to the <pre>.git</pre>
     * directory. This method will delete the parent of the <pre>.git</pre> directory,
     * which corresponds to the cloned folder with the source code from git.
     *
     * @param repo Git repository object
     * @throws IOException if it fails to delete the Git repository directory
     * @since 1.4.5
     */
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

    /**
     * Deletes the bundle temporary directory.
     *
     * <p>The <code>Bundle</code> object is an Apache Taverna object. Be careful
     * as it acts as a virtual file system, with a root directory (normally "/"),
     * but that's an abstract representation of the bundle files.</p>
     *
     * <p>This method looks inside the bundle for the bundle source directory.
     * This directory contains a hash in its name structure. Apache Taverna creates
     * temporary directories in <pre>java.io.tmpdir</pre> using this same
     * hash. This method will delete any directory matching that hash in the
     * temporary directory.</p>
     *
     * <p>NOTE: the directory deleted is expected to be empty. It is a left over
     * by Apache Taverna, and failing to delete it shouldn't be a major issue
     * (other than using an inode to represent the file in the file system). And
     * also note that we are not deleting the parent directory, since the
     * left-over directory is always stored in the system temporary directory,
     * while the ROBundle with the hash is stored in the Spring configured
     * directory for bundles.</p>
     *
     * @param bundle A bundle object
     * @throws IOException if it fails to delete the bundle temporary directory
     * @since 1.4.5
     */
    public static void deleteBundleTemporaryDirectory(Bundle bundle) throws IOException {
        // The RoBundleService#saveToFile call will delegate to Apache Taverna's
        // Bundles.closeAndSaveBundle, which empties the bundle temporary
        // directory without deleting the directory itself. The following call is
        // just for cleaning it up.
        if (bundle != null) {
            BundleFileSystem fs = (BundleFileSystem) bundle.getFileSystem();
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            // The file system source will be something like /tmp/bundles_dir/hash/bundle.zip,
            // and we want /tmp/hash to be deleted. N.B. Apache Taverna uses the temporary directory
            // for the temporary bundles' directory, not the bundles file specified by Spring.
            Path parent = fs.getSource().toAbsolutePath().getParent().getFileName();
            if (parent != null) {
                String bundleTmpDirName = parent.toString();
                File bundleTmpDir = new File(tmpDir, bundleTmpDirName);
                if (bundleTmpDir.exists() && bundleTmpDir.isDirectory()) {
                    File[] files = bundleTmpDir.listFiles();
                    // We expect the file to be empty, else we better avoid deleting it.
                    if (files != null && files.length == 0) {
                        org.apache.commons.io.FileUtils.forceDelete(bundleTmpDir);
                    }
                }
            }
        }
    }

    /**
     * Used to delete the directory with a robundle ZIP file. It must be used
     * when the system fails to generate a bundle file. Apache Taverna's API
     * may have already created the Bundle ZIP.
     *
     * <p>Normally this file is stored in a newly created directory (e.g.
     * <pre>/tmp/bundles/bundle-hash-1234/robundle.zip</pre>), and when we fail
     * to successfully create the bundle, it is best to remove this file to
     * prevent disk space issues (especially if we end up with duplicated
     * bundles).</p>
     *
     * @param bundle A bundle object
     * @throws IOException if it fails to delete the bundle temporary directory
     * @since 1.4.5
     */
    public static void deleteBundleParentDirectory(Bundle bundle) throws IOException {
        if (bundle != null) {
            Path bundleSource = bundle.getSource();
            File parent = bundleSource.getParent().toFile();
            if (parent.exists() && parent.isDirectory()) {
                File[] files = parent.listFiles();
                // We expect the directory to be empty or contain just one file (the ZIP bundle,
                // since the bundle files are stored temporarily in another temporary directory).
                if (files != null && files.length <= 1) {
                    org.apache.commons.io.FileUtils.forceDelete(parent);
                }
            }
        }
    }
}
