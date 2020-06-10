package com.github.zxhr.buildsrc.gradle.xtext;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Usage;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.VariantVersionMappingStrategy;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;

import com.gradle.publish.PluginBundleExtension;
import com.gradle.publish.PublishPlugin;
import com.gradle.publish.PublishTask;

public class ConfigurePublishingPlugin implements Plugin<Project> {

    @SuppressWarnings("deprecation")
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(MavenPublishPlugin.class);
        project.getPluginManager().apply(JavaGradlePluginPlugin.class);
        project.getPluginManager().apply(PublishPlugin.class);
        String pomTask = "generatePomFileForPluginMavenPublication";
        project.getTasks().named("publishPlugins", PublishTask.class, task -> {
            task.dependsOn(pomTask);
        });
        PublishingExtension publishing = project.getExtensions().findByType(PublishingExtension.class);
        // see https://github.com/gradle/gradle/issues/12323
        project.afterEvaluate(__ -> {
            publishing.getPublications().named("pluginMaven", MavenPublication.class, publication -> {
                publication.versionMapping(mapping -> {
                    mapping.usage(Usage.JAVA_API, VariantVersionMappingStrategy::fromResolutionResult);
                    mapping.usage(Usage.JAVA_RUNTIME, VariantVersionMappingStrategy::fromResolutionResult);
                });
            });
        });
        PluginBundleExtension extension = project.getExtensions().findByType(PluginBundleExtension.class);
        extension.withDependencies(dependencies -> {
            File pom = project.getTasks().named(pomTask, GenerateMavenPom.class).get().getDestination();
            Model model;
            try (FileReader reader = new FileReader(pom)) {
                MavenXpp3Reader mavenReader = new MavenXpp3Reader();
                model = mavenReader.read(reader);
                model.setPomFile(pom);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Map<String, String> dependencyToVersion = new HashMap<>();
            for (Dependency dep : model.getDependencies()) {
                dependencyToVersion.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep.getVersion());
            }
            for (Dependency dep : dependencies) {
                if (dep.getVersion() == null) {
                    dep.setVersion(dependencyToVersion.get(dep.getGroupId() + ":" + dep.getArtifactId()));
                }
            }
        });
    }

}
