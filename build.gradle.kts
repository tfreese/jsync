// Execute Tasks in SubModule: gradle MODUL:clean build
plugins {
    id("de.freese.gradle.conventions").apply(false)
    // id("io.spring.dependency-management").apply(false)
    id("org.springframework.boot").apply(false)
}

allprojects {
    plugins.apply("base")
}

subprojects {
    plugins.apply("de.freese.gradle.conventions")

    pluginManager.withPlugin("java") {
        dependencies {
            add("implementation", platform(project(":platform")))

            // add("testImplementation", "org.junit.jupiter:junit-jupiter")
            // add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
    }
}
