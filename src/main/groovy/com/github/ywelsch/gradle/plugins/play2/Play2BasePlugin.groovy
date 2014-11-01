package com.github.ywelsch.gradle.plugins.play2

import com.github.ywelsch.gradle.plugins.play2.extension.PlayPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.scala.ScalaCompile
import com.github.ywelsch.gradle.plugins.play2.tasks.PlayCompileRoutes
import com.github.ywelsch.gradle.plugins.play2.tasks.PlayCompileTemplates
import com.github.ywelsch.gradle.plugins.play2.tasks.PlayEnhanceClasses
import org.gradle.api.tasks.testing.Test

abstract class Play2BasePlugin implements Plugin<Project> {
    static final String COMPILE_TEMPLATES_CONFIGURATION_NAME = 'play2Templates'
    static final String COMPILE_ROUTES_CONFIGURATION_NAME = 'play2Routes'
    static final String ENHANCE_CLASSES_CONFIGURATION_NAME = 'play2EnhanceClasses'
    static final String EBEAN_CONFIGURATION_NAME = 'ebean'
    static final String PLAY_EXTENSION_NAME = 'play2'
    static final String PLAY_COMPILE_TEMPLATES_TASK_NAME = 'compilePlayTemplates'
    static final String PLAY_COMPILE_ROUTES_TASK_NAME = 'compilePlayRoutes'
    static final String PLAY_ENHANCE_CLASSES_TASK_NAME = 'playEnhanceClasses'

    enum ProjectMode { JAVA, SCALA }

