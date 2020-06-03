package com.github.zxhr.gradle.xtext.model.project;

import org.eclipse.xtext.xtext.generator.model.project.WebProjectConfig;
import org.gradle.api.file.DirectoryProperty;

/**
 * Configuration for an Xtext sub-project, analogous to
 * {@link WebProjectConfig}.
 */
public interface IWebGradleProjectConfig extends ISubGradleProjectConfig {

	/**
	 * Returns the assets directory used by Xtext.
	 * 
	 * @return the assets directory used by Xtext
	 */
	DirectoryProperty getAssetsDirectory();

}
