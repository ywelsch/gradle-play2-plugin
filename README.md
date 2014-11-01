gradle-play2-plugin
===================

The gradle-play2-plugin enables the creation of Play 2 applications using Gradle.

Currently supported Play versions: [2.2.0 - 2.3.6]

Example Play 2 projects using the gradle plugin can be found [in the samples directory](samples)

*The plugin will be published on Maven Central very soon.*

## Scope

This gradle plugin provides the core functionality to develop Play 2 applications.
It includes routes and template compiler, class enhancer for getters and settes and ebean, and a fully-configured lifecycle.
It does not include, however, support for compiling LESS files, minifying JS files or other non-Play specific functionality.
Examples of integrating other Gradle plugins for that goal can be found in the samples directory, for example the [zentasks example](samples/playframework-2.3.6/samples/scala/zentasks/build.gradle).

## Running examples

All examples from the Play 2 distribution are fully running.
As long as the gradle-play2-plugin is not published on Maven Central, the examples can be run from the root project directory of the plugin as follows:

    gradlew samples -Pdir=samples/playframework-2.3.6/samples/scala/zentasks -PgradleTasks=run

This calls the run task on the zentasks example.

## Using the plugin

A typical usage of the plugin looks as follows:

    buildscript {
        repositories {
            mavenCentral()
        }
        dependencies {
            classpath 'com.github.ywelsch:gradle-play2-plugin:+'
        }
    }

    apply plugin: 'com.github.ywelsch.play2-java'

    play2 {
        version = '2.3.6'
        scalaVersion = '2.11'
    }

As the plugin heavily relies on the Play 2 libraries, the Typesafe repository needs to be included:

    repositories {
        mavenCentral()
        maven {
            url 'http://repo.typesafe.com/typesafe/releases'
        }
    }

### Java or Scala flavor

To configure the default lifecycle for building a Java Play 2 project, apply:

    apply plugin: 'com.github.ywelsch.play2-java'

To configure the default lifecycle for building a Scala Play 2 project, apply:

    apply plugin: 'com.github.ywelsch.play2-scala'

The default lifecycle includes support for packaging and running the Play 2 application.
To that effect, it relies on the Gradle application plugin, providing the standard `run`, `installApp`, `distZip` tasks.
The test framework is also pre-configured.

### Configuration

    play2 {
        // default configuration
        version = '2.3.6'
        scalaVersion = '2.11'
        appDir = new File('app')
        ebeanEnabled = false
        ebeanModels = 'models.*'
    }

### Tasks

The gradle-play2-plugin provides a number of tasks:

* PlayCompileRoutes
* PlayCompileTemplates
* PlayEnhanceClasses

#### PlayCompileTemplates

    task compilePlayTemplates(type: PlayCompileTemplates) {
        // default configuration
        templatesRootDir = play2.appDir
        templateImports = ["models._", "controllers._", ...] // List of imports
        templateFormats = ["html": "play.twirl.api.HtmlFormat", "txt": "play.twirl.api.TxtFormat", ...] // Map
        sourceEncoding = 'UTF-8'

        // Other more exotic configuration options
        playTemplatesClasspath = configurations.play2Templates
        useOldParser = false
        inclusiveDot = false
        oldCompiler = play2.version.startsWith('2.2.')

        // No default but pre-configured for standard task
        outputDir = file("$project.buildDir/generated-sources")
    }

#### PlayCompileRoutes

    task compilePlayRoutes(type: PlayCompileRoutes) {
        // default configuration
        additionalImports = ['play.libs.F', ...] // List of imports
        generateReverseRouter = true
        generateRefReverseRouter = true
        namespaceReverseRouter = false
        routesFile = file('conf/routes')

        // Other more exotic configuration options
        playRoutesClasspath = configurations.play2Routes
        oldCompiler = play2.version.startsWith('2.2.')

        // No default but pre-configured for standard task
        outputDir = file("$project.buildDir/generated-sources")
    }

#### PlayEnhanceClasses

    task playEnhanceClasses(type: PlayEnhanceClasses) {
        // default configuration
        ebeanEnabled = play2.ebeanEnabled
        ebeanModels = play2.ebeanModels
        outputDir = sourceSets.main.output.classesDir
        enhancementClasspath = file("$project.buildDir/classes/toBeEnhanced") // files to be enhanced
        classpath = configurations.compile + project.files(file("$project.buildDir/classes/toBeEnhanced"))

        // Other more exotic configuration options
        playEnhanceClassesClasspath = configurations.play2EnhanceClasses
        ebeanClasspath = configurations.ebean
    }

