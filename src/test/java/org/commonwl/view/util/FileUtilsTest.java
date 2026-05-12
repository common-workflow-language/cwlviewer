package org.commonwl.view.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.fs.BundleFileSystem;
import org.commonwl.view.git.GitDetails;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * Tests for <code>FileUtils</code>.
 *
 * @since 1.4.6
 */
@ExtendWith(MockitoExtension.class)
public class FileUtilsTest {

  @TempDir public Path temporaryFolder;

  // --- git files

  @ParameterizedTest
  @NullSource
  void testNullRepository(Git repository) throws IOException {
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      FileUtils.deleteGitRepository(repository);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testRepositoryNullRepository() throws IOException {
    Git repository = mock(Git.class);
    when(repository.getRepository()).thenReturn(null);
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      FileUtils.deleteGitRepository(repository);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testRepositoryRepositoryNullDirectory() throws IOException {
    Git repository = mock(Git.class);
    Repository repository1 = mock(Repository.class);
    when(repository.getRepository()).thenReturn(repository1);
    when(repository1.getDirectory()).thenReturn(null);
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      FileUtils.deleteGitRepository(repository);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testRepositoryRepositoryDirectoryDoesNotExist() throws IOException {
    Git repository = mock(Git.class);
    Repository repository1 = mock(Repository.class);
    when(repository.getRepository()).thenReturn(repository1);
    File doesNotExist = new File("/tmp/tmp/tmp/tmp/1/2/3/4/5");
    when(repository1.getDirectory()).thenReturn(doesNotExist);
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      FileUtils.deleteGitRepository(repository);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testRepositoryRepositoryDirectory() throws IOException {
    Git repository = mock(Git.class);
    Repository repository1 = mock(Repository.class);
    when(repository.getRepository()).thenReturn(repository1);
    Path gitRepository = Files.createFile(temporaryFolder.resolve(".git"));
    when(repository1.getDirectory()).thenReturn(gitRepository.toFile());
    assertTrue(Files.exists(gitRepository));
    FileUtils.deleteGitRepository(repository);
    assertFalse(Files.exists(gitRepository));
  }

  // --- bundle temporary files

  @ParameterizedTest
  @NullSource
  void testBundleTemporaryDirectoryNullBundle(Bundle bundle) throws IOException {
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      FileUtils.deleteBundleTemporaryDirectory(bundle);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testBundleTemporaryDirectoryNoParentFileName() throws IOException {
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      Bundle bundle = mock(Bundle.class);
      BundleFileSystem fileSystem = mock(BundleFileSystem.class);
      when(bundle.getFileSystem()).thenReturn(fileSystem);
      when(fileSystem.getSource()).thenReturn(Path.of("/does-not-exist-12345"));
      FileUtils.deleteBundleTemporaryDirectory(bundle);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testBundleTemporaryDirectoryDoesNotExist() throws IOException {
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      Bundle bundle = mock(Bundle.class);
      BundleFileSystem fileSystem = mock(BundleFileSystem.class);
      when(bundle.getFileSystem()).thenReturn(fileSystem);
      when(fileSystem.getSource())
          .thenReturn(Path.of("/does/not/exist/1/2/3/does-not-exist-12345"));
      FileUtils.deleteBundleTemporaryDirectory(bundle);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testBundleTemporaryDirectoryNotEmpty() throws IOException {
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      Bundle bundle = mock(Bundle.class);
      BundleFileSystem fileSystem = mock(BundleFileSystem.class);
      when(bundle.getFileSystem()).thenReturn(fileSystem);
      final String hash = "bundle.zip";
      Path bundleTemporaryDirectory = Files.createFile(temporaryFolder.resolve(hash));
      when(fileSystem.getSource()).thenReturn(bundleTemporaryDirectory);
      FileUtils.deleteBundleTemporaryDirectory(bundle);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testBundleTemporaryDirectory() throws IOException {
    Bundle bundle = mock(Bundle.class);
    BundleFileSystem fileSystem = mock(BundleFileSystem.class);
    when(bundle.getFileSystem()).thenReturn(fileSystem);
    final String hash = "bundle.zip";
    Path bundleTemporaryDirectory = Files.createFile(temporaryFolder.resolve(hash));
    // We want an empty temporary directory, so we can delete the bundle.zip file
    // (in real-life it will have been moved from the temporary location to the
    // bundles' directory).
    Files.deleteIfExists(bundleTemporaryDirectory);
    when(fileSystem.getSource()).thenReturn(bundleTemporaryDirectory);
    assertTrue(Files.exists(bundleTemporaryDirectory.getParent()));
    FileUtils.deleteBundleTemporaryDirectory(bundle);
    assertFalse(Files.exists(bundleTemporaryDirectory));
  }

  // --- bundle parent directory

  @ParameterizedTest
  @NullSource
  void testBundleParentDirectoryNullBundle(Bundle bundle) throws IOException {
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      FileUtils.deleteBundleParentDirectory(bundle);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testBundleParentDirectoryDoesNotExist() throws IOException {
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      Bundle bundle = mock(Bundle.class);
      when(bundle.getSource()).thenReturn(Path.of("/does/not/exist/1/2/3/does-not-exist-12345"));
      FileUtils.deleteBundleParentDirectory(bundle);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testBundleParentDirectoryWithTooManyFiles() throws IOException {
    try (MockedStatic<org.apache.commons.io.FileUtils> fileUtilsMocked =
        mockStatic(org.apache.commons.io.FileUtils.class)) {
      fileUtilsMocked
          .when(() -> org.apache.commons.io.FileUtils.deleteDirectory(Mockito.any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      Bundle bundle = mock(Bundle.class);
      final String hash = "bundle.zip";
      Path bundleTemporaryDirectory = Files.createFile(temporaryFolder.resolve(hash));
      for (int i = 0; i < 3; i++) {
        Files.createFile(temporaryFolder.resolve("file-" + i));
      }
      when(bundle.getSource()).thenReturn(bundleTemporaryDirectory);
      FileUtils.deleteBundleParentDirectory(bundle);
      fileUtilsMocked.verifyNoInteractions();
    }
  }

  @Test
  void testBundleParentDirectory() throws IOException {
    Bundle bundle = mock(Bundle.class);
    final String hash = "bundle.zip";
    Path bundleTemporaryDirectory = Files.createFile(temporaryFolder.resolve(hash));
    when(bundle.getSource()).thenReturn(bundleTemporaryDirectory);
    assertTrue(Files.exists(bundleTemporaryDirectory.getParent()));
    FileUtils.deleteBundleParentDirectory(bundle);
    assertFalse(Files.exists(bundleTemporaryDirectory));
  }

  @Test
  void testRemoveTemporaryRepository() throws IOException {
    Git tempRepository = mock(Git.class);
    Repository tempRepository1 = mock(Repository.class);
    when(tempRepository.getRepository()).thenReturn(tempRepository1);
    Path tempGitRepositoryParent =
        Files.createDirectory(temporaryFolder.resolve(String.valueOf(UUID.randomUUID())));
    File tempGitRepository = tempGitRepositoryParent.resolve(".git").toFile();
    Files.createDirectory(tempGitRepository.toPath());
    when(tempRepository1.getDirectory()).thenReturn(tempGitRepository);
    assertTrue(tempGitRepository.exists());
    FileUtils.deleteTemporaryGitRepository(tempRepository);
    assertFalse(tempGitRepository.exists());

    Git notTempRepository = mock(Git.class);
    Repository notTempRepository1 = mock(Repository.class);
    when(notTempRepository.getRepository()).thenReturn(notTempRepository1);
    Path notTempGitRepositoryParent =
        Files.createDirectory(
            Path.of(
                DigestUtils.sha1Hex(
                    GitDetails.normaliseUrl(
                        "https://github.com/common-workflow-language/cwlviewer.git"))));
    File notTempGitRepository = notTempGitRepositoryParent.resolve(".git").toFile();
    Files.createDirectory(notTempGitRepository.toPath());
    when(notTempRepository1.getDirectory()).thenReturn(notTempGitRepository);
    assertTrue(notTempGitRepository.exists());
    FileUtils.deleteTemporaryGitRepository(notTempRepository);
    assertTrue(notTempGitRepository.exists());
  }
}
