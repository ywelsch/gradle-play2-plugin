package com.github.ywelsch.gradle.plugins.play2.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class PlayCompileRoutes extends AbstractionTransformationTask {
    @InputFiles
    FileCollection playRoutesClasspath

    @InputFile
    File routesFile

    @Input
    Iterable<String> additionalImports

    @Input
    Boolean generateReverseRouter

    @Input
    Boolean generateRefReverseRouter

    @Input
    Boolean namespaceReverseRouter

    @Input
    Boolean oldCompiler

    @OutputDirectory
    def File outputDir

    @TaskAction
    protected void compileRoutes() {
        logger.info "Compiling play routes ${getProject().file(getRoutesFile())}"

        withClassLoader(getPlayRoutesClasspath()) { pluginClassloader ->
            def compileMethod

            if (getOldCompiler()) {
                compileMethod = pluginClassloader.loadClass('play.router.RoutesCompiler').getMethod('compile',
                        File.class, File.class, pluginClassloader.loadClass('scala.collection.Seq'), boolean.class, boolean.class)
            } else {
                compileMethod = pluginClassloader.loadClass('play.router.RoutesCompiler').getMethod('compile',
                        File.class, File.class, pluginClassloader.loadClass('scala.collection.Seq'), boolean.class, boolean.class, boolean.class)
            }

            def iterableAsScalaIterableMethod = pluginClassloader.loadClass('scala.collection.JavaConversions').getMethod('iterableAsScalaIterable',
                    Iterable.class)

            def additionalImportsScala = iterableAsScalaIterableMethod.invoke(null, getAdditionalImports()).toList()

            if (getOldCompiler()) {
                compileMethod.invoke(null, getProject().file(getRoutesFile()), outputDir, additionalImportsScala,
                        getGenerateReverseRouter(), getNamespaceReverseRouter())
            } else {
                compileMethod.invoke(null, getProject().file(getRoutesFile()), outputDir, additionalImportsScala,
                        getGenerateReverseRouter(), getGenerateRefReverseRouter(), getNamespaceReverseRouter())
            }
        }
    }
}
