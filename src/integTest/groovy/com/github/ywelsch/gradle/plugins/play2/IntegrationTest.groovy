package com.github.ywelsch.gradle.plugins.play2

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.internal.consumer.DefaultGradleConnector
import org.junit.After
import org.junit.Test

abstract class IntegrationTest {
    def projectName
    def playVersion
    def scalaVersion
    def lang
    def buildFileName
    def projectDir
    def buildFile
    def output

    public IntegrationTest(projectName, lang, playVersion, scalaVersion, buildFileName) {
        this.projectName = projectName
        this.playVersion = playVersion
        this.scalaVersion = scalaVersion
        this.lang = lang
        this.buildFileName = buildFileName
        projectDir = new File("samples/playframework-$playVersion/samples/$lang/$projectName")
        buildFile = new File(projectDir, buildFileName)
    }

    @After
    public void cleanUp() {
        new File(projectDir, 'build').deleteDir()
        new File(projectDir, '.gradle').deleteDir()
    }

    def initFile = new File('src/integTest/resources/init.gradle')

    @Test
    public void runTestTask() {
        if (!initFile.exists()) {
            throw new RuntimeException('Init file not found')
        }
        runTasks(['-u', '-b', buildFileName, '--init-script', initFile.absolutePath,
                  '-DtestContextProjectDir=' + new File('.').absolutePath, '-PscalaVersion=' + scalaVersion], 'test')
    }

    def samplesGradleHome = new File('samplesGradleHome')

    void runTasks(List<String> arguments = [], String... tasks) {
        ProjectConnection conn
        try {
            samplesGradleHome.mkdirs()
            GradleConnector gradleConnector = GradleConnector.newConnector()
                    .forProjectDirectory(projectDir)
                    .useGradleVersion('2.1')
                    .useGradleUserHomeDir(samplesGradleHome)
            // hack: cast to internal API to prevent new daemon from spawning
            // this allows us to fork a test per sample project which is properly cleaned
            ((DefaultGradleConnector)gradleConnector).embedded(true)
            conn = gradleConnector.connect()
            ByteArrayOutputStream stream = new ByteArrayOutputStream()
            def builder = conn.newBuild()
            if (arguments) {
                builder.withArguments(*arguments)
            }
            builder.forTasks(tasks).setStandardOutput(stream).run()
            output = stream.toString()
        }
        finally {
            conn?.close()
        }
    }
}
