package com.github.zxhr.gradle.xtext.model.project;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class SubGradleProjectConfig implements ISubGradleProjectConfig {

    protected final Project project;
    private final Property<String> name;
    private final DirectoryProperty metaInf;
    private final DirectoryProperty src;
    private final DirectoryProperty srcGen;
    private final DirectoryProperty icons;

    public SubGradleProjectConfig(Project project) {
        ObjectFactory objects = project.getObjects();
        this.project = project;
        this.name = objects.property(String.class);
        this.metaInf = objects.directoryProperty();
        this.src = objects.directoryProperty();
        this.srcGen = objects.directoryProperty();
        this.icons = objects.directoryProperty();
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Property<String> getProjectName() {
        return name;
    }

    @Override
    public DirectoryProperty getMetaInfDirectory() {
        return metaInf;
    }

    @Override
    public DirectoryProperty getSrcDirectory() {
        return src;
    }

    @Override
    public DirectoryProperty getSrcGenDirectory() {
        return srcGen;
    }

    @Override
    public DirectoryProperty getIconsDirectory() {
        return icons;
    }

}
