gradle-play2-plugin
===================

The gradle-play2-plugin enables the creation of Play 2 applications using Gradle.

Currently supported Play versions: [2.2.0 - 2.3.6]

Example Play 2 projects using the gradle plugin can be found [in the samples directory](samples)

The plugin will be published on Maven Central very soon.

## Design decisions

This gradle plugin provides the core functionality to develop Play 2 applications.
It includes routes and template compiler, class enhancer for getters and settes and ebean, and a fully-configured lifecycle for copying public resources.
It does not include, however, support for compiling LESS files, minifying JS files or other non-Play specific functionality.
Examples of integrating other Gradle plugins for that goal can be found in the samples directory, for example the [zentasks example](samples/playframework-2.3.6/samples/scala/zentasks/build.gradle).