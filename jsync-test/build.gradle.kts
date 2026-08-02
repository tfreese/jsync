plugins {
    id("java")
}

description = "A Java rsync clone: Test Module"

dependencies {
    testImplementation(project(":jsync-remote-nio-server"))
    testImplementation(project(":jsync-remote-rsocket-server"))

    testImplementation("org.awaitility:awaitility")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.junit.jupiter:junit-jupiter")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    dependsOn(":jsync-remote-nio-server:build")
    dependsOn(":jsync-remote-rsocket-server:build")
}
