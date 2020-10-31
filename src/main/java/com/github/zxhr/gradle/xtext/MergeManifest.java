package com.github.zxhr.gradle.xtext;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.java.archives.ManifestMergeSpec;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;

/**
 * Task for merging the Xtext-generated manifest into the Jar task's manifest.
 */
public abstract class MergeManifest extends DefaultTask {

    private Action<? super ManifestMergeSpec> mergeAction = null;

    @Inject
    protected abstract TaskContainer getTasks();

    /**
     * Returns the collection of Xtext-generated manifest files that will be merged
     * into the {@link Jar#manifest(Action) Jar manifest}.
     * 
     * @return the collection of manifest files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    @SkipWhenEmpty
    public abstract ConfigurableFileCollection getManifests();

    /**
     * Returns the {@link Jar} task into which the Xtext-generated manifests will be
     * merged.
     * 
     * @return the {@link Jar} task
     */
    @Input
    @Optional
    public abstract Property<String> getJarTaskName();

    /**
     * Configures how the Xtext-generated manifests will be
     * {@link Manifest#from(Object, Action) merged} into the
     * {@link Jar#manifest(Action) Jar manifest}
     * 
     * @param action manifest merge action
     */
    public void merge(Action<? super ManifestMergeSpec> action) {
        this.mergeAction = action;
    }

    @TaskAction
    protected void mergeManifest() {
        String taskName = getJarTaskName().getOrNull();
        if (taskName == null) {
            super.setDidWork(false);
            return;
        }
        getTasks().named(taskName, Jar.class, task -> {
            Manifest manifest = task.getManifest();
            for (File xtextManifest : getManifests()) {
                if (mergeAction == null) {
                    manifest.from(xtextManifest);
                } else {
                    manifest.from(xtextManifest, mergeAction);
                }
            }
        });
    }

}
