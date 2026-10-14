plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.13.0"
}

group = "com.fengjianliang.restful.toolkit"
version = "1.0.0"

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
            sinceBuild = "222"
            untilBuild = "262.*"
        }
    }

    pluginVerification {
        ides {
            // Local paths can be supplied for offline verification; never commit machine paths.
            val compatibilityIdes = providers.gradleProperty("compatibilityIdePaths").orNull
            if (compatibilityIdes != null) {
                compatibilityIdes.split(';').filter { it.isNotBlank() }.forEach { local(it) }
            } else {
                // IGP's IDE resolver requires 223+; verify 222 with the standalone CLI (README).
                create("IC", "2022.3.3")
                create("IU", "2025.1")
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // Gradle/the compilation SDK use Java 21; IDEA 2022.2 runs on Java 17.
    options.release = 17
}

tasks.withType<Test> {
    useJUnitPlatform()
}
