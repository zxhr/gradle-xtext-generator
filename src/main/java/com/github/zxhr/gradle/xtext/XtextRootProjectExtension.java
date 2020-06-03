package com.github.zxhr.gradle.xtext;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.eclipse.xtext.xtext.generator.CodeConfig;
import org.eclipse.xtext.xtext.generator.XtextGenerator;
import org.eclipse.xtext.xtext.generator.XtextGeneratorLanguage;
import org.eclipse.xtext.xtext.generator.model.ManifestAccess;
import org.eclipse.xtext.xtext.generator.model.PluginXmlAccess;
import org.eclipse.xtext.xtext.generator.model.project.BundleProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.ISubProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.IXtextProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.RuntimeProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.SubProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.WebProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.XtextProjectConfig;
import org.gradle.api.Action;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskProvider;

import com.github.zxhr.gradle.xtext.model.project.IBundleGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.IRuntimeGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.ISubGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.IWebGradleProjectConfig;

/**
 * Configuration for Xtext projects, analogous to {@link IXtextProjectConfig}.
 */
public abstract class XtextRootProjectExtension {

    private final XtextGenerator generator;
    private final Map<String, XtextGeneratorLanguage> languages = new HashMap<>();
    private final Property<XtextGenerator> xtextGenerator;
    private TaskProvider<GenerateMwe2> generateMwe2Task;

    public XtextRootProjectExtension() {
        this.generator = new XtextGenerator();
        this.xtextGenerator = getObjects().property(XtextGenerator.class);
        this.xtextGenerator.set(getProviders().provider(this::finalizeGenerator));
        this.xtextGenerator.finalizeValueOnRead();
        this.xtextGenerator.disallowChanges();
        XtextProjectConfig projectConfigs = new XtextProjectConfig();
        this.generator.getConfiguration().setProject(projectConfigs);
        for (SubProjectConfig projectConfig : projectConfigs.getAllProjects()) {
            projectConfig.setEnabled(false);
            projectConfig.setOverwriteSrc(false);
        }
    }

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviders();

    /**
     * Returns the Xtext version for this project.
     * 
     * @return the Xtext version for this project
     */
    public abstract Property<String> getXtextVersion();

    /**
     * Returns the Xtext runtime configuration. Analogous to
     * {@link IXtextProjectConfig#getRuntime()}.
     * 
     * <p>
     * The configuration may be {@link Provider#isPresent() unset} to
     * {@link ISubProjectConfig#isEnabled() disable} generation of the
     * configuration.
     * </p>
     * 
     * @return the Xtext runtime configuration
     */
    public abstract Property<IRuntimeGradleProjectConfig> getRuntimeConfig();

    /**
     * Returns the Xtext runtime test configuration. Analogous to
     * {@link IXtextProjectConfig#getRuntimeTest()}.
     * 
     * <p>
     * The configuration may be {@link Provider#isPresent() unset} to
     * {@link ISubProjectConfig#isEnabled() disable} generation of the
     * configuration.
     * </p>
     * 
     * @return the Xtext runtime test configuration
     */
    public abstract Property<IBundleGradleProjectConfig> getRuntimeTestConfig();

    /**
     * Returns the Xtext generic IDE configuration. Analogous to
     * {@link IXtextProjectConfig#getGenericIde()}.
     * 
     * <p>
     * The configuration may be {@link Provider#isPresent() unset} to
     * {@link ISubProjectConfig#isEnabled() disable} generation of the
     * configuration.
     * </p>
     * 
     * @return the Xtext generic IDE configuration
     */
    public abstract Property<IBundleGradleProjectConfig> getGenericIdeConfig();

    /**
     * Returns the Xtext Eclipse plugin configuration. Analogous to
     * {@link IXtextProjectConfig#getEclipsePlugin()}.
     * 
     * <p>
     * The configuration may be {@link Provider#isPresent() unset} to
     * {@link ISubProjectConfig#isEnabled() disable} generation of the
     * configuration.
     * </p>
     * 
     * @return the Xtext Eclipse plugin configuration
     */
    public abstract Property<IBundleGradleProjectConfig> getEclipsePluginConfig();

