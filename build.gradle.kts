import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "fm.apakabar"
version =
    requireNotNull(
        Regex("""^## (\d+\.\d+\.\d+)$""", RegexOption.MULTILINE)
            .find(file("CHANGELOG.md").readText()),
    ) { "CHANGELOG.md has no released version heading" }.groupValues[1]

dependencies {
    api("fm.apakabar:readalign-kotlin:0.17.1")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation("io.heapy.kotaml:kotaml:0.111.0")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

dokka {
    dokkaPublications.html {
        moduleName.set("ReadAloudKit for Kotlin")
        moduleVersion.set(project.version.toString())
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        includes.from("docs/module.md")
    }
    dokkaSourceSets.configureEach {
        sourceRoots.from(file("src/main/kotlin"))
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl.set(uri("https://github.com/apakabarlabs/readaloudkit-kotlin/tree/main/src/main/kotlin"))
            remoteLineSuffix.set("#L")
        }
    }
}

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources(),
        ),
    )
    publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.PUBLISHED)
    if (!providers.gradleProperty("unsignedLocalPublish").isPresent) {
        signAllPublications()
    }
    coordinates("fm.apakabar", "readaloudkit-kotlin", version.toString())
    pom {
        name.set("ReadAloudKit for Kotlin")
        description.set("Decides whether a person reading a printed text aloud said what is written.")
        url.set("https://github.com/apakabarlabs/readaloudkit-kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("apakabarlabs")
                name.set("Apakabar")
                url.set("https://github.com/apakabarlabs")
            }
        }
        scm {
            url.set("https://github.com/apakabarlabs/readaloudkit-kotlin")
            connection.set("scm:git:https://github.com/apakabarlabs/readaloudkit-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/apakabarlabs/readaloudkit-kotlin.git")
        }
    }
}

ktlint {
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
