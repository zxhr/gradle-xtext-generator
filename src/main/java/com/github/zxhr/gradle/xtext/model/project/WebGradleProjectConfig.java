package com.github.zxhr.gradle.xtext.model.project;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

public class WebGradleProjectConfig extends SubGradleProjectConfig implements IWebGradleProjectConfig {

	private final DirectoryProperty assets;

	public WebGradleProjectConfig(Project project) {
		super(project);
		this.assets = project.getObjects().directoryProperty();
	}

	@Override
	public DirectoryProperty getAssetsDirectory() {
		return assets;
	}

}
