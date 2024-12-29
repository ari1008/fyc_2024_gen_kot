plugins {
    kotlin("jvm") version "2.1.0"
}

group = "com.fyc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}


gradle.startParameter.logLevel = LogLevel.INFO
tasks.register<GradleBuild>("cleanBuildWithKspDebug") {
    group = "build"
    description = "Clean and build the project with KSP debug enabled"
    tasks = listOf("clean", "build")
    startParameter.projectProperties["ksp.debug"] = "true"
    startParameter.projectProperties["ksp.info"] = "true"

}


// Configuration pour voir les logs de Gradle
gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS
gradle.startParameter.logLevel = LogLevel.INFO