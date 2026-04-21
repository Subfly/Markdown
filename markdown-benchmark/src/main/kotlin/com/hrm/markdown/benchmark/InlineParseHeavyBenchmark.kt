package com.hrm.markdown.benchmark

import com.hrm.markdown.parser.MarkdownParser
import kotlin.math.roundToLong

/**
 * 行内解析压力基准。
 *
 * 构造一份"行内元素密集"的文档（强调、内联代码、引用链接、GFM 自动链接、删除线、emoji）
 * 反复 `parser.parse(input)`，测量端到端解析耗时与每篇平均/吞吐。
 *
 * 用于回归比较 InlineParser 热路径优化（如 GFM autolink、normalizeLinkLabel、
 * appendText 文本缓冲复用、StringBuilder 等）的实际收益。
 *
 * 使用：
 *   ./gradlew :markdown-benchmark:inlineParseHeavyBenchmark \
 *       --args="--paragraphs 200 --warmups 5 --iterations 30"
 */
private const val DEFAULT_PARAGRAPHS = 200
private const val DEFAULT_WARMUPS = 5
private const val DEFAULT_ITERATIONS = 30

fun main(args: Array<String>) {
    val cli = HeavyCli.parse(args)
    val input = buildHeavyInlineMarkdown(cli.paragraphs)

    println("Inline Parse Heavy Benchmark")
    println("paragraphs=${cli.paragraphs}, chars=${input.length}, warmups=${cli.warmups}, iterations=${cli.iterations}")
    println()

    val parser = MarkdownParser()

    repeat(cli.warmups) { i ->
        val doc = parser.parse(input)
        if (i == 0) {
            println("warmup blockCount=${doc.children.size}")
        }
    }

    val durations = LongArray(cli.iterations)
    for (i in 0 until cli.iterations) {
        val start = System.nanoTime()
        parser.parse(input)
        durations[i] = System.nanoTime() - start
    }

    val avg = durations.average()
    val sorted = durations.sortedArray()
    val p50 = sorted[sorted.size / 2]
    val p95 = sorted[((sorted.size - 1) * 0.95).toInt()]
    val min = sorted.first()
    val max = sorted.last()
    val throughputMbPerSec = (input.length.toDouble() / (avg / 1_000_000_000.0)) / (1024.0 * 1024.0)

    println("Summary")
    println("avg=${(avg / 1_000_000.0).fmt()} ms  p50=${(p50 / 1_000_000.0).fmt()} ms  p95=${(p95 / 1_000_000.0).fmt()} ms")
    println("min=${(min / 1_000_000.0).fmt()} ms  max=${(max / 1_000_000.0).fmt()} ms")
    println("throughput=${"%.2f".format(throughputMbPerSec)} MB/s")
}

private data class HeavyCli(
    val paragraphs: Int,
    val warmups: Int,
    val iterations: Int,
) {
    companion object {
        fun parse(args: Array<String>): HeavyCli {
            var paragraphs = DEFAULT_PARAGRAPHS
            var warmups = DEFAULT_WARMUPS
            var iterations = DEFAULT_ITERATIONS
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--paragraphs" -> paragraphs = args.getOrNull(i + 1)?.toIntOrNull() ?: paragraphs
                    "--warmups" -> warmups = args.getOrNull(i + 1)?.toIntOrNull() ?: warmups
                    "--iterations" -> iterations = args.getOrNull(i + 1)?.toIntOrNull() ?: iterations
                }
                i += 2
            }
            return HeavyCli(paragraphs.coerceAtLeast(1), warmups.coerceAtLeast(0), iterations.coerceAtLeast(1))
        }
    }
}

private fun Double.fmt(): String = "%.3f".format(this)

/**
 * 构造一篇行内元素密集的 Markdown 文档：
 *   - 50% 单词带 `**emphasis**` 或 `_italic_`
 *   - 每段 1-2 个 `[link](https://example.com/path)` 引用链接
 *   - 每段 1 个 GFM 裸链接 `https://github.com/foo/bar`（触发 InlineParser GFM autolink 热路径）
 *   - 每段 1 个 `inline code`
 *   - 每段含若干 `~~strike~~` 与 `:emoji_name:`
 */
private fun buildHeavyInlineMarkdown(paragraphCount: Int): String = buildString {
    repeat(paragraphCount) { idx ->
        appendParagraph(idx)
        append("\n\n")
    }
}

private fun StringBuilder.appendParagraph(seed: Int) {
    val words = listOf(
        "Kotlin", "Multiplatform", "rendering", "incremental", "parser", "stream",
        "Compose", "performance", "optimize", "benchmark", "throughput", "latency",
        "memory", "allocation", "garbage", "collection", "regex", "substring",
    )
    val rng = (seed.toLong() * 2654435761L)
    var r = rng
    fun next(): Int { r = r * 6364136223846793005L + 1442695040888963407L; return ((r ushr 33) and 0xFFFF).toInt() }

    for (i in 0 until 24) {
        val w = words[next() % words.size]
        when (next() % 8) {
            0 -> append("**$w** ")
            1 -> append("_${w}_ ")
            2 -> append("`$w` ")
            3 -> append("~~$w~~ ")
            4 -> append(":smile: $w ")
            5 -> append("[$w](https://example.com/$w/$seed) ")
            6 -> append("https://github.com/$w/repo$seed ")
            else -> append("$w ")
        }
    }
}
