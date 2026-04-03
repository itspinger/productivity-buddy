plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "org.productivitybuddy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("org.productivitybuddy.productivitybuddy")
    mainClass.set("org.productivitybuddy.ProductivityBuddyLauncher")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
        exclude(group = "org.openjfx")
    }

    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-bootstrapicons-pack:12.3.1")
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    annotationProcessor("org.springframework:spring-context-indexer:6.2.2")

    implementation("org.springframework:spring-context:6.2.2")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.oshi:oshi-core:6.6.5")

    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Native packaging via jpackage (classpath mode, bundles JRE)
tasks.register<Exec>("nativePackage") {
    dependsOn("jar")
    group = "distribution"
    description = "Creates a native .dmg installer with bundled JRE via jpackage"

    val jpackageBin = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }.map { it.executablePath.asFile.parentFile.resolve("jpackage").absolutePath }

    val appJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    val libsDir = layout.buildDirectory.dir("nativeLibs")
    val outputDir = layout.buildDirectory.dir("nativePackage")

    // Collect all runtime dependencies into a single directory
    doFirst {
        val dir = libsDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        configurations.named("runtimeClasspath").get().resolve().forEach { file ->
            file.copyTo(File(dir, file.name), overwrite = true)
        }
        appJar.get().asFile.copyTo(File(dir, appJar.get().asFile.name), overwrite = true)
    }

    val installerType = when {
        org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "dmg"
        org.gradle.internal.os.OperatingSystem.current().isWindows -> "msi"
        else -> "deb"
    }

    executable = jpackageBin.get()
    args = listOf(
        "--type", installerType,
        "--name", "ProductivityBuddy",
        "--app-version", "1.0.0",
        "--input", libsDir.get().asFile.absolutePath,
        "--main-jar", appJar.get().asFile.name,
        "--main-class", "org.productivitybuddy.ProductivityBuddyLauncher",
        "--dest", outputDir.get().asFile.absolutePath
    )
}
