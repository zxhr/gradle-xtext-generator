package com.github.zxhr.gradle.xtext;

import org.gradle.api.tasks.SourceSet;

import com.github.zxhr.gradle.xtext.model.project.IWebGradleProjectConfig;
import com.github.zxhr.gradle.xtext.model.project.WebGradleProjectConfig;

/**
 * Plugin that configures the {@link XtextRootProjectExtension#getWebConfig()}
 * with this project.
 */
public class XtextWebPlugin extends AbstractXtextPlugin<IWebGradleProjectConfig> {

    /**
     * The name of the {@link IWebGradleProjectConfig} extension.
     */
    public static final String EXTENSION_NAME = "xtextWeb";

    public XtextWebPlugin() {
        super(SourceSet.MAIN_SOURCE_SET_NAME, EXTENSION_NAME, IWebGradleProjectConfig.class,
                WebGradleProjectConfig::new);
    }

}
