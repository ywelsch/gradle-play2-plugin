package com.github.ywelsch.gradle.plugins.play2.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption

public class PlayEnhanceClasses extends AbstractionTransformationTask {
    @InputFiles
    FileCollection playEnhanceClassesClasspath

    @InputFiles
    FileCollection ebeanClasspath

    @InputFiles
    FileCollection classpath

    @InputDirectory
    File enhancementClasspath

    @Input
    Boolean ebeanEnabled

    @Input
    String ebeanModels

    @OutputDirectory
    def File outputDir


    @TaskAction
    protected void enhanceClasses(IncrementalTaskInputs inputs) {
        logger.info "Enhancing play classes for ${getProject()}"

        withClassLoader(getPlayEnhanceClassesClasspath()) { enhancerClassloader ->
            withOptionalClassLoader(getEbeanClasspath(), getEbeanEnabled()) { ebeanClassloader ->

                def generateAccessorsMethod = enhancerClassloader.loadClass('play.core.enhancers.PropertiesEnhancer')
                        .getMethod('generateAccessors', String.class, File.class)

                def rewriteAccessMethod = enhancerClassloader.loadClass('play.core.enhancers.PropertiesEnhancer')
                        .getMethod('rewriteAccess', String.class, File.class)

                def classpathString = getClasspath().asPath

                def inputStreamTransform = null
                def transformMethod = null
                def writeBytesMethod = null
                def stringReplaceMethod = null
                if (getEbeanEnabled()) {
                    def transformerClass = ebeanClassloader.loadClass('com.avaje.ebean.enhance.agent.Transformer')
                    def transformerConstructor = transformerClass.getConstructor(URL[].class, String.class)
                    def t = transformerConstructor.newInstance(toURLArray(getClasspath().files), "debug=-1")

                    def inputStreamTransformClass = ebeanClassloader.loadClass(
                            'com.avaje.ebean.enhance.agent.InputStreamTransform')
                    def inputStreamTransformConstructor = inputStreamTransformClass.getConstructor(
                            transformerClass, ClassLoader.class)
                    inputStreamTransform = inputStreamTransformConstructor.newInstance(t, ClassLoader.getSystemClassLoader())
                    transformMethod = inputStreamTransformClass.getMethod('transform', String.class, File.class)
                    writeBytesMethod = inputStreamTransformClass.getMethod('writeBytes', byte[].class, File.class)
                    def stringReplaceClass = ebeanClassloader.loadClass('com.avaje.ebean.enhance.ant.StringReplace')
                    stringReplaceMethod = stringReplaceClass.getMethod('replace', String.class, String.class, String.class)
                }

                inputs.outOfDate { change ->
                    forClassFileInDir(change.file, getEnhancementClasspath()) { relativizedFile ->
                        def outputFile = new File(getOutputDir(), relativizedFile.getPath())
                        outputFile.getParentFile().mkdirs()
                        Files.copy(change.file.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

                        // play getter / setter enhancement
                        generateAccessorsMethod.invoke(null, classpathString, outputFile)
                        logger.debug "Enhanced getters and setters for $relativizedFile"
                        rewriteAccessMethod.invoke(null, classpathString, outputFile)
                        logger.debug "Rewrite access methods for $relativizedFile"

                        // ebean enhancement
                        if (getEbeanEnabled()) {
                            String path = change.file.getAbsolutePath()
                            path = path.substring(getEnhancementClasspath().getAbsolutePath().length() + 1)
                            path = path.substring(0, path.length() - ".class".length())
                            def className = stringReplaceMethod.invoke(null, path, "\\", "/")

                            def classNamePath = FileSystems.getDefault().getPath(className)
                            def matcher = FileSystems.getDefault().getPathMatcher('glob:' + getEbeanModels().replace('.', '/'))
                            if (matcher.matches(classNamePath)) {
                                def result = transformMethod.invoke(inputStreamTransform, className, outputFile)
                                if (result != null) {
                                    writeBytesMethod.invoke(null, result, outputFile)
                                    logger.debug "Ebean enhanced $className"
                                }
                            }
                        }
                    }
                }

                inputs.removed { change ->
                    forClassFileInDir(change.file, getEnhancementClasspath()) { relativizedFile ->
                        def outputFile = new File(getOutputDir(), relativizedFile.getPath())
                        if (outputFile.exists()) {
                            outputFile.delete()
                        }
                    }
                }
            }
        }
    }
}
