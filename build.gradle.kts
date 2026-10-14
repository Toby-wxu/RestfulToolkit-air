plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.13.0"
}

group = "me.kkk.restful.toolkit"
version = "2.1.6"

java {
    // Resolved from the local JDK or a downloaded one; never hardcode org.gradle.java.home,
    // that path only exists on the machine that created it.
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.fifesoft:rsyntaxtextarea:3.1.6")

    // Jupiter API is needed to compile the tests; the launcher itself comes from the
    // platform testFramework below.
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // The IntelliJ Platform test runtime referenced by IGP's `test` task still links
    // against JUnit 4 rules, so it has to be present even for Jupiter-only tests.
    testRuntimeOnly("junit:junit:4.13.2")

    intellijPlatform {
        create("IU", "2025.1")
        bundledPlugins(
            "com.intellij.java",
            "org.jetbrains.kotlin",
            "com.intellij.properties",
            "org.jetbrains.plugins.yaml"
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "RestfulToolkit-air"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "251"
            untilBuild = "261.*"
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