    /**
     * Returns the Xtext Eclipse plugin test configuration. Analogous to
     * {@link IXtextProjectConfig#getEclipsePluginTest()}.
     * 
     * <p>
     * The configuration may be {@link Provider#isPresent() unset} to
     * {@link ISubProjectConfig#isEnabled() disable} generation of the
     * configuration.
     * </p>
     * 
     * @return the Xtext Eclipse plugin test configuration
     */
    public abstract Property<IBundleGradleProjectConfig> getEclipsePluginTestConfig();

    /**
     * Returns the Xtext web configuration. Analogous to
     * {@link IXtextProjectConfig#getWeb()}.
     * 
     * <p>
     * The configuration may be {@link Provider#isPresent() unset} to
     * {@link ISubProjectConfig#isEnabled() disable} generation of the
     * configuration.
     * </p>
     * 
     * @return the Xtext web configuration
     */
    public abstract Property<IWebGradleProjectConfig> getWebConfig();

    /**
     * Returns the task responsible for generating the Xtext projects.
     * 
     * @return the task responsible for generating the Xtext projects
     */
    public TaskProvider<GenerateMwe2> getGenerateMwe2Task() {
        return Objects.requireNonNull(generateMwe2Task, GenerateMwe2.class.getName() + " task not set");
    }

    /**
     * Sets the task responsible for generating the Xtext projects.
     * 
     * @param generateMwe2Task the task responsible for generating the Xtext
     *                         projects
     */
    public void setGenerateMwe2Task(TaskProvider<GenerateMwe2> generateMwe2Task) {
        this.generateMwe2Task = generateMwe2Task;
    }

    /**
     * Creates a {@link XtextGeneratorLanguage} with the given name if it does not
     * already exist and configures it with the given action.
     * 
     * @param name   language name
     * @param action action to configure the language
     * @return the language
     */
    public XtextGeneratorLanguage language(String name, Action<? super XtextGeneratorLanguage> action) {
        return language(name, XtextGeneratorLanguage.class, action);
    }

    /**
     * Creates a {@link XtextGeneratorLanguage} with the given name if it does not
     * already exist and configures it with the given action.
     * 
     * <p>
     * The language class must satisfy the conditions described by
     * {@link ObjectFactory#newInstance(Class, Object...)}.
     * </p>
     * 
     * @param name          language name
     * @param languageClass language class
     * @param action        action to configure the language
     * @param <T>           language class type
     * @return the language
     */
    public <T extends XtextGeneratorLanguage> T language(String name, Class<T> languageClass,
            Action<? super T> action) {
        @SuppressWarnings("unchecked")
        T language = (T) languages.compute(name, (__, previous) -> {
            if (previous == null) {
                T instance = getObjects().newInstance(languageClass);
                instance.setName(name);
                generator.addLanguage(instance);
                return instance;
            } else {
                return languageClass.cast(previous);
            }
        });
        action.execute(language);
        return language;
    }

    /**
     * Returns the {@link CodeConfig} for the project generation.
     * 
     * @return the {@link CodeConfig} for the project generation
     */
    public CodeConfig getCodeConfig() {
        return generator.getConfiguration().getCode();
    }

    /**
     * Returns the fully-configured {@link XtextGenerator}.
     * 
     * @return Xtext generator
     */
    public Provider<XtextGenerator> getXtextGenerator() {
        return xtextGenerator;
    }

