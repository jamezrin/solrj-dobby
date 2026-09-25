import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    base
    alias(libs.plugins.spotless)
    alias(libs.plugins.maven.publish) apply false
}

group = "com.jamezrin"
version = "1.1.0"

repositories {
    mavenCentral()
}

val junitBom = libs.junit.bom
val junitJupiter = libs.junit.jupiter
val junitPlatformLauncher = libs.junit.platform.launcher
val testcontainersCore = libs.testcontainers.core
val testcontainersJunitJupiter = libs.testcontainers.junit.jupiter
val testcontainersSolr = libs.testcontainers.solr

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")
    apply(plugin = "com.vanniktech.maven.publish")

    group = rootProject.group
    version = rootProject.version

    val projectName = name
    val artifactId = "solrj-dobby-$projectName"
    val solrImage = if (projectName == "solr10") "solr:10" else "solr:9"

    base {
        archivesName.set(artifactId)
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        sourceSets.named("main") {
            java.setSrcDirs(
                listOf(
                    rootProject.file("src/main/java"),
                    rootProject.file("src/$projectName/java"),
                )
            )
        }
        sourceSets.named("test") {
            java.setSrcDirs(
                listOf(
                    rootProject.file("src/test/java"),
                    rootProject.file("src/$projectName/test/java"),
                )
            )
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        "testImplementation"(platform(junitBom))
        "testImplementation"(junitJupiter)
        "testImplementation"(testcontainersCore)
        "testImplementation"(testcontainersJunitJupiter)
        "testImplementation"(testcontainersSolr)
        "testRuntimeOnly"(junitPlatformLauncher)
    }

    tasks.withType<Test>().configureEach {
        systemProperty("solr.image", solrImage)
    }

    tasks.named<Test>("test") {
        useJUnitPlatform {
            excludeTags("integration")
        }
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    val javaExtension = extensions.getByType(JavaPluginExtension::class.java)
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests that require Docker"
        group = "verification"

        useJUnitPlatform {
            includeTags("integration")
        }

        val testSourceSet = javaExtension.sourceSets.getByName("test")
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath

        shouldRunAfter(tasks.named("test"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    configure<MavenPublishBaseExtension> {
        coordinates(group.toString(), artifactId, version.toString())

        pom {
            name.set(artifactId)
            description.set("A modern replacement for SolrJ's DocumentObjectBinder")
            url.set("https://github.com/jamezrin/solrj-dobby")
            inceptionYear.set("2025")

            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("jamezrin")
                    name.set("Jaime Martinez Rincon")
                    email.set("mrjaime1999@gmail.com")
                    url.set("https://github.com/jamezrin")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/jamezrin/solrj-dobby.git")
                developerConnection.set("scm:git:ssh://github.com:jamezrin/solrj-dobby.git")
                url.set("https://github.com/jamezrin/solrj-dobby")
            }
        }

        publishToMavenCentral()
        signAllPublications()
    }

    // Existing coordinate. Same jar as solrj-dobby-solr9, so SolrJ 9 projects keep working.
    if (projectName == "solr9") {
        configure<PublishingExtension> {
            publications.create<MavenPublication>("legacy") {
                from(components["java"])
                this.artifactId = "solrj-dobby"

                pom {
                    name.set("solrj-dobby")
                    description.set("A modern replacement for SolrJ's DocumentObjectBinder")
                    url.set("https://github.com/jamezrin/solrj-dobby")
                    inceptionYear.set("2025")

                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("jamezrin")
                            name.set("Jaime Martinez Rincon")
                            email.set("mrjaime1999@gmail.com")
                            url.set("https://github.com/jamezrin")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/jamezrin/solrj-dobby.git")
                        developerConnection.set("scm:git:ssh://github.com:jamezrin/solrj-dobby.git")
                        url.set("https://github.com/jamezrin/solrj-dobby")
                    }
                }
            }
        }
        afterEvaluate {
            extensions.getByType(PublishingExtension::class.java)
                .publications
                .named<MavenPublication>("legacy")
                .configure {
                    artifact(tasks.named("plainJavadocJar"))
                }
        }
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(libs.versions.google.java.format.get())
        removeUnusedImports()
        importOrder("java|javax", "org.apache", "", "com.jamezrin")
    }
}
