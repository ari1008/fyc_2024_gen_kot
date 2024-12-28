plugins {
    kotlin("jvm") version "2.1.0"  // Match your existing Kotlin version
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"  // Updated KSP version for Kotlin 2.0.21
    id("application")
}

group = "com.fyc.start"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS
gradle.startParameter.logLevel = LogLevel.INFO

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory}/generated/ksp/main/kotlin")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    implementation(project(":annotation"))
    ksp(project(":processor"))
    kspTest(project(":processor"))
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.8.0") // Match Kotlin version
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.fyc.start.MainKt")
}