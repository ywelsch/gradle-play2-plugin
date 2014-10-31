package com.github.ywelsch.gradle.plugins.play2

import org.gradle.api.Project

class Play2JavaPlugin extends Play2BasePlugin {
    @Override
    void apply(Project project) {
        apply(project, ProjectMode.JAVA)
    }
}
