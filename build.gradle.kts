plugins {
    `java-library`
    id("com.diffplug.spotless") version "7.0.3"
}

group = "com.jamezrin"
version = "0.2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // SolrJ as an API dependency so it's transitively exposed to consumers
    api("org.apache.solr:solr-solrj:9.10.1")

    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        googleJavaFormat("1.26.0")
        removeUnusedImports()
        importOrder("java|javax", "org.apache", "", "com.jamezrin")
    }
}
