package com.github.zxhr.gradle.xtext;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

/**
 * Task for configuring a project for Eclipse PDE. This includes generating the
 * project's {@code build.properties} file and updating the project's
 * {@code org.eclipse.pde.core.prefs} settings file.
 */
public abstract class ConfigurePde extends DefaultTask {

    private static final String META_INF = "META-INF";
    private static final String MANIFEST = "MANIFEST.MF";
    private static final String PLUGIN_XML = "plugin.xml";
    private static final String BUILD_PROPERTIES = "build.properties";

    public ConfigurePde() {
        getProperties().put("bin.includes", "META-INF/,plugin.xml");
    }

    /**
     * Returns the directory to generate the project's {@code build.properties}
     * file.
     * 
     * @return the directory to generate the project's {@code build.properties} file
     */
    @OutputDirectory
    public abstract DirectoryProperty getPdeDirectory();

    /**
     * Returns the location of the project's {@code org.eclipse.pde.core.prefs}
     * file.
     * 
     * @return the location of the project's {@code org.eclipse.pde.core.prefs} file
     */
    @OutputFile
    public abstract RegularFileProperty getPdeSettingFile();

    /**
     * Returns the {@link Jar} containing the project's {@code plugin.xml} and
     * {@code MANIFEST.MF}
     * 
     * @return the project's {@link Jar}
     */
    @InputFile
    public abstract RegularFileProperty getJar();

    /**
     * Returns the properties that will be written to the {@code build.properties}
     * file.
     * 
     * @return the properties that will be written to the {@code build.properties}
     *         file
     */
    @Input
    @Optional
    public abstract MapProperty<String, String> getProperties();

    @Inject
    protected abstract ProjectLayout getLayout();

    @TaskAction
    protected void configurePde() throws IOException {
        Path pdeDirectory = getPdeDirectory().get().getAsFile().toPath();
        copy(getJar().get().getAsFile().toPath(), pdeDirectory);
        Properties buildProperties = new MapBackedProperties(getProperties().getOrElse(Collections.emptyMap()));
        Files.createDirectories(pdeDirectory);
        try (Writer writer = Files.newBufferedWriter(pdeDirectory.resolve(BUILD_PROPERTIES))) {
            buildProperties.store(writer, null);
        }
        Properties pdePrefs = new MapBackedProperties(new LinkedHashMap<>());
        Path settingsFile = getPdeSettingFile().get().getAsFile().toPath();
        if (Files.isRegularFile(settingsFile)) {
            try (Reader reader = Files.newBufferedReader(settingsFile)) {
                pdePrefs.load(reader);
            }
        } else {
            Files.createDirectories(settingsFile.getParent());
        }
        pdePrefs.setProperty("eclipse.preferences.version", "1");
        Path projectDir = getLayout().getProjectDirectory().getAsFile().toPath();
        Path relative = projectDir.relativize(pdeDirectory);
        String path = "";
        for (Path part : relative) {
            path += part + "/";
        }
        pdePrefs.setProperty("BUNDLE_ROOT_PATH", path);
        try (Writer writer = Files.newBufferedWriter(settingsFile)) {
            pdePrefs.store(writer, null);
        }
    }

    private static void copy(Path jarFile, Path directory) throws IOException {
        URI zipUri = URI.create("jar:" + jarFile.toUri());
        try (FileSystem zipFs = FileSystems.newFileSystem(zipUri, Collections.emptyMap())) {
            Path manifest = zipFs.getPath(META_INF, MANIFEST);
            if (Files.isRegularFile(manifest)) {
                Path metaInf = directory.resolve(META_INF);
                Path destination = metaInf.resolve(MANIFEST);
                Files.createDirectories(metaInf);
                Files.copy(manifest, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            Path pluginXml = zipFs.getPath(PLUGIN_XML);
            if (Files.isRegularFile(pluginXml)) {
                Path destination = directory.resolve(PLUGIN_XML);
                Files.copy(pluginXml, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

}
