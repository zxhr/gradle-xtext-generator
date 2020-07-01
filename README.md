# Gradle Xtext Generator Plugins ![Build](https://github.com/zxhr/gradle-xtext-generator/workflows/Build/badge.svg)

gradle-xtext-generator is a set of Gradle plugins for configuring and using the
[Xtext Language Generator](https://www.eclipse.org/Xtext/documentation/302_configuration.html#generator) seamlessly
within Gradle. This includes:

* Automatically configuring the
  [XtextProjectConfig](https://www.eclipse.org/Xtext/documentation/302_configuration.html#project-configuration)
  based on how the [Gradle Project](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html) and
  [JavaPlugin](https://docs.gradle.org/current/userguide/java_plugin.html) are configured.
* [Xtext Language Configuration](https://www.eclipse.org/Xtext/documentation/302_configuration.html#language-configuration)
  and [CodeConfig](https://www.eclipse.org/Xtext/documentation/302_configuration.html#other-general-configuration)
  are configured inside the `build.gradle` obviating the need to use an `.mwe2` file.
* Generates the project's `build.properties` when imported into Eclipse so that dsl project can be launched and tested
  within Eclipse PDE.

These plugins have been successfully tested with Gradle 6.1 up to 6.5. They should work with newer versions as well.

## Usage

gradle-xtext-generator provides the following plugins:

* `com.github.zxhr.xtext-generator-root-project` - applied to the root Gradle project and used to configure the
  [Xtext Language Configuration](https://www.eclipse.org/Xtext/documentation/302_configuration.html#language-configuration)
  and [CodeConfig](https://www.eclipse.org/Xtext/documentation/302_configuration.html#other-general-configuration).
* `com.github.zxhr.xtext-generator-runtime` - configures the XtextProjectConfig's runtime project from the Gradle
  project the plugin is applied to.
* `com.github.zxhr.xtext-generator-runtime-test` - configures the XtextProjectConfig's runtimeTest project from the
  Gradle project the plugin is applied to.
* `com.github.zxhr.xtext-generator-generic-ide` - configures the XtextProjectConfig's genericIde project from the
  Gradle project the plugin is applied to.
* `com.github.zxhr.xtext-generator-eclipse-plugin` - configures the XtextProjectConfig's eclipsePlugin project from the
  Gradle project the plugin is applied to.
* `com.github.zxhr.xtext-generator-eclipse-plugin-test` - configures the XtextProjectConfig's eclipsePluginTest project
  from the Gradle project the plugin is applied to.
* `com.github.zxhr.xtext-generator-web` - configures the XtextProjectConfig's web project from the Gradle project the 
  plugin is applied to.

### Example Setup

#### `projectRoot/build.gradle`
```groovy
import org.eclipse.xtext.xtext.generator.StandardLanguage

buildscript {
    ext {
        xtextVersion = '2.22.0'
    }
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath enforcedPlatform("org.eclipse.xtext:xtext-dev-bom:$xtextVersion")
        classpath 'com.github.zxhr:gradle-xtext-generator:<plugin-version>'
        classpath "org.eclipse.xtext:org.eclipse.xtext.xtext.generator:$xtextVersion"
        classpath "org.eclipse.xtext:org.eclipse.xtext.common.types:$xtextVersion"
        classpath 'org.xtext:xtext-gradle-plugin:2.0.8'
    }
}

apply plugin: 'com.github.zxhr.xtext-generator-root-project'
apply plugin: 'org.xtext.xtend'

xtextRoot {
    language('MyDsl', StandardLanguage) {
        grammarUri = file('src/main/xtext/MyDsl.xtext').toURI().toString()
        fileExtensions = 'mydsl'

        /* Other language configuration */
    }
    codeConfig {
        preferXtendStubs = true // this plugin sets the default to false
    }
}
```

#### `projectRoot/my.dsl/build.gradle`
```groovy
plugins {
    id 'com.github.zxhr.xtext-generator-runtime'
    id 'com.github.zxhr.xtext-generator-runtime-test'
}

dependencies {
    implementation platform("org.eclipse.xtext:xtext-dev-bom:$xtextVersion")
    implementation "org.eclipse.xtext:org.eclipse.xtext:$xtextVersion"
    testImplementation "org.eclipse.xtext:org.eclipse.xtext.testing:$xtextVersion"
    testImplementation 'junit:junit:4.12'
}
```

#### `projectRoot/my.dsl.ide/build.gradle`
```groovy
plugins {
    id 'com.github.zxhr.xtext-generator-generic-ide'
}

dependencies {
    implementation project(':my.dsl')
    implementation platform("org.eclipse.xtext:xtext-dev-bom:$xtextVersion")
    implementation "org.eclipse.xtext:org.eclipse.xtext.ide:$xtextVersion"
}
```

#### `projectRoot/my.dsl.eclipse.ui/build.gradle`
```groovy
plugins {
    id 'com.github.zxhr.xtext-generator-eclipse-plugin'
    id 'com.github.zxhr.xtext-generator-eclipse-plugin-test'
}

dependencies {
    // ...
}
```

### Modifying the project's generated `plugin.xml`

#### `projectRoot/my.dsl/build.gradle`
```groovy
import groovy.util.XmlSlurper
import groovy.xml.XmlUtil

plugins {
    id 'com.github.zxhr.xtext-generator-runtime'
    id 'com.github.zxhr.xtext-generator-runtime-test'
}

dependencies {
    // ...
}

xtextRuntime {
    generateMwe2.configure {
        doLast {
            def xml = new XmlSlurper().parse(file(pluginXml))
            xml.appendNode {
                extension(point: 'org.example.extension.point') {
                    'package'(uri: 'https://www.example.com',
                              class: 'org.example.cls')
                }
            }
            file(pluginXml).text = XmlUtil.serialize(xml)
        }
    }
}
```

### Eclipse PDE Configuration

When a project is imported into Eclipse through Buildship, the gradle-xtext-generator plugins will generate
`build/pde/build.properties` and configure the project's `org.eclipse.pde.core.prefs` settings so that
the Xtext DSL projects can be launched within Eclipse PDE from the Plugin-manifest editor.

### Other Notes

* The `com.github.zxhr.xtext-generator-root-project` plugin configures the default `preferXtendStubs` to `false`.
* The plugins will set the project's
  [`ext.xtextVersion`](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.ExtraPropertiesExtension.html)
  to the xtext version used.
* The plugins include the Xtext-generated MANIFEST.MF into the Gradle
  [Jar](https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_tasks) task's
  [manifest](https://docs.gradle.org/current/javadoc/org/gradle/api/java/archives/Manifest.html);
  however, the plugins do not currently handle the logic for merging OSGi headers. For example, if you
  want to add another `Export-Package` value to what is generated by Xtext, you will either need to add
  it to the manifest generated from Xtext or manually combine it with the `Export-Package` list from the
  Xtext using Gradle's Jar manifest.
