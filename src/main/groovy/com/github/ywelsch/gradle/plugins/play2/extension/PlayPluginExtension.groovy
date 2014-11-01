package com.github.ywelsch.gradle.plugins.play2.extension

class PlayPluginExtension {
    Boolean ebeanEnabled = false
    String ebeanModels = 'models.*'
    String version = '2.3.6'
    String scalaVersion = '2.11'
    File appDir = new File('app')
}
