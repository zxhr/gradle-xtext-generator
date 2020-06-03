package com.github.zxhr.gradle.xtext.model.project;

import org.eclipse.xtext.xtext.generator.model.project.RuntimeProjectConfig;
import org.gradle.api.file.DirectoryProperty;

/**
 * Configuration for the Xtext runtime sub-project, analogous to
 * {@link RuntimeProjectConfig}.
 */
public interface IRuntimeGradleProjectConfig extends IBundleGradleProjectConfig {

	/**
	 * Returns the directory for Xtext-generated model files.
	 * 
	 * @return the directory for Xtext-generated model files
	 */
	DirectoryProperty getEcoreModelDirectory();

}
