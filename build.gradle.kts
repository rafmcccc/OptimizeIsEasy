plugins {
    java
}

group = "com.optimizeiseasy"
version = "3.1.2"

// Compiled against the OLDEST supported API. javac refuses anything newer than the
// compile target, so every symbol we call provably exists on 1.21.1. CI also compiles
// the same sources against the newest API to catch upstream removals:
//   ./gradlew compileJava -PpaperApi=26.2.build.132-stable -Pjdk=25
val paperApi = providers.gradleProperty("paperApi").getOrElse("1.21.1-R0.1-SNAPSHOT")
val jdk = providers.gradleProperty("jdk").getOrElse("21")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.placeholderapi.com/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApi")
    compileOnly("me.clip:placeholderapi:2.11.6")

    testImplementation("io.papermc.paper:paper-api:$paperApi")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdk.toInt()))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // release tracks the toolchain on purpose: the shipped build is JDK 21 (release 21),
    // the compat check runs on 25 so Gradle will resolve a Java 25 paper-api at all.
    options.release.set(jdk.toInt())
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(mapOf("version" to project.version))
    }
    filesMatching("profiles/*.kos") {
        expand(mapOf("version" to project.version))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("OptimizeIsEasy")
    archiveClassifier.set("")
}
