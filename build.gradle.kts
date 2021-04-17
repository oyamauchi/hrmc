plugins {
    kotlin("jvm") version "1.3.61"
}

group = "com.owenyamauchi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.allWarningsAsErrors = true
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

/**
 * Custom task to create a jar with all dependencies included.
 */
task<Jar>("fatJar") {
    manifest {
        attributes("Main-Class" to "MainKt")
    }
    archiveClassifier.set("all")
    from(configurations.runtimeClasspath.get().files.map {
        if (it.isDirectory) it else zipTree(it)
    })
    with(tasks.jar.get())
}
