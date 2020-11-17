package com.github.zxhr.gradle.xtext;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Function;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

import com.github.zxhr.gradle.xtext.model.project.IBundleGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.IRuntimeGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.ISubGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.IWebGradleProjectConfig;

/**
 * Configures the project as follows:
 * 
 * <ul>
 * <li>Sets the default {@link ISubGradleProjectConfig#getProjectName()} to
 * {@link Project#getName()}</li>
 * <li>Sets the default {@link ISubGradleProjectConfig#getSrcDirectory()} to the
 * first {@link SourceDirectorySet#getSrcDirs() directory} in the java source
 * set.</li>
 * <li>Sets the default {@link ISubGradleProjectConfig#getSrcGenDirectory()} to
 * {@link Project#getBuildDir()}{@code /src-gen/<sourceSetName>/java}.</li>
 * <li>Sets the default {@link ISubGradleProjectConfig#getMetaInfDirectory()} to
 * {@link Project#getBuildDir()}{@code /src-gen/<sourceSetName>/resources/META-INF}</li>
 * <li>If applicable, sets the default
 * {@link IBundleGradleProjectConfig#getManifest()} to
 * {@link ISubGradleProjectConfig#getMetaInfDirectory()}{@code /MANIFEST.MF}.</li>
 * <li>If applicable, sets the default
 * {@link IBundleGradleProjectConfig#getPluginXml()} to
 * {@link Project#getBuildDir()}{@code /src-gen/<sourceSetName>/resources/plugin.xml}</li>
 * <li>If applicable, sets the default
 * {@link IRuntimeGradleProjectConfig#getEcoreModelDirectory()} to
 * {@link Project#getBuildDir()}{@code /src-gen/<sourceSetName>/resources/model}</li>
 * <li>If applicable, sets the default
 * {@link IWebGradleProjectConfig#getAssetsDirectory()} to
 * {@link Project#getBuildDir()}{@code /src-gen/<sourceSetName>/resources/assets}</li>
 * <li>Adds the {@link ISubGradleProjectConfig#getSrcGenDirectory()} to the java
 * source set {@link SourceDirectorySet#getSrcDirs() directory}</li>
 * <li>Adds the parent directories of
 * {@link ISubGradleProjectConfig#getMetaInfDirectory()},
 * {@link IBundleGradleProjectConfig#getPluginXml()},
 * {@link IRuntimeGradleProjectConfig#getEcoreModelDirectory()}, and
 * {@link IWebGradleProjectConfig#getAssetsDirectory()} (when applicable) to the
 * resource source set {@link SourceDirectorySet#getSrcDirs() directory}</li>
 * </ul>
 * 
 * @param <C> configuration type
 */
public abstract class AbstractXtextPlugin<C extends ISubGradleProjectConfig> implements Plugin<Project> {

    /**
     * The name of the {@link MergeManifest} task for merging the Xtext-generated
     * manifest into the Jar task's manifest.
     */
    public static final String MERGE_MANIFEST_TASK_NAME = "mergeManifest";

    /**
     * The name of the {@link ConfigurePde} task for configuring the project for
     * Eclipse PDE.
     */
    public static final String CONFIGURE_PDE_TASK_NAME = "configurePde";

    private final String sourceSetName;
    private final String extensionName;
    private final Class<C> configClass;
    private final Function<? super Project, ? extends C> configConstructor;

