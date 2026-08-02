plugins {
    id("java")
    id("org.springframework.boot")
}

description = "A Java rsync clone: Swing-GUI"

dependencies {
    implementation(project(":jsync-remote-rsocket"))
    implementation(project(":jsync-remote-nio"))

    runtimeOnly("ch.qos.logback:logback-classic")
}

// Start: gradle bootRun --args="--spring.profiles.active=dev"
// The archive name. If the name has not been explicitly set, the pattern for the name is:
// [archiveBaseName]-[archiveAppendix]-[archiveVersion]-[archiveClassifier].[archiveExtension]
// archiveFileName = "my-boot.jar"
springBoot {
    mainClass.set("de.freese.jsync.swing.JSyncSwingLauncher")
}

// gradle bootRun --args="--spring.profiles.active=Server,HsqldbEmbeddedServer --server.port=65111"
// gradle bootRun Dspring-boot.run.arguments="65111"
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
//        args = listOf(
//                "--spring.profiles.active=Server,HsqldbEmbeddedServer"
//                , "--server.port=65111"
//        )
    jvmArgs = listOf(
        "-Xms32m",
        "-Xmx512m",
        "-XX:TieredStopAtLevel=1",
        "-Djava.security.egd=file:/dev/./urandom",
        "--enable-native-access=ALL-UNNAMED"
    )
    // -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
}
