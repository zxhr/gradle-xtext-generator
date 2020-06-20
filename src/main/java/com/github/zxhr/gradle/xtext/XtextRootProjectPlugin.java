package com.github.zxhr.gradle.xtext;

import org.codehaus.groovy.runtime.GStringImpl;
import org.eclipse.xtext.util.XtextVersion;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

import com.github.zxhr.gradle.xtext.model.project.IBundleGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.ISubGradleProjectConfig;

public class XtextRootProjectPlugin implements Plugin<Project> {

    /**
     * The name of the Xtext version {@link ExtraPropertiesExtension ext} property.
     */
    public static final String XTEXT_VERSION_PROPERTY = "xtextVersion";

    /**
     * The name of the {@link XtextRootProjectExtension}.
     */
    public static final String EXTENSION_NAME = "xtextRoot";

    /**
     * The name of the task which generates the Xtext projects.
     */
    public static final String GENERATE_MWE2_TASK_NAME = "generateMwe2";

    private XtextRootProjectExtension rootExtension;
    private Object xtextVersion;

    @Override
    public void apply(Project project) {
        configureExtension(project);
        xtextVersion = getXtextVersion(project, rootExtension);
        addXtextVersion(project);
        TaskProvider<GenerateMwe2> generateMwe2 = project.getTasks().register(GENERATE_MWE2_TASK_NAME,
                GenerateMwe2.class, task -> {
                    task.getGenerator().set(rootExtension.getXtextGenerator());
                    task.getGenerator().finalizeValueOnRead();
                });
        rootExtension.setGenerateMwe2Task(generateMwe2);
        project.allprojects(p -> {
            configurePlugin(XtextRuntimePlugin.class, p, rootExtension.getRuntimeConfig(),
                    XtextRuntimePlugin.EXTENSION_NAME);
            configurePlugin(XtextRuntimeTestPlugin.class, p, rootExtension.getRuntimeTestConfig(),
                    XtextRuntimeTestPlugin.EXTENSION_NAME);
            configurePlugin(XtextGenericIdePlugin.class, p, rootExtension.getGenericIdeConfig(),
                    XtextGenericIdePlugin.EXTENSION_NAME);
            configurePlugin(XtextEclipsePluginPlugin.class, p, rootExtension.getEclipsePluginConfig(),
                    XtextEclipsePluginPlugin.EXTENSION_NAME);
            configurePlugin(XtextEclipsePluginTestPlugin.class, p, rootExtension.getEclipsePluginTestConfig(),
                    XtextEclipsePluginTestPlugin.EXTENSION_NAME);
            configurePlugin(XtextWebPlugin.class, p, rootExtension.getWebConfig(), XtextWebPlugin.EXTENSION_NAME);
        });

        generateMwe2.configure(task -> {
            ((Task) task).doLast(new Action<Task>() {
                @Override
                public void execute(Task t) {
                    configureJarManifest(rootExtension.getRuntimeConfig());
                    configureJarManifest(rootExtension.getGenericIdeConfig());
                    configureJarManifest(rootExtension.getEclipsePluginConfig());
                }
            });
        });
    }

    private void configureExtension(Project project) {
        rootExtension = project.getExtensions().create(EXTENSION_NAME, XtextRootProjectExtension.class);
        rootExtension.getCodeConfig().setPreferXtendStubs(false);
        rootExtension.getXtextVersion().convention(project.provider(() -> {
            XtextVersion version = rootExtension.getCodeConfig().getXtextVersion();
            if (version == null) {
                version = XtextVersion.getCurrent();
            }
            return version.getVersion();
        }));
        rootExtension.getXtextVersion().finalizeValueOnRead();
    }

    private <T extends ISubGradleProjectConfig> void configurePlugin(Class<? extends Plugin<Project>> pluginClass,
            Project project, Property<T> property, String extensionName) {
        project.getPlugins().withType(pluginClass, plugin -> {
            if (property.isPresent()) {
                throw new GradleException(pluginClass.getName() + " has already been applied to project "
                        + property.get().getProject().getName());
            }
            @SuppressWarnings("unchecked")
            T config = (T) project.getExtensions().getByName(extensionName);
            property.set(config);
            if (project.getExtensions().findByName(EXTENSION_NAME) == null) {
                project.getExtensions().add(XtextRootProjectExtension.class, EXTENSION_NAME, rootExtension);
            }
            addXtextVersion(project);
        });
    }

    private Object getXtextVersion(Project project, XtextRootProjectExtension rootExtension) {
        Property<String> version = project.getObjects().property(String.class);
        version.set(rootExtension.getXtextVersion().orElse(XtextVersion.getCurrent().getVersion()));
        version.finalizeValueOnRead();
        return new GStringImpl(new Object[] { new Object() {
            @Override
            public String toString() {
                return version.get();
            }
        } }, new String[] { "" });
    }

    private void addXtextVersion(Project project) {
        ExtraPropertiesExtension ext = project.getExtensions().getByType(ExtraPropertiesExtension.class);
        if (!ext.has(XTEXT_VERSION_PROPERTY)) {
            ext.set(XTEXT_VERSION_PROPERTY, xtextVersion);
        }
    }

    private static <T extends IBundleGradleProjectConfig> void configureJarManifest(Provider<T> projectConfigProvider) {
        IBundleGradleProjectConfig projectConfig = projectConfigProvider.getOrNull();
        if (projectConfig == null) {
            return;
        }
        RegularFile manifest = projectConfig.getManifest().getOrNull();
        if (manifest == null) {
            return;
        }
        projectConfig.getProject().getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class,
                task -> task.getManifest().from(manifest));
    }
}
