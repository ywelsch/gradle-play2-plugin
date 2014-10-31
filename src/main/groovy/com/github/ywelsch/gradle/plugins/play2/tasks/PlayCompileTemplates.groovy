package com.github.ywelsch.gradle.plugins.play2.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.util.regex.Pattern

class PlayCompileTemplates extends AbstractionTransformationTask {
    @Input
    Iterable<String> templateImports

    @InputFiles
    FileCollection playTemplatesClasspath

    @InputDirectory
    def File templatesRootDir

    @OutputDirectory
    def File outputDir

    @Input
    String sourceEncoding

    @Input
    Boolean useOldParser

    @Input
    Boolean inclusiveDot

    @Input
    Map<String, String> templateFormats

    @Input
    Boolean oldCompiler

    public static final Pattern SCALA_FILE_PATTERN = Pattern.compile('.*\\.scala\\.(\\w+)$')

    @TaskAction
    protected void execute(IncrementalTaskInputs inputs) {
        logger.info "Compiling play templates for ${getProject()}"

        withClassLoader(getPlayTemplatesClasspath()) { pluginClassloader ->
            def compileMethod
            def compilerInstance
            def defaultCodec
            if (getOldCompiler()) {
                compileMethod = pluginClassloader.loadClass('play.templates.ScalaTemplateCompiler')
                        .getMethod('compile', File.class, File.class, File.class, String.class, String.class)
            } else {
                def codecClass = pluginClassloader.loadClass('scala.io.Codec')
                def compilerClass = pluginClassloader.loadClass('play.twirl.compiler.TwirlCompiler$')
                compileMethod = compilerClass.getMethod('compile',
                        File.class, File.class, File.class, String.class, String.class,
                        codecClass, boolean.class, boolean.class)
                compilerInstance = compilerClass.getField('MODULE$').get(null)

                defaultCodec = codecClass.getMethod('apply', String.class).invoke(null, getSourceEncoding())
            }

            inputs.outOfDate { change ->
                def relativizedFile = relativizeInputFile(change.file, getTemplatesRootDir())
                def matcher = SCALA_FILE_PATTERN.matcher(change.file.name)
                if (topMostParent(relativizedFile).name.equals('views') && matcher.matches()) {
                    def fileType = matcher.group(1)
                    logger.info "Compiling template file $change.file"
                    if (getOldCompiler()) {
                        compileMethod.invoke(null, change.file, getProject().file(getTemplatesRootDir()),
                                getProject().file(getOutputDir()),
                                getTemplateFormats().get(fileType),
                                "import play.api.templates._\nimport play.api.templates.PlayMagic._" + "\nimport "
                                        + getTemplateImports().collect{ it.replaceAll('%format%', fileType) }.join("\nimport "))
                    } else {
                        compileMethod.invoke(compilerInstance, change.file, getProject().file(getTemplatesRootDir()),
                                getProject().file(getOutputDir()),
                                getTemplateFormats().get(fileType),
                                ([""] + getTemplateImports().collect{ it.replaceAll('%format%', fileType) }).join("\nimport "),
                                defaultCodec,
                                getInclusiveDot(),
                                getUseOldParser())
                    }
                }
            }

            inputs.removed { change ->
                def relativizedFile = relativizeInputFile(change.file, getTemplatesRootDir())
                def matcher = SCALA_FILE_PATTERN.matcher(change.file.name)
                if (topMostParent(relativizedFile).name.equals('views') && matcher.matches()) {
                    def fileType = matcher.group(1)
                    def relativeTargetFile = new File(
                            new File(relativizedFile.path.replace('views', 'views/' + fileType)).parentFile,
                            change.file.name.replace('.scala.' + fileType, '.template.scala'))
                    def targetFileName = "$outputDir/$relativeTargetFile.path"
                    logger.info "Deleting template file $targetFileName"
                    def targetFile = project.file(targetFileName)
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                }
            }
        }
    }

    protected File topMostParent(File file) {
        if (file.parentFile == null) {
            file
        } else {
            topMostParent(file.parentFile)
        }
    }
}
