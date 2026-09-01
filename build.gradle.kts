plugins {
    alias(libs.plugins.typst) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidxRoom3) apply false
}

// Корневые lifecycle-команды проекта: только JVM-тесты Desktop-приложения.
tasks.register("test") {
    group = "verification"
    description = "Runs all core and app tests on the JVM"
    dependsOn(":core:jvmTest", ":app:jvmTest")
}

tasks.register("build") {
    group = "build"
    description = "Builds the core and Desktop JVM application"
    dependsOn("test", ":core:build", ":app:build")
}
