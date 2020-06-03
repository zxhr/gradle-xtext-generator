package com.github.zxhr.gradle.xtext.model.project;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;

public class BundleGradleProjectConfig extends SubGradleProjectConfig implements IBundleGradleProjectConfig {

	private final RegularFileProperty manifest;
	private final RegularFileProperty pluginXml;

	public BundleGradleProjectConfig(Project project) {
		super(project);
		this.manifest = project.getObjects().fileProperty();
		this.pluginXml = project.getObjects().fileProperty();
	}

	@Override
	public RegularFileProperty getManifest() {
		return manifest;
	}

	@Override
	public RegularFileProperty getPluginXml() {
		return pluginXml;
	}

}
