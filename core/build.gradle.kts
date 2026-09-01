plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidxRoom3)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}

dependencies {
    add("kspJvm", libs.androidx.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}
