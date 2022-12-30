package org.commonwl.view.util;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.fs.BundleFileSystem;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for <code>FileUtils</code>.
 *
 * @since 1.4.6
 */
public class FileUtilsTest {

    @Rule
    public MockitoRule initRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    // --- git files

    @ParameterizedTest
    @NullSource
    public void testNullRepository(Git repository) throws IOException {
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            FileUtils.deleteGitRepository(repository);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testRepositoryNullRepository() throws IOException {
        Git repository = mock(Git.class);
        when(repository.getRepository()).thenReturn(null);
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            FileUtils.deleteGitRepository(repository);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testRepositoryRepositoryNullDirectory() throws IOException {
        Git repository = mock(Git.class);
        Repository repository1 = mock(Repository.class);
        when(repository.getRepository()).thenReturn(repository1);
        when(repository1.getDirectory()).thenReturn(null);
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            FileUtils.deleteGitRepository(repository);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testRepositoryRepositoryDirectoryDoesNotExist() throws IOException {
        Git repository = mock(Git.class);
        Repository repository1 = mock(Repository.class);
        when(repository.getRepository()).thenReturn(repository1);
        File doesNotExist = new File("/tmp/tmp/tmp/tmp/1/2/3/4/5");
        when(repository1.getDirectory()).thenReturn(doesNotExist);
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            FileUtils.deleteGitRepository(repository);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testRepositoryRepositoryDirectory() throws IOException {
        Git repository = mock(Git.class);
        Repository repository1 = mock(Repository.class);
        when(repository.getRepository()).thenReturn(repository1);
        File gitRepository = temporaryFolder.newFile(".git");
        when(repository1.getDirectory()).thenReturn(gitRepository);
        assertTrue(gitRepository.exists());
        FileUtils.deleteGitRepository(repository);
        assertFalse(gitRepository.exists());
    }

    // --- bundle temporary files

    @ParameterizedTest
    @NullSource
    public void testBundleTemporaryDirectoryNullBundle(Bundle bundle) throws IOException {
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            FileUtils.deleteBundleTemporaryDirectory(bundle);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testBundleTemporaryDirectoryNoParentFileName() throws IOException {
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            Bundle bundle = mock(Bundle.class);
            BundleFileSystem fileSystem = mock(BundleFileSystem.class);
            when(bundle.getFileSystem()).thenReturn(fileSystem);
            when(fileSystem.getSource()).thenReturn(Path.of("/does-not-exist-12345"));
            FileUtils.deleteBundleTemporaryDirectory(bundle);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testBundleTemporaryDirectoryDoesNotExist() throws IOException {
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            Bundle bundle = mock(Bundle.class);
            BundleFileSystem fileSystem = mock(BundleFileSystem.class);
            when(bundle.getFileSystem()).thenReturn(fileSystem);
            when(fileSystem.getSource()).thenReturn(Path.of("/does/not/exist/1/2/3/does-not-exist-12345"));
            FileUtils.deleteBundleTemporaryDirectory(bundle);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testBundleTemporaryDirectoryNotEmpty() throws IOException {
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            Bundle bundle = mock(Bundle.class);
            BundleFileSystem fileSystem = mock(BundleFileSystem.class);
            when(bundle.getFileSystem()).thenReturn(fileSystem);
            final String hash = "bundle.zip";
            File bundleTemporaryDirectory = temporaryFolder.newFile(hash);
            when(fileSystem.getSource()).thenReturn(bundleTemporaryDirectory.toPath());
            FileUtils.deleteBundleTemporaryDirectory(bundle);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testBundleTemporaryDirectory() throws IOException {
        Bundle bundle = mock(Bundle.class);
        BundleFileSystem fileSystem = mock(BundleFileSystem.class);
        when(bundle.getFileSystem()).thenReturn(fileSystem);
        final String hash = "bundle.zip";
        File bundleTemporaryDirectory = temporaryFolder.newFile(hash);
        // We want an empty temporary directory, so we can delete the bundle.zip file
        // (in real-life it will have been moved from the temporary location to the
        // bundles' directory).
        org.apache.commons.io.FileUtils.forceDelete(bundleTemporaryDirectory);
        when(fileSystem.getSource()).thenReturn(bundleTemporaryDirectory.toPath());
        assertTrue(bundleTemporaryDirectory.getParentFile().exists());
        FileUtils.deleteBundleTemporaryDirectory(bundle);
        assertFalse(bundleTemporaryDirectory.exists());
    }

    // --- bundle parent directory

    @ParameterizedTest
    @NullSource
    public void testBundleParentDirectoryNullBundle(Bundle bundle) throws IOException {
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            FileUtils.deleteBundleParentDirectory(bundle);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testBundleParentDirectoryDoesNotExist() throws IOException {
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            Bundle bundle = mock(Bundle.class);
            when(bundle.getSource()).thenReturn(Path.of("/does/not/exist/1/2/3/does-not-exist-12345"));
            FileUtils.deleteBundleParentDirectory(bundle);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testBundleParentDirectoryWithTooManyFiles() throws IOException {
        try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked = mockStatic(org.apache.commons.io.FileUtils.class)) {
            fileUtilsMocked.when(
                    () -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any())
            ).thenAnswer((Answer<Void>) invocation -> null);
            Bundle bundle = mock(Bundle.class);
            final String hash = "bundle.zip";
            File bundleTemporaryDirectory = temporaryFolder.newFile(hash);
            for (int i = 0; i < 3; i++) {
                temporaryFolder.newFile("file-" + i);
            }
            when(bundle.getSource()).thenReturn(bundleTemporaryDirectory.toPath());
            FileUtils.deleteBundleParentDirectory(bundle);
            fileUtilsMocked.verifyNoInteractions();
        }
    }

    @Test
    public void testBundleParentDirectory() throws IOException {
        Bundle bundle = mock(Bundle.class);
        final String hash = "bundle.zip";
        File bundleTemporaryDirectory = temporaryFolder.newFile(hash);
        when(bundle.getSource()).thenReturn(bundleTemporaryDirectory.toPath());
        assertTrue(bundleTemporaryDirectory.getParentFile().exists());
        FileUtils.deleteBundleParentDirectory(bundle);
        assertFalse(bundleTemporaryDirectory.exists());
    }
}
