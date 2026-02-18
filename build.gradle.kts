plugins {
    `java-library`
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.maven.publish)
}

group = "com.jamezrin"
version = "1.0.0"

base {
    archivesName = "solrj-dobby"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
    api(libs.solrj)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

spotless {
    java {
        googleJavaFormat(libs.versions.google.java.format.get())
        removeUnusedImports()
        importOrder("java|javax", "org.apache", "", "com.jamezrin")
    }
}

mavenPublishing {
    coordinates(group.toString(), "solrj-dobby", version.toString())

    pom {
        name = "solrj-dobby"
        description = "A modern replacement for SolrJ's DocumentObjectBinder"
        url = "https://github.com/jamezrin/solrj-dobby"
        inceptionYear = "2025"

        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "jamezrin"
                name = "Jaime Martinez Rincon"
                email = "mrjaime1999@gmail.com"
                url = "https://github.com/jamezrin"
            }
        }

        scm {
            connection = "scm:git:git://github.com/jamezrin/solrj-dobby.git"
            developerConnection = "scm:git:ssh://github.com:jamezrin/solrj-dobby.git"
            url = "https://github.com/jamezrin/solrj-dobby"
        }
    }

    publishToMavenCentral()
    signAllPublications()
}