    void apply(Project project, ProjectMode mode) {
        project.plugins.apply(ScalaPlugin.class)
        project.plugins.apply(ApplicationPlugin.class)

        project.configurations.create(COMPILE_TEMPLATES_CONFIGURATION_NAME).setVisible(false).setTransitive(true)
                .setDescription('The libraries to be used to compile the play templates for this project.')
        project.configurations.create(COMPILE_ROUTES_CONFIGURATION_NAME).setVisible(false).setTransitive(true)
                .setDescription('The libraries to be used to compile the play routes for this project.')
        project.configurations.create(ENHANCE_CLASSES_CONFIGURATION_NAME).setVisible(false).setTransitive(true)
                .setDescription('The libraries to be used to play enhance the classes for this project.')
        project.configurations.create(EBEAN_CONFIGURATION_NAME).setVisible(false).setTransitive(true)
                .setDescription('The libraries to be used to ebean enhance the classes for this project.')

        project.mainClassName = 'play.core.server.NettyServer'

        PlayPluginExtension playPluginExtension = project.extensions.create(PLAY_EXTENSION_NAME, PlayPluginExtension)


        // source folders
        project.sourceSets {
            main {
                java {
                    srcDirs = []
                }
                resources {
                    srcDirs = ['conf']
                }
                scala {
                    srcDirs = ['app']
                }
            }

            if (mode == ProjectMode.JAVA) {
                test {
                    java {
                        srcDirs = ['test']
                    }
                    scala {
                        srcDirs = []
                    }
                }
            } else {
                test {
                    java {
                        srcDirs = []
                    }
                    scala {
                        srcDirs = ['test']
                    }
                }
            }
        }

        project.afterEvaluate {
            // compile and testCompile configurations
            switch (mode) {
                case ProjectMode.JAVA:
                    project.dependencies.add(JavaPlugin.COMPILE_CONFIGURATION_NAME,
                            "com.typesafe.play:play-java_$playPluginExtension.scalaVersion:$playPluginExtension.version")
                    if (playPluginExtension.ebeanEnabled) {
                        project.dependencies.add(JavaPlugin.COMPILE_CONFIGURATION_NAME,
                                "com.typesafe.play:play-java-ebean_$playPluginExtension.scalaVersion:$playPluginExtension.version")
                    }
                    project.dependencies.add(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME,
                            "com.typesafe.play:play-test_$playPluginExtension.scalaVersion:$playPluginExtension.version")

                case ProjectMode.SCALA:
                    project.dependencies.add(JavaPlugin.COMPILE_CONFIGURATION_NAME,
                            "com.typesafe.play:play_$playPluginExtension.scalaVersion:$playPluginExtension.version")
                    project.dependencies.add(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME,
                            "com.typesafe.play:play-test_$playPluginExtension.scalaVersion:$playPluginExtension.version")
            }

            // play2 configuration
            project.dependencies.add(COMPILE_ROUTES_CONFIGURATION_NAME,
                    "com.typesafe.play:routes-compiler_2.10:$playPluginExtension.version")

            if (playPluginExtension.version.startsWith('2.2.')) {
                project.dependencies.add(COMPILE_TEMPLATES_CONFIGURATION_NAME,
                        "com.typesafe.play:templates-compiler_2.10:$playPluginExtension.version")
            } else {
                project.dependencies.add(COMPILE_TEMPLATES_CONFIGURATION_NAME,
                        "com.typesafe.play:twirl-compiler_2.10:1.0.2")
            }

            if (playPluginExtension.version.startsWith('2.2.')) {
                project.dependencies.add(ENHANCE_CLASSES_CONFIGURATION_NAME,
                        "com.typesafe.play:sbt-link:$playPluginExtension.version")
            } else {
                project.dependencies.add(ENHANCE_CLASSES_CONFIGURATION_NAME,
                        "com.typesafe.play:build-link:$playPluginExtension.version")
            }
            if (mode == ProjectMode.JAVA && playPluginExtension.ebeanEnabled) {
                project.dependencies.add(EBEAN_CONFIGURATION_NAME, 'org.avaje.ebeanorm:avaje-ebeanorm:3.3.4')
                project.dependencies.add(EBEAN_CONFIGURATION_NAME, 'org.avaje.ebeanorm:avaje-ebeanorm-agent:3.2.2')
            }
        }

        project.tasks.withType(JavaCompile) {
            options.encoding = "UTF-8"
        }

        project.tasks.withType(ScalaCompile) {
            options.encoding = "UTF-8"
            scalaCompileOptions.encoding = "UTF-8"
        }

        def generatedSourcesOutputDir = "$project.buildDir/generated-sources"

        project.tasks.withType(PlayCompileTemplates) {
            conventionMapping.map('templatesRootDir') { project.file(playPluginExtension.appDir) }
            conventionMapping.map('playTemplatesClasspath') {
                project.configurations.getByName(COMPILE_TEMPLATES_CONFIGURATION_NAME).asFileTree }
            conventionMapping.map('templateImports') {
                switch (mode) {
                    case ProjectMode.JAVA:
                        return [
                                "models._",
                                "controllers._",
                                "java.lang._",
                                "java.util._",
                                "scala.collection.JavaConversions._",
                                "scala.collection.JavaConverters._",
                                "play.api.i18n._",
                                "play.core.j.PlayMagicForJava._",
                                "play.mvc._",
                                "play.data._",
                                "play.api.data.Field",
                                "play.mvc.Http.Context.Implicit._",
                                "views.%format%._"
                        ]
                    case ProjectMode.SCALA:
                        return [
                                "models._",
                                "controllers._",
                                "play.api.i18n._",
                                "play.api.mvc._",
                                "play.api.data._",
                                "views.%format%._"
                        ]
                }
            }
            // neither java nor scala: "play.api.templates._", "play.api.templates.PlayMagic._"
            conventionMapping.map('sourceEncoding') { "UTF-8" }
            conventionMapping.map('useOldParser') { false }
            conventionMapping.map('inclusiveDot') { false }
            conventionMapping.map('templateFormats') {
                if (playPluginExtension.version.startsWith('2.2.')) {
                    ["html": "play.api.templates.HtmlFormat",
                     "txt": "play.api.templates.TxtFormat",
                     "xml": "play.api.templates.XmlFormat",
                     "js": "play.api.templates.JavaScriptFormat"]
                } else {
                    ["html": "play.twirl.api.HtmlFormat",
                     "txt": "play.twirl.api.TxtFormat",
                     "xml": "play.twirl.api.XmlFormat",
                     "js": "play.twirl.api.JavaScriptFormat"]
                }
            }
            conventionMapping.map('oldCompiler') { playPluginExtension.version.startsWith('2.2.') }
        }

        project.tasks.create(PLAY_COMPILE_TEMPLATES_TASK_NAME, PlayCompileTemplates) {
            description = 'Compiles the play template files.'
            outputDir = project.file(generatedSourcesOutputDir)
        }

        project.afterEvaluate {
            project.sourceSets.main.scala.srcDir project.tasks."$PLAY_COMPILE_TEMPLATES_TASK_NAME".outputDir
        }

        project.tasks.compileScala.dependsOn project.tasks.getByName(PLAY_COMPILE_TEMPLATES_TASK_NAME)

        project.tasks.withType(PlayCompileRoutes) {
            conventionMapping.map('playRoutesClasspath') {
                project.configurations.getByName(COMPILE_ROUTES_CONFIGURATION_NAME).asFileTree }
            conventionMapping.map('additionalImports') {
                def basePlugins = [] as List
                if (!playPluginExtension.version.startsWith('2.2.')) {
                    basePlugins += ['controllers.Assets.Asset']
                }
                if (mode == ProjectMode.JAVA) {
                    basePlugins += ['play.libs.F']
                }
                basePlugins
            }
            conventionMapping.map('generateReverseRouter') { true }
            conventionMapping.map('generateRefReverseRouter') { true }
            conventionMapping.map('namespaceReverseRouter') { false }
            conventionMapping.map('routesFile') { project.file(new File('conf/routes')) }
            conventionMapping.map('oldCompiler') { playPluginExtension.version.startsWith('2.2.') }
        }

        project.tasks.create(PLAY_COMPILE_ROUTES_TASK_NAME, PlayCompileRoutes) {
            description = 'Compiles the play routes files.'
            outputDir = project.file(generatedSourcesOutputDir)
        }

        project.afterEvaluate {
            project.sourceSets.main.scala.srcDir project.tasks."$PLAY_COMPILE_ROUTES_TASK_NAME".outputDir
        }

        project.tasks.compileScala.dependsOn project.tasks.getByName(PLAY_COMPILE_ROUTES_TASK_NAME)

        if (mode == ProjectMode.JAVA) {
            def classesToBeEnhancedDir = project.file("$project.buildDir/classes/toBeEnhanced")

            project.tasks.compileScala.destinationDir = classesToBeEnhancedDir

            project.tasks.withType(PlayEnhanceClasses) {
                conventionMapping.map('playEnhanceClassesClasspath') {
                    project.configurations.getByName(ENHANCE_CLASSES_CONFIGURATION_NAME).asFileTree }
                conventionMapping.map('ebeanClasspath') {
                    project.configurations.getByName(EBEAN_CONFIGURATION_NAME).asFileTree }
                conventionMapping.map('classpath') { project.configurations.compile + project.files(classesToBeEnhancedDir) }
                conventionMapping.map('enhancementClasspath') { classesToBeEnhancedDir }
                conventionMapping.map('ebeanEnabled') { playPluginExtension.ebeanEnabled }
                conventionMapping.map('ebeanModels') { playPluginExtension.ebeanModels }
                conventionMapping.map('outputDir') { project.sourceSets.main.output.classesDir }
            }

            project.tasks.create(PLAY_ENHANCE_CLASSES_TASK_NAME, PlayEnhanceClasses) {
                description = 'Enhances classes for play.'
                dependsOn project.tasks.compileScala
            }

            project.tasks.classes.dependsOn project.tasks.getByName(PLAY_ENHANCE_CLASSES_TASK_NAME)
        }



        def publicResources = "$project.buildDir/resources/publicResources"

        project.tasks.create('copyPublicResources', Sync) {
            description = 'Stages the resources from the public directory'
            from 'public'
            into "$publicResources/public"
        }

        project.sourceSets.main.output.dir(publicResources, builtBy: 'copyPublicResources')

        if (mode == ProjectMode.SCALA) {
            project.tasks.withType(Test) {
                systemProperty 'specs2.outDir', "$project.reportsDir/specs-report"
            }
        }
    }

}