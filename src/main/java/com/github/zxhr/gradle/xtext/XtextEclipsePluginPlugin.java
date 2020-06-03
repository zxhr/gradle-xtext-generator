package com.github.zxhr.gradle.xtext;

import org.gradle.api.tasks.SourceSet;

import com.github.zxhr.gradle.xtext.model.project.BundleGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.IBundleGradleProjectConfig;

/**
 * Plugin that configures the
 * {@link XtextRootProjectExtension#getEclipsePluginConfig()} with this project.
 */
public class XtextEclipsePluginPlugin extends AbstractXtextPlugin<IBundleGradleProjectConfig> {

    /**
     * The name of the {@link IBundleGradleProjectConfig} extension.
     */
    public static final String EXTENSION_NAME = "xtextEclipsePlugin";

    public XtextEclipsePluginPlugin() {
        super(SourceSet.MAIN_SOURCE_SET_NAME, EXTENSION_NAME, IBundleGradleProjectConfig.class,
                BundleGradleProjectConfig::new);
    }

}
