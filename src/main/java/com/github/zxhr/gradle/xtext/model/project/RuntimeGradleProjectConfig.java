package com.github.zxhr.gradle.xtext.model.project;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

public class RuntimeGradleProjectConfig extends BundleGradleProjectConfig implements IRuntimeGradleProjectConfig {

	private final DirectoryProperty ecoreModel;

	public RuntimeGradleProjectConfig(Project project) {
		super(project);
		this.ecoreModel = project.getObjects().directoryProperty();
	}

	@Override
	public DirectoryProperty getEcoreModelDirectory() {
		return ecoreModel;
	}

}
