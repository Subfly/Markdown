package com.hrm.markdown.benchmark

import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.core.HtmlEntities
import kotlin.system.measureNanoTime

/**
 * Cold-start benchmark.
 *
 * 量化 LLM 应用首次加载 markdown-parser 时的延迟，特别是：
 *   1. 第一次创建 [MarkdownParser] 的成本（FlavourCache、PostProcessor 注册等）
 *   2. 第一次访问 [HtmlEntities] object 的成本（2125 entries eager init）
 *   3. 第一次 parse 一段含/不含 HTML 实体的 markdown 的端到端延迟
 *
 * 必须在新 JVM 进程里跑，每次只测一项 — 因为 class loader 一旦加载就永久在内存里。
 *
 * Usage:
 *   ./gradlew :markdown-benchmark:coldStartBenchmark
 *   ./gradlew :markdown-benchmark:coldStartBenchmark --args="--isolate htmlentities"
 *   ./gradlew :markdown-benchmark:coldStartBenchmark --args="--isolate parser"
 *   ./gradlew :markdown-benchmark:coldStartBenchmark --args="--isolate firstparse-noentity"
 *   ./gradlew :markdown-benchmark:coldStartBenchmark --args="--isolate firstparse-withentity"
 */
fun main(args: Array<String>) {
    val isolate = args.indexOf("--isolate").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }

    if (isolate == null) {
        println("Cold-Start Benchmark (single-process; numbers AFTER the first measurement are misleading)")
        println("To get reliable cold-start numbers, run with --isolate <stage>:")
        println("  htmlentities         : just the HtmlEntities object class init")
        println("  parser               : MarkdownParser() constructor only")
        println("  firstparse-noentity  : first parse, content has no &xxx; entities")
        println("  firstparse-withentity: first parse, content includes HTML entities")
        println()
        println("Running all stages sequentially anyway (only stage 1 is reliable cold-start):")
        println()
        runStage("htmlentities (CLASS INIT)") {
            // Touch the object — first reference triggers <clinit>
            HtmlEntities.resolve("amp")
        }
        runStage("parser (constructor only)") { MarkdownParser() }
        runStage("firstparse-noentity") {
            MarkdownParser().parse(SAMPLE_NO_ENTITY)
        }
        runStage("firstparse-withentity") {
            MarkdownParser().parse(SAMPLE_WITH_ENTITY)
        }
        return
    }

    when (isolate) {
        "htmlentities" -> runStage("htmlentities CLASS INIT (cold)") {
            HtmlEntities.resolve("amp")
        }
        "parser" -> runStage("MarkdownParser() (cold)") { MarkdownParser() }
        "firstparse-noentity" -> runStage("first parse no-entity (cold)") {
            MarkdownParser().parse(SAMPLE_NO_ENTITY)
        }
        "firstparse-withentity" -> runStage("first parse with-entity (cold)") {
            MarkdownParser().parse(SAMPLE_WITH_ENTITY)
        }
        else -> {
            println("Unknown stage: $isolate")
            return
        }
    }
}

private fun runStage(name: String, block: () -> Unit) {
    val ns = measureNanoTime { block() }
    val ms = ns / 1_000_000.0
    println("  $name: ${"%.3f".format(ms)} ms")
}

private val SAMPLE_NO_ENTITY = """
# Hello World

This is a paragraph with **bold**, *italic*, and `inline code`.

- list item one
- list item two

```kotlin
fun main() = println("hi")
```
""".trimIndent()

private val SAMPLE_WITH_ENTITY = """
# Hello &amp; World

This text has &nbsp; and &copy; and &mdash; entities, plus numeric &#x1F4A9; and &#169;.

| col &alpha; | col &beta; |
|------|------|
| &lt;tag&gt; | &quot;quoted&quot; |
""".trimIndent()
