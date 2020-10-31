package com.github.zxhr.gradle.xtext;

import static com.github.zxhr.gradle.xtext.AbstractXtextPlugin.MERGE_MANIFEST_TASK_NAME;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

class MergeManifestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        TaskContainer tasks = project.getTasks();
        project.getPluginManager().apply(JavaPlugin.class);
        TaskProvider<MergeManifest> mergeTask = tasks.register(MERGE_MANIFEST_TASK_NAME, MergeManifest.class, task -> {
            task.getJarTaskName().set(JavaPlugin.JAR_TASK_NAME);
        });
        project.afterEvaluate(__ -> {
            Provider<String> taskName = mergeTask.flatMap(MergeManifest::getJarTaskName);
            if (taskName.isPresent()) {
                tasks.named(taskName.get(), Jar.class).configure(task -> task.dependsOn(mergeTask));
            }
        });
    }

}
