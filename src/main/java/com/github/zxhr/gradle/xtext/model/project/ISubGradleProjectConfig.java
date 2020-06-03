package com.github.zxhr.gradle.xtext.model.project;

import org.eclipse.xtext.xtext.generator.model.project.SubProjectConfig;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration for an Xtext sub-project, analogous to
 * {@link SubProjectConfig}.
 */
public interface ISubGradleProjectConfig {

    /**
     * Returns the Gradle {@link Project} associated with this configuration.
     * 
     * @return the Gradle {@link Project} associated with this configuration
     */
    Project getProject();

    /**
     * Returns the name of the project.
     * 
     * @return the name of the project
     */
    Property<String> getProjectName();

    /**
     * Returns the directory for Xtext-generated {@code META-INF} files.
     * 
     * @return the directory for Xtext-generated {@code META-INF} files
     */
    DirectoryProperty getMetaInfDirectory();

    /**
     * Returns the source directory for Xtext-generated stub files.
     * 
     * @return the source directory for Xtext-generated stub files
     */
    DirectoryProperty getSrcDirectory();

    /**
     * Returns the source directory for Xtext-generated source files.
     * 
     * @return the source directory for Xtext-generated source files
     */
    DirectoryProperty getSrcGenDirectory();

    /**
     * Returns the icons directory used by Xtext.
     * 
     * @return the icons directory used by Xtext
     */
    DirectoryProperty getIconsDirectory();

}
