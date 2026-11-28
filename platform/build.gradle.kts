plugins {
    id("java-platform")
}

javaPlatform {
    allowDependencies()
}

// https://docs.gradle.org/current/userguide/platforms.html
dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:" + property("version_springBoot")))

    constraints {
        api("commons-cli:commons-cli:" + property("version_commonsCli"))
    }
}