    private XtextGenerator finalizeGenerator() {
        XtextProjectConfig config = generator.getConfiguration().getProject();
        IRuntimeGradleProjectConfig runtime = getRuntimeConfig().getOrNull();
        if (runtime != null) {
            copyRuntime(runtime, config.getRuntime());
        }
        IBundleGradleProjectConfig runtimeTest = getRuntimeTestConfig().getOrNull();
        if (runtimeTest != null) {
            copyBundle(runtimeTest, config.getRuntimeTest());
        }
        IBundleGradleProjectConfig genericIde = getGenericIdeConfig().getOrNull();
        if (genericIde != null) {
            copyBundle(genericIde, config.getGenericIde());
        }
        IBundleGradleProjectConfig eclipsePlugin = getEclipsePluginConfig().getOrNull();
        if (eclipsePlugin != null) {
            copyBundle(eclipsePlugin, config.getEclipsePlugin());
        }
        IBundleGradleProjectConfig eclipsePluginTest = getEclipsePluginTestConfig().getOrNull();
        if (eclipsePluginTest != null) {
            copyBundle(eclipsePluginTest, config.getEclipsePluginTest());
        }
        IWebGradleProjectConfig web = getWebConfig().getOrNull();
        if (web != null) {
            copyWeb(web, config.getWeb());
        }
        return generator;
    }

    private void copyRuntime(IRuntimeGradleProjectConfig fromConfig, RuntimeProjectConfig toConfig) {
        copyBundle(fromConfig, toConfig);
        if (fromConfig.getEcoreModelDirectory().isPresent()) {
            toConfig.setEcoreModel(fromConfig.getEcoreModelDirectory().get().getAsFile().getAbsolutePath());
        }
    }

    private void copyBundle(IBundleGradleProjectConfig fromConfig, BundleProjectConfig toConfig) {
        copyProject(fromConfig, toConfig);
        if (fromConfig.getPluginXml().isPresent()) {
            if (toConfig.getPluginXml() == null) {
                toConfig.setPluginXml(new PluginXmlAccess());
            }
            toConfig.getPluginXml()
                    .setPath(relativeToRoot(fromConfig.getProject().getProjectDir(), fromConfig.getPluginXml()));
        }
        if (fromConfig.getManifest().isPresent()) {
            if (toConfig.getManifest() == null) {
                toConfig.setManifest(new ManifestAccess());
            }
            toConfig.getManifest().setPath(
                    relativeToRoot(fromConfig.getMetaInfDirectory().get().getAsFile(), fromConfig.getManifest()));
        }
    }

    private void copyWeb(IWebGradleProjectConfig fromConfig, WebProjectConfig toConfig) {
        copyProject(fromConfig, toConfig);
        toConfig.setAssets(fromConfig.getAssetsDirectory().get().getAsFile().getAbsolutePath());
    }

    private void copyProject(ISubGradleProjectConfig fromConfig, SubProjectConfig toConfig) {
        toConfig.setEnabled(true);
        toConfig.setOverwriteSrc(false);
        toConfig.setName(fromConfig.getProjectName().getOrElse(fromConfig.getProject().getName()));
        toConfig.setRoot(fromConfig.getProject().getProjectDir().getAbsolutePath());
        if (fromConfig.getSrcDirectory().isPresent()) {
            toConfig.setSrc(fromConfig.getSrcDirectory().get().getAsFile().getAbsolutePath());
        }
        if (fromConfig.getSrcGenDirectory().isPresent()) {
            toConfig.setSrcGen(fromConfig.getSrcGenDirectory().get().getAsFile().getAbsolutePath());
        }
        if (fromConfig.getMetaInfDirectory().isPresent()) {
            toConfig.setMetaInf(fromConfig.getMetaInfDirectory().get().getAsFile().getAbsolutePath());
        }
        if (fromConfig.getIconsDirectory().isPresent()) {
            toConfig.setIcons(fromConfig.getIconsDirectory().get().getAsFile().getAbsolutePath());
        }
    }

    private String relativeToRoot(File root, Provider<? extends FileSystemLocation> file) {
        Path relative = root.toPath().toAbsolutePath().relativize(file.get().getAsFile().toPath());
        return StreamSupport.stream(relative.spliterator(), false).map(Path::toString).collect(Collectors.joining("/"));
    }
}
