import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.gradle.node.pnpm.task.PnpmTask
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.node)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.jacoco.testkit)
    alias(libs.plugins.publish)
    alias(libs.plugins.versions)
    jacoco
}

group = "de.smartsquare"
version = System.getenv("GITHUB_VERSION") ?: "1.0.0-SNAPSHOT"
description = "Automated testing of JSON, XML, SOAP and other apis"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.dom4j)
    api(libs.gson)
    api(libs.typesafe.config)

    implementation(libs.jaxen)
    implementation(libs.pull.parser)
    implementation(libs.xml.sec)
    implementation(libs.xml.unit)
    implementation(libs.json.unit)
    implementation(libs.okhttp)
    implementation(libs.alphanumeric.comparator)
    implementation(libs.diff.utils) {
        exclude(group = "org.eclipse.jgit")
    }

    testImplementation(gradleTestKit())
    testImplementation(kotlin("reflect"))
    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testImplementation(libs.h2)
    testImplementation(libs.kluent)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.mockk)

    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors = true
    }
}

node {
    nodeProjectDir = file("html-report")
}

val buildHtmlReport = tasks.register<PnpmTask>("buildHtmlReport") {
    dependsOn(tasks.pnpmInstall)

    args = listOf("run", "build")

    inputs.files(
        fileTree("html-report") {
            exclude("node_modules/**")
            exclude("dist/**")
        },
    )

    outputs.file("report-frontend/dist/index.html")
}

tasks.processResources {
    dependsOn(buildHtmlReport)

    from("html-report/dist/index.html") {
        rename { "squit-report-template.html" }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }

    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = libs.versions.jacoco.asProvider().get()
}

detekt {
    config.setFrom("$rootDir/detekt.yml")

    buildUponDefaultConfig = true
}

tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaGeneratePublicationJavadoc)
}

dokka {
    val dom4jVersion = resolveVersion("org.dom4j:dom4j")
    val typesafeConfigVersion = resolveVersion("com.typesafe:config")
    val gsonVersion = resolveVersion("com.google.code.gson:gson")

    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("gradle") {
            url("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/")
            packageListUrl("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/element-list")
        }

        externalDocumentationLinks.register("dom4j") {
            url("https://javadoc.io/doc/org.dom4j/dom4j/$dom4jVersion/")
        }

        externalDocumentationLinks.register("config") {
            url("https://javadoc.io/doc/com.typesafe/config/$typesafeConfigVersion/")
        }

        externalDocumentationLinks.register("gson") {
            url("https://javadoc.io/doc/com.google.code.gson/gson/$gsonVersion")
            packageListUrl("https://javadoc.io/doc/com.google.code.gson/gson/$gsonVersion/element-list")
        }
    }
}

fun resolveVersion(dependency: String): String =
    project.configurations.getByName("runtimeClasspath").resolvedConfiguration.resolvedArtifacts
        .find { it.moduleVersion.id.module.toString() == dependency }
        ?.moduleVersion?.id?.version
        ?: "latest"

gradlePlugin {
    website = "https://github.com/SmartsquareGmbH/squit"
    vcsUrl = "https://github.com/SmartsquareGmbH/squit"

    plugins {
        create("squit") {
            displayName = "Squit"
            id = "de.smartsquare.squit"
            implementationClass = "de.smartsquare.squit.SquitPlugin"
            description = "Gradle plugin for simple testing of JSON/XML/SOAP/etc APIs."
            tags = listOf("testing", "soap", "xml", "automation")
        }
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return !isStable
}

tasks.withType<Wrapper> {
    gradleVersion = libs.versions.gradle.get()
}
