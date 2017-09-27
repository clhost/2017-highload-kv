// See https://gradle.org and https://github.com/gradle/kotlin-dsl

// Apply the java plugin to add support for Java
plugins {
    java
    application
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    // Annotations for better code documentation
    compile("com.intellij:annotations:12.0")

    // Jetty
    compile("org.eclipse.jetty:jetty-server:9.4.6.v20170531")
    compile("org.eclipse.jetty:jetty-webapp:9.4.6.v20170531")

    // Logging
    compile("org.apache.logging.log4j:log4j-api:2.8.2")
    compile("org.apache.logging.log4j:log4j-core:2.8.2")

    // JUnit test framework
    testCompile("junit:junit:4.12")

    // HTTP client for unit tests
    testCompile("org.apache.httpcomponents:fluent-hc:4.5.3")
}

tasks {
    "test"(Test::class) {
        maxHeapSize = "1g"
    }
}

application {
    // Define the main class for the application
    mainClassName = "ru.mail.polis.Server"

    // And limit Xmx
    applicationDefaultJvmArgs = listOf("-Xmx1g")
}
