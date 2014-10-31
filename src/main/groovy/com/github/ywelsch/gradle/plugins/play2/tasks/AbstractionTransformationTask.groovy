package com.github.ywelsch.gradle.plugins.play2.tasks

import org.apache.tools.ant.AntClassLoader
import org.gradle.api.DefaultTask
import org.gradle.api.UncheckedIOException
import org.gradle.api.file.FileCollection

abstract class AbstractionTransformationTask extends DefaultTask {

    protected URL[] toURLArray(Set<File> files) {
        List<URL> urls = new ArrayList<URL>(files.size())
        files.each { file ->
            try {
                urls << file.toURI().toURL()
            }
            catch(MalformedURLException e) {
                throw new UncheckedIOException(e)
            }
        }
        urls.toArray(new URL[urls.size()])
    }

    protected File relativizeInputFile(File file, File dir) {
        return dir.toPath().relativize(file.toPath()).toFile()
    }

    protected withClassLoader(FileCollection fc, Closure cl) {
        ClassLoader rootClassLoader = new AntClassLoader(Thread.currentThread().getContextClassLoader(), false)
        URLClassLoader customClassloader = new URLClassLoader(toURLArray(fc.files), rootClassLoader)
        try {
            rootClassLoader = new AntClassLoader(Thread.currentThread().getContextClassLoader(), false)
            customClassloader = new URLClassLoader(toURLArray(fc.files), rootClassLoader)
            cl(customClassloader)
        } finally {
            if (customClassloader != null) {
                customClassloader.close()
            }
            if (rootClassLoader != null) {
                rootClassLoader.cleanup()
            }
        }
    }

    protected withOptionalClassLoader(FileCollection fc, boolean condition, Closure cl) {
        if (condition) {
            withClassLoader(fc, cl)
        } else {
            cl(null)
        }
    }

    protected forClassFileInDir(File file, File dir, Closure cl) {
        if (file.isFile() && file.name.endsWith('.class') && file.toPath().startsWith(dir.toPath())) {
            cl(relativizeInputFile(file, dir))
        }
    }
}
