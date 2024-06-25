import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.dokkatooHtml)
    alias(libs.plugins.dokkatooJavadoc)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.jacocoTestkit)
    alias(libs.plugins.gradleVersions)
    `java-gradle-plugin`
    `maven-publish`
    jacoco
}

group = "de.smartsquare"
version = libs.versions.squit

repositories {
    mavenCentral()
}

dependencies {
    api(libs.dom4j)
    api(libs.gson)
    api(libs.typesafeConfig)

    implementation(libs.jaxen)
    implementation(libs.pullParser)
    implementation(libs.xmlSec)
    implementation(libs.xmlUnit)
    implementation(libs.jsonUnit)
    implementation(libs.okhttp)
    implementation(libs.kotlinxHtml)
    implementation(libs.alphanumericComparator)
    implementation(libs.diffUtils) {
        exclude(group = "org.eclipse.jgit")
    }

    implementation(libs.jquery)
    implementation(libs.bootstrap)
    implementation(libs.popper)
    implementation(libs.fontAwesome)
    implementation(libs.marked)
    implementation(libs.diff2html) {
        exclude(group = "org.webjars.npm")
    }

    testImplementation(gradleTestKit())
    testImplementation(kotlin("reflect"))
    testImplementation(libs.junit)
    testImplementation(libs.junitParams)
    testImplementation(libs.h2)
    testImplementation(libs.kluent)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.mockk)

    testRuntimeOnly(libs.junitPlatformLauncher)
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }

    compilerOptions {
        allWarningsAsErrors = true
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

detekt {
    config.from(file("${rootDir}/detekt.yml"))

    buildUponDefaultConfig = true
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkatooGeneratePublicationJavadoc"))
}

dokkatoo {
    val dom4jVersion = resolveVersion("org.dom4j:dom4j")
    val typesafeConfigVersion = resolveVersion("com.typesafe:config")
    val gsonVersion = resolveVersion("com.google.code.gson:gson")

    dokkatooSourceSets.configureEach {
        externalDocumentationLinks.create("gradle") {
            url("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/")
            packageListUrl("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/element-list")
        }

        externalDocumentationLinks.create("dom4j") {
            url("https://javadoc.io/doc/org.dom4j/dom4j/$dom4jVersion/")
        }

        externalDocumentationLinks.create("config") {
            url("https://javadoc.io/doc/com.typesafe/config/$typesafeConfigVersion/")
        }

        externalDocumentationLinks.create("gson") {
            url("https://javadoc.io/doc/com.google.code.gson/gson/$gsonVersion")
            packageListUrl("https://javadoc.io/doc/com.google.code.gson/gson/$gsonVersion/element-list")
        }
    }
}

fun resolveVersion(dependency: String): String {
    return project.configurations.getByName("runtimeClasspath").resolvedConfiguration.resolvedArtifacts
        .find { it.moduleVersion.id.module.toString() == dependency }
        ?.moduleVersion?.id?.version
        ?: "latest"
}

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