    /**
     * @param sourceSetName     name of source set to configure
     * @param extensionName     name of extension for setting the project config
     * @param configClass       config class for the extension
     * @param configConstructor constructor for initializing the config class
     */
    protected AbstractXtextPlugin(String sourceSetName, String extensionName, Class<C> configClass,
            Function<? super Project, ? extends C> configConstructor) {
        this.sourceSetName = sourceSetName;
        this.extensionName = extensionName;
        this.configClass = configClass;
        this.configConstructor = configConstructor;
    }

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        C projectConfig = configConstructor.apply(project);
        project.getExtensions().add(configClass, extensionName, projectConfig);
        configure(project, sourceSetName, projectConfig);
        if (IBundleGradleProjectConfig.class.isAssignableFrom(configClass)) {
            configurePdeTask(project);
        }
    }

    private static void configure(Project project, String sourceSetName, ISubGradleProjectConfig projectConfig) {
        SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
        NamedDomainObjectProvider<SourceSet> sourceSet = sourceSets.named(sourceSetName);
        Provider<File> srcDir = sourceSet.map(s -> s.getJava().getSrcDirs().iterator().next());
        DirectoryProperty build = project.getLayout().getBuildDirectory();
        Provider<Directory> srcGenDir = build.dir("src-gen").map(d -> d.dir(sourceSetName));
        Provider<Directory> srcGenJavaDir = srcGenDir.flatMap(d -> sourceSet.map(s -> d.dir(s.getJava().getName())));
        Provider<Directory> srcGenResourcesDir = srcGenDir
                .flatMap(d -> sourceSet.map(s -> d.dir(s.getResources().getName())));
        projectConfig.getProjectName().set(project.getName());
        projectConfig.getSrcDirectory().set(project.getLayout().dir(srcDir));
        projectConfig.getSrcGenDirectory().set(srcGenJavaDir);
        projectConfig.getResourcesGenDirectory().set(srcGenResourcesDir);
        projectConfig.getMetaInfDirectory().set(projectConfig.getResourcesGenDirectory().map(d -> d.dir("META-INF")));
        if (projectConfig instanceof IBundleGradleProjectConfig) {
            IBundleGradleProjectConfig bundleConfig = (IBundleGradleProjectConfig) projectConfig;
            bundleConfig.getManifest().set(projectConfig.getMetaInfDirectory().file("MANIFEST.MF"));
            bundleConfig.getPluginXml().set(projectConfig.getResourcesGenDirectory().map(d -> d.file("plugin.xml")));
        }
        if (projectConfig instanceof IRuntimeGradleProjectConfig) {
            IRuntimeGradleProjectConfig runtimeConfig = (IRuntimeGradleProjectConfig) projectConfig;
            runtimeConfig.getEcoreModelDirectory()
                    .set(projectConfig.getResourcesGenDirectory().map(d -> d.dir("model").dir("generated")));
        }
        if (projectConfig instanceof IWebGradleProjectConfig) {
            IWebGradleProjectConfig webConfig = (IWebGradleProjectConfig) projectConfig;
            webConfig.getAssetsDirectory().set(projectConfig.getResourcesGenDirectory().map(d -> d.dir("assets")));
        }
        sourceSet.configure(ss -> configureSourceSet(project, ss, projectConfig));
    }

    private static void configureSourceSet(Project project, SourceSet sourceSet,
            ISubGradleProjectConfig projectConfig) {
        Provider<XtextRootProjectExtension> rootExtension = project
                .provider(() -> project.getExtensions().getByType(XtextRootProjectExtension.class));
        Provider<?> generateMwe2 = rootExtension.flatMap(XtextRootProjectExtension::getGenerateMwe2Task);
        project.getTasks().named(sourceSet.getCompileJavaTaskName(), task -> task.dependsOn(generateMwe2));
        project.getTasks().named(sourceSet.getProcessResourcesTaskName(), task -> task.dependsOn(generateMwe2));
        sourceSet.getJava().srcDir(projectConfig.getSrcGenDirectory());
        SourceDirectorySet resources = sourceSet.getResources();
        SetProperty<File> resourceDirs = project.getObjects().setProperty(File.class);
        resourceDirs.add(projectConfig.getMetaInfDirectory().getAsFile().map(File::getParentFile));

        if (projectConfig instanceof IBundleGradleProjectConfig) {
            IBundleGradleProjectConfig bundleConfig = (IBundleGradleProjectConfig) projectConfig;
            resourceDirs.add(bundleConfig.getPluginXml().getAsFile().map(File::getParentFile));
            if (SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName())) {
                project.getPluginManager().apply(MergeManifestPlugin.class);
                TaskContainer tasks = project.getTasks();
                tasks.named(MERGE_MANIFEST_TASK_NAME, MergeManifest.class, task -> {
                    task.getManifests().from(bundleConfig.getManifest());
                });
            }
        }
        if (projectConfig instanceof IRuntimeGradleProjectConfig) {
            IRuntimeGradleProjectConfig runtimeConfig = (IRuntimeGradleProjectConfig) projectConfig;
            resourceDirs.add(runtimeConfig.getEcoreModelDirectory().getAsFile().map(File::getParentFile));
        }
        if (projectConfig instanceof IWebGradleProjectConfig) {
            IWebGradleProjectConfig webConfig = (IWebGradleProjectConfig) projectConfig;
            resourceDirs.add(webConfig.getAssetsDirectory().getAsFile().map(File::getParentFile));
        }
        project.afterEvaluate(__ -> {
            rootExtension.get().getGenerateMwe2Task().configure(task -> {
                task.doLast(new Action<Task>() {
                    @Override
                    public void execute(Task t) {
                        Path srcGenDir = projectConfig.getSrcGenDirectory().get().getAsFile().toPath();
                        Path resourcesDir = projectConfig.getResourcesGenDirectory().get().getAsFile().toPath();
                        moveXtextFilesToResources(srcGenDir, resourcesDir);
                    }
                });
            });
        });
        resources.srcDir(resourceDirs);
    }

    private static void moveXtextFilesToResources(Path srcGenDir, Path resourcesDir) {
        try {
            Files.walkFileTree(srcGenDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isNonSourceFile(file)) {
                        Path destination = resourcesDir.resolve(srcGenDir.relativize(file));
                        Files.createDirectories(destination.getParent());
                        Files.move(file, destination);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    boolean empty;
                    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
                        empty = !dirStream.iterator().hasNext();
                    }
                    if (empty) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isNonSourceFile(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        return !("java".equals(extension) || "xtend".equals(extension));
    }

    private static void configurePdeTask(Project project) {
        project.getPlugins().withType(EclipsePlugin.class, plugin -> {
            TaskContainer tasks = project.getTasks();
            TaskProvider<ConfigurePde> configurePde;
            try {
                tasks.named(CONFIGURE_PDE_TASK_NAME, ConfigurePde.class);
                return;
            } catch (UnknownTaskException e) {
                configurePde = tasks.register(CONFIGURE_PDE_TASK_NAME, ConfigurePde.class, task -> {
                    ((Task) task).setGroup("IDE");
                    ((Task) task).setDescription("Generates the org.eclipse.pde.core.prefs file.");
                    task.getPdeDirectory().set(project.getLayout().getBuildDirectory().dir("pde"));
                    task.getPdeSettingFile().convention(
                            project.getLayout().getProjectDirectory().file(".settings/org.eclipse.pde.core.prefs"));
                    task.getJar().set(tasks.named(JavaPlugin.JAR_TASK_NAME, Jar.class).flatMap(Jar::getArchiveFile));
                });
                SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class)
                        .getSourceSets();
                SourceSet pde = sourceSets.create("pde");
                pde.getResources().srcDir(configurePde.flatMap(ConfigurePde::getPdeDirectory));
            }
            plugin.getLifecycleTask().configure(task -> task.dependsOn(CONFIGURE_PDE_TASK_NAME));
            EclipseModel eclipseModel = project.getExtensions().getByType(EclipseModel.class);
            eclipseModel.synchronizationTasks(CONFIGURE_PDE_TASK_NAME);
        });
    }

}
