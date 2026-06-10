import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.zeropointone.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.zeropointone.zeropointone"
            packageVersion = "1.0.0"
        }
    }
}

// Headless parameter-sweep runner (no GUI). Writes CSVs to ./results.
//   ./gradlew :composeApp:sweep
//   ./gradlew :composeApp:sweep -PsweepArgs="1500 20"   (ticks, seeds)
tasks.register<JavaExec>("sweep") {
    group = "application"
    description = "Run headless parameter sweeps and write CSV results to ./results"
    val jvmJar = tasks.named("jvmJar")
    dependsOn(jvmJar)
    classpath = files(jvmJar) + configurations.getByName("jvmRuntimeClasspath")
    mainClass.set("com.zeropointone.experiments.SweepKt")
    workingDir = rootProject.projectDir
    notCompatibleWithConfigurationCache("JavaExec sweep runs ad-hoc experiments")
    if (project.hasProperty("sweepArgs")) {
        args((project.property("sweepArgs") as String).split(" ").filter { it.isNotBlank() })
    }
}
