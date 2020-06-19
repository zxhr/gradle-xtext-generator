package com.github.zxhr.gradle.xtext;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.emf.mwe2.language.Mwe2StandaloneSetup;
import org.eclipse.emf.mwe2.runtime.workflow.IWorkflowContext;
import org.eclipse.emf.mwe2.runtime.workflow.Workflow;
import org.eclipse.xtext.xtext.generator.XtextGenerator;
import org.eclipse.xtext.xtext.generator.XtextGeneratorLanguage;
import org.eclipse.xtext.xtext.generator.model.project.SubProjectConfig;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import com.google.inject.Injector;

/**
 * Task for generating an Xtext project.
 */
public abstract class GenerateMwe2 extends DefaultTask {

    public GenerateMwe2() {
        getGenerator().finalizeValueOnRead();
        ((Task) this).getInputs().files(getGenerator().map(generator -> {
            List<XtextGeneratorLanguage> languages = generator.getLanguageConfigs();
            List<File> grammars = new ArrayList<>(languages.size());
            for (XtextGeneratorLanguage language : languages) {
                URI uri = URI.create(language.getGrammarUri());
                if (uri.getScheme() == null || "file".equals(uri.getScheme())) {
                    grammars.add(new File(uri));
                }
            }
            return grammars;
        })).withPathSensitivity(PathSensitivity.NONE).withPropertyName("grammars");
        ((Task) this).getOutputs().dirs(getGenerator().map(generator -> {
            List<File> outputDirs = new ArrayList<>();
            for (SubProjectConfig projectConfig : generator.getConfiguration().getProject().getEnabledProjects()) {
                outputDirs.add(new File(projectConfig.getSrcGenPath()));
            }
            return outputDirs;
        })).withPropertyName("srcGenDirs");
    }

    /**
     * Returns the {@link XtextGenerator} used for generating the Xtext project.
     * 
     * @return the {@link XtextGenerator} used for generating the Xtext project
     */
    @Internal
    public abstract Property<XtextGenerator> getGenerator();

    @TaskAction
    protected void generateMwe2() {
        XtextGenerator generator = getGenerator().get();
        Injector injector = new Mwe2StandaloneSetup().createInjectorAndDoEMFRegistration();
        IWorkflowContext ctx = injector.getInstance(IWorkflowContext.class);
        Workflow workflow = new Workflow();
        workflow.addComponent(generator);
        workflow.invoke(ctx);
    }

}
