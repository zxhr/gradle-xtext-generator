package com.github.zxhr.gradle.xtext;

import static com.github.zxhr.gradle.xtext.XtextRootProjectPlugin.GENERATE_MWE2_TASK_NAME;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_TASK_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.CLEAN_TASK_NAME;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class XtextProjectPluginsFunctionalTest {

    private Path tempDir;

    private void setupProject(String project) throws IOException {
        tempDir = Files.createTempDirectory(project);
        Path rootProject = Paths.get("src", "test", "resources", project);
        Files.walkFileTree(rootProject, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path tempFile = tempDir.resolve(rootProject.relativize(file));
                Files.createDirectories(tempFile.getParent());
                Files.copy(file, tempFile);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "mydsl", "mydsl-xtend", "mydsl-xtext-version" })
    public void testPlugin(String project) throws IOException {
        setupProject(project);
        BuildResult result = GradleRunner.create().withProjectDir(tempDir.toFile()).forwardOutput()
                .withArguments(CLEAN_TASK_NAME, BUILD_TASK_NAME,
                        "-PxtextExampleVersion=" + System.getProperty("xtextVersion"),
                        "-PpluginVersion=" + System.getProperty("pluginVersion"),
                        "-Dmaven.repo.local=" + System.getProperty("m2"), "-s", "--warning-mode=fail")
                .build();
        assertEquals(SUCCESS, result.task(":" + GENERATE_MWE2_TASK_NAME).getOutcome());
        assertEquals(SUCCESS, result.task(":example.mydsl:" + COMPILE_JAVA_TASK_NAME).getOutcome());
        assertEquals(SUCCESS, result.task(":example.mydsl:" + COMPILE_TEST_JAVA_TASK_NAME).getOutcome());
        assertTrue(isDirectory(tempDir.resolve("example.mydsl/src/main/java")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl/build/src-gen/main/java")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl/build/src-gen/main/resources/model")));
        assertTrue(isRegularFile(tempDir.resolve("example.mydsl/build/src-gen/main/resources/META-INF/MANIFEST.MF")));
        assertTrue(isRegularFile(tempDir.resolve("example.mydsl/build/src-gen/main/resources/plugin.xml")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl/src/test/java")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl/build/src-gen/test/java")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl/build/src-gen/test/resources")));
        assertEquals(SUCCESS, result.task(":example.mydsl.ide:" + COMPILE_JAVA_TASK_NAME).getOutcome());
        assertTrue(isDirectory(tempDir.resolve("example.mydsl.ide/src/main/java")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl.ide/build/src-gen/main/java")));
        assertTrue(
                isRegularFile(tempDir.resolve("example.mydsl.ide/build/src-gen/main/resources/META-INF/MANIFEST.MF")));
        assertEquals(SUCCESS, result.task(":example.mydsl.ui:" + COMPILE_JAVA_TASK_NAME).getOutcome());
        assertEquals(SUCCESS, result.task(":example.mydsl.ui:" + COMPILE_TEST_JAVA_TASK_NAME).getOutcome());
        assertTrue(isDirectory(tempDir.resolve("example.mydsl.ui/src/main/java")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl.ui/build/src-gen/main/java")));
        assertTrue(
                isRegularFile(tempDir.resolve("example.mydsl.ui/build/src-gen/main/resources/META-INF/MANIFEST.MF")));
        assertTrue(isRegularFile(tempDir.resolve("example.mydsl.ui/build/src-gen/main/resources/plugin.xml")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl.ui/build/src-gen/test/java")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl.ui/build/src-gen/test/resources")));
        assertEquals(SUCCESS, result.task(":example.mydsl.web:" + COMPILE_JAVA_TASK_NAME).getOutcome());
        assertTrue(isDirectory(tempDir.resolve("example.mydsl.web/src/main/java")));
        assertTrue(isDirectory(tempDir.resolve("example.mydsl.web/build/src-gen/main/java")));
    }
}
