package com.github.ywelsch.gradle.plugins.play2

import org.gradle.api.Project

class Play2ScalaPlugin extends Play2BasePlugin {
    @Override
    void apply(Project project) {
        apply(project, ProjectMode.SCALA)
    }
}
