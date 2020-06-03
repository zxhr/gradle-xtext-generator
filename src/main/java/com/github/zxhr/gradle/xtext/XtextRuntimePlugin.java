package com.github.zxhr.gradle.xtext;

import org.gradle.api.tasks.SourceSet;

import com.github.zxhr.gradle.xtext.model.project.IRuntimeGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.RuntimeGradleProjectConfig;

/**
 * Plugin that configures the
 * {@link XtextRootProjectExtension#getRuntimeConfig()} with this project.
 */
public class XtextRuntimePlugin extends AbstractXtextPlugin<IRuntimeGradleProjectConfig> {

    /**
     * The name of the {@link IRuntimeGradleProjectConfig} extension.
     */
    public static final String EXTENSION_NAME = "xtextRuntime";

    public XtextRuntimePlugin() {
        super(SourceSet.MAIN_SOURCE_SET_NAME, EXTENSION_NAME, IRuntimeGradleProjectConfig.class,
                RuntimeGradleProjectConfig::new);
    }

}
