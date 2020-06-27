package com.github.zxhr.gradle.xtext;

import static com.github.zxhr.gradle.xtext.XtextRootProjectPlugin.GENERATE_MWE2_TASK_NAME;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.util.Arrays.asList;
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
import java.util.ArrayList;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class XtextProjectPluginsFunctionalTest {

    private Path tempDir;

    private void setupProject(String project) throws IOException {
        tempDir = Files.createTempDirectory(Paths.get(System.getProperty("testdir")), project);
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

    @Test
    public void testXtextJavaProject() throws IOException {
        setupProject("mydsl");
        BuildResult result = runProject(CLEAN_TASK_NAME, BUILD_TASK_NAME);
        checkProjectsGenerated(result, "example.mydsl", "example.mydsl.ide", "example.mydsl.ui", "example.mydsl.web");
    }

    @Test
    public void testXtextXtendProject() throws IOException {
        setupProject("mydsl-xtend");
        BuildResult result = runProject(CLEAN_TASK_NAME, BUILD_TASK_NAME);
        checkProjectsGenerated(result, "example.mydsl", "example.mydsl.ide", "example.mydsl.ui", "example.mydsl.web");
    }

    @Test
    public void testDifferentVersionXtextProject() throws IOException {
        setupProject("mydsl-xtext-version");
        BuildResult result = runProject(CLEAN_TASK_NAME, BUILD_TASK_NAME);
        checkProjectsGenerated(result, "example.mydsl", "example.mydsl.ide", "example.mydsl.ui", "example.mydsl.web");
    }

    private BuildResult runProject(String... tasks) {
        List<String> arguments = new ArrayList<>(asList(tasks));
        arguments.addAll(asList("-PxtextExampleVersion=" + System.getProperty("xtextVersion"),
                "-PpluginVersion=" + System.getProperty("pluginVersion"),
                "-Dmaven.repo.local=" + System.getProperty("m2"), "-s", "--warning-mode=fail"));
        return GradleRunner.create().withProjectDir(tempDir.toFile()).forwardOutput().withArguments(arguments).build();
    }

    private void checkProjectsGenerated(BuildResult result, String runtimeProject, String genericIdeProject,
            String eclipsePluginProject, String webProject) {
        assertEquals(SUCCESS, result.task(":" + GENERATE_MWE2_TASK_NAME).getOutcome());
        assertEquals(SUCCESS, result.task(":" + runtimeProject + ":" + COMPILE_JAVA_TASK_NAME).getOutcome());
        assertEquals(SUCCESS, result.task(":" + runtimeProject + ":" + COMPILE_TEST_JAVA_TASK_NAME).getOutcome());
        checkRuntimeProjectGenerated(tempDir.resolve(runtimeProject));
        assertEquals(SUCCESS, result.task(":" + genericIdeProject + ":" + COMPILE_JAVA_TASK_NAME).getOutcome());
        checkBundleProjectGenerated(tempDir.resolve(genericIdeProject));
        assertEquals(SUCCESS, result.task(":" + eclipsePluginProject + ":" + COMPILE_JAVA_TASK_NAME).getOutcome());
        assertEquals(SUCCESS, result.task(":" + eclipsePluginProject + ":" + COMPILE_TEST_JAVA_TASK_NAME).getOutcome());
        checkEclipsePluginProjectGenerated(tempDir.resolve(eclipsePluginProject));
        assertEquals(SUCCESS, result.task(":" + webProject + ":" + COMPILE_JAVA_TASK_NAME).getOutcome());
        checkProjectGenerated(tempDir.resolve(webProject));
    }

    private void checkProjectGenerated(Path projectDir) {
        assertTrue(isDirectory(projectDir.resolve("src/main/java")));
        assertTrue(isDirectory(projectDir.resolve("build/src-gen/main/java")));
    }

    private void checkBundleProjectGenerated(Path projectDir) {
        checkProjectGenerated(projectDir);
        assertTrue(isRegularFile(projectDir.resolve("build/src-gen/main/resources/META-INF/MANIFEST.MF")));
    }

    private void checkEclipsePluginProjectGenerated(Path projectDir) {
        checkBundleProjectGenerated(projectDir);
        assertTrue(isRegularFile(projectDir.resolve("build/src-gen/main/resources/plugin.xml")));
        assertTrue(isDirectory(projectDir.resolve("build/src-gen/test/java")));
        assertTrue(isDirectory(projectDir.resolve("build/src-gen/test/resources")));
    }

    private void checkRuntimeProjectGenerated(Path projectDir) {
        checkBundleProjectGenerated(projectDir);
        assertTrue(isRegularFile(projectDir.resolve("build/src-gen/main/resources/plugin.xml")));
        assertTrue(isDirectory(projectDir.resolve("build/src-gen/main/resources/model")));
        assertTrue(isDirectory(projectDir.resolve("src/test/java")));
        assertTrue(isDirectory(projectDir.resolve("build/src-gen/test/java")));
        assertTrue(isDirectory(projectDir.resolve("build/src-gen/test/resources")));
        assertTrue(isRegularFile(projectDir.resolve("build/src-gen/main/resources/plugin.xml")));
    }
}
