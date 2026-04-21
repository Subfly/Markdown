plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":markdown-renderer"))
    implementation(project(":markdown-parser"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.hrm.markdown.benchmark.StreamingRenderBenchmarkKt")
}

tasks.register<JavaExec>("inlineParseHeavyBenchmark") {
    group = "benchmark"
    description = "Runs the inline-parse-heavy benchmark (regress InlineParser hot path)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.hrm.markdown.benchmark.InlineParseHeavyBenchmarkKt")
}

tasks.register<JavaExec>("incrementalEditBenchmark") {
    group = "benchmark"
    description = "Runs the incremental-edit benchmark (regress IncrementalEngine.applyEdit)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.hrm.markdown.benchmark.IncrementalEditBenchmarkKt")
}
