plugins {
    kotlin("multiplatform") version "2.4.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    jvm()

    js {
        // brauseri jaoks ES-moodul koos TypeScripti tüübikirjeldustega (vt web/)
        browser {
            // testid käivitatakse Node'is (jsNodeTest), brauserit selleks vaja ei ole
            testTask { enabled = false }
        }
        nodejs()
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// käsurea programm: ./gradlew run --args='...'
tasks.register<JavaExec>("run") {
    group = "application"
    description = "Käivitab simulatsiooni käsurealt."
    val jvmMain = kotlin.jvm().compilations.getByName("main")
    classpath(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    mainClass.set("MainKt")
    workingDir = projectDir
}

// testid loevad andmefaile projekti kaustast
tasks.withType<Test>().configureEach {
    workingDir = projectDir
}

// 2023. aasta ametliku tulemuse eksport: RESULTS.xml -> src/jvmMain/resources/rk2023/tulemus.json
tasks.register<JavaExec>("ekspordiRk2023") {
    group = "andmed"
    description = "Teisendab 2023. aasta ametliku tulemuse XML-faili JSON-failiks."
    val jvmMain = kotlin.jvm().compilations.getByName("main")
    classpath(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    mainClass.set("andmed.Rk2023EksportKt")
    workingDir = projectDir
    args("src/jvmTest/resources/rk2023/RESULTS.xml", "src/jvmMain/resources/rk2023/tulemus.json")
}
