// Can not be configured by Conventions-Plugin.
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    val versionMyJavaConventionPlugin = providers.gradleProperty("version_myJavaConventionPlugin")
    val version_springDependencyManagementPlugin = providers.gradleProperty("version_springDependencyManagementPlugin")
    val version_springBoot = providers.gradleProperty("version_springBoot")

    plugins {
        id("de.freese.gradle.conventions").version(versionMyJavaConventionPlugin).apply(false)
        id("io.spring.dependency-management").version(version_springDependencyManagementPlugin).apply(false)
        id("org.springframework.boot").version(version_springBoot).apply(false)
    }
}

// Without rootProject.name the Name of the Projekt-Directory is used.
// rootProject.name = "jsync"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

include("jsync-core")
include("jsync-console")
include("jsync-remote-rsocket")
include("jsync-remote-rsocket-server")
include("jsync-remote-nio")
include("jsync-remote-nio-server")
include("jsync-swing")
include("jsync-test")
