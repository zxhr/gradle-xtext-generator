package com.github.zxhr.gradle.xtext;

import java.io.File;
import java.util.function.Function;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

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
        projectConfig.getMetaInfDirectory().set(srcGenResourcesDir.map(d -> d.dir("META-INF")));
        if (projectConfig instanceof IBundleGradleProjectConfig) {
            IBundleGradleProjectConfig bundleConfig = (IBundleGradleProjectConfig) projectConfig;
            bundleConfig.getManifest().set(projectConfig.getMetaInfDirectory().file("MANIFEST.MF"));
            bundleConfig.getPluginXml().set(srcGenResourcesDir.map(d -> d.file("plugin.xml")));
        }
        if (projectConfig instanceof IRuntimeGradleProjectConfig) {
            IRuntimeGradleProjectConfig runtimeConfig = (IRuntimeGradleProjectConfig) projectConfig;
            runtimeConfig.getEcoreModelDirectory().set(srcGenResourcesDir.map(d -> d.dir("model")));
        }
        if (projectConfig instanceof IWebGradleProjectConfig) {
            IWebGradleProjectConfig webConfig = (IWebGradleProjectConfig) projectConfig;
            webConfig.getAssetsDirectory().set(srcGenResourcesDir.map(d -> d.dir("assets")));
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
        resources.srcDir(projectConfig.getMetaInfDirectory().getAsFile().map(File::getParentFile));
        if (projectConfig instanceof IBundleGradleProjectConfig) {
            IBundleGradleProjectConfig bundleConfig = (IBundleGradleProjectConfig) projectConfig;
            resources.srcDir(bundleConfig.getPluginXml().getAsFile().map(File::getParentFile));
        }
        if (projectConfig instanceof IRuntimeGradleProjectConfig) {
            IRuntimeGradleProjectConfig runtimeConfig = (IRuntimeGradleProjectConfig) projectConfig;
            resources.srcDir(runtimeConfig.getEcoreModelDirectory().getAsFile().map(File::getParentFile));
        }
        if (projectConfig instanceof IWebGradleProjectConfig) {
            IWebGradleProjectConfig webConfig = (IWebGradleProjectConfig) projectConfig;
            resources.srcDir(webConfig.getAssetsDirectory().getAsFile().map(File::getParent));
        }
    }

}
