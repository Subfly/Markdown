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

tasks.register<JavaExec>("llmStreamingBenchmark") {
    group = "benchmark"
    description = "Simulates LLM token-by-token streaming and reports per-append latency + frame-budget violations."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.hrm.markdown.benchmark.LlmStreamingBenchmarkKt")
}

tasks.register<JavaExec>("llmRecomposeBenchmark") {
    group = "benchmark"
    description = "Simulates LLM streaming and counts equivalent Compose block recompositions via stableKey/contentHash diff."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.hrm.markdown.benchmark.LlmRecomposeBenchmarkKt")
}

tasks.register<JavaExec>("mainThreadJankBenchmark") {
    group = "benchmark"
    description = "Measures whether running streaming append on the main thread causes frame jank, vs offloading to a background pool."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.hrm.markdown.benchmark.MainThreadJankBenchmarkKt")
}
