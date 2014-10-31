package com.github.ywelsch.gradle.plugins.play2.extension

class PlayPluginExtension {
    Boolean ebeanEnabled = false
    String ebeanModels = 'models.*'
    String version = '2.3.5'
    String scalaVersion = '2.10'
    File appDir = new File('app')
}
