package com.github.zxhr.gradle.xtext.model.project;

import org.eclipse.xtext.xtext.generator.model.project.BundleProjectConfig;
import org.gradle.api.file.RegularFileProperty;

/**
 * Configuration for an Xtext bundle sub-project, analogous to
 * {@link BundleProjectConfig}.
 */
public interface IBundleGradleProjectConfig extends ISubGradleProjectConfig {

	/**
	 * Returns the file location of the Xtext-generated {@code MANIFEST.MF}.
	 * 
	 * @return the file location of the Xtext-generated {@code MANIFEST.MF}
	 */
	RegularFileProperty getManifest();

	/**
	 * Returns the file location of the Xtext-generated {@code plugin.xml}.
	 * 
	 * @return the file location of the Xtext-generated {@code plugin.xml}
	 */
	RegularFileProperty getPluginXml();

}
