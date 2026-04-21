package com.hrm.markdown.benchmark

import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.ast.Document
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * Renderer 层流式重组基准。
 *
 * Compose 渲染器使用 `key(block.stableKey)` 包裹每个顶层块。
 * 对于稳定参数，Compose 会跳过未变更的块（smart skip）。
 * 因此「真正的重组次数」 = ∑ over chunks of:
 *   - 新增块: prev 中不存在 stableKey
 *   - 内容变更块: stableKey 相同但 contentHash 不同
 *   - 跳过的块: stableKey 相同且 contentHash 相同（Compose 不会重组）
 *
 * 这是 LLM 流式展示的核心 UI 性能指标：每个 token 实际触发多少块级重组。
 *
 * 使用：
 *   ./gradlew :markdown-benchmark:llmRecomposeBenchmark \
 *       --args="--bytes 50000 --tokenSize 4 --warmups 2 --iterations 3"
 */
private const val DEFAULT_BYTES = 50_000
private const val DEFAULT_TOKEN_SIZE = 4
private const val DEFAULT_WARMUPS = 2
private const val DEFAULT_ITERATIONS = 3

fun main(args: Array<String>) {
    val cli = RecomposeCli.parse(args)
    val doc = buildLlmLikeDocumentForRecompose(cli.bytes)
    val chunks = chunkLikeLlmForRecompose(doc, cli.tokenSize, Random(0xC0FFEEFEEDL))

    println("LLM Renderer Recompose Benchmark")
    println("docBytes=${doc.length}, chunks=${chunks.size}, avgChunk=${doc.length / chunks.size}")
    println("warmups=${cli.warmups}, iterations=${cli.iterations}")
    println()

    repeat(cli.warmups) { runRecomposeIteration(chunks) }

    val results = Array(cli.iterations) { runRecomposeIteration(chunks) }

    val totalAdded = results.map { it.totalAdded }.average()
    val totalChanged = results.map { it.totalChanged }.average()
    val totalSkipped = results.map { it.totalSkipped }.average()
    val totalRemoved = results.map { it.totalRemoved }.average()

    val perChunkAdded = totalAdded / chunks.size
    val perChunkChanged = totalChanged / chunks.size
    val perChunkSkipped = totalSkipped / chunks.size

    val recomposeWork = totalAdded + totalChanged
    val totalBlockSeen = totalAdded + totalChanged + totalSkipped
    val skipRate = if (totalBlockSeen > 0) totalSkipped / totalBlockSeen * 100 else 0.0

    val streamMs = results.map { it.streamNs / 1_000_000.0 }.average()
    val finalBlocks = results.map { it.finalBlocks }.average()

    println("Per-iteration totals (avg over ${cli.iterations} iterations)")
    println("  added blocks    = ${totalAdded.fmt()}")
    println("  changed blocks  = ${totalChanged.fmt()}")
    println("  skipped blocks  = ${totalSkipped.fmt()}  (Compose smart-skip)")
    println("  removed blocks  = ${totalRemoved.fmt()}")
    println()
    println("Compose work")
    println("  recompose work  = ${recomposeWork.fmt()}  (added + changed)")
    println("  smart-skip rate = ${skipRate.fmt()}%   (the higher the better)")
    println()
    println("Per-chunk averages (≈ per LLM token)")
    println("  added    / chunk = ${perChunkAdded.fmt()}")
    println("  changed  / chunk = ${perChunkChanged.fmt()}")
    println("  skipped  / chunk = ${perChunkSkipped.fmt()}")
    val recomposePerChunk = perChunkAdded + perChunkChanged
    println("  recompose/chunk  = ${recomposePerChunk.fmt()}  (avg block recomposes per token)")
    println()
    println("Reference")
    println("  final block count = ${finalBlocks.fmt()}")
    println("  streaming time    = ${streamMs.fmt()} ms")
}

private data class RecomposeIterResult(
    val totalAdded: Long,
    val totalChanged: Long,
    val totalSkipped: Long,
    val totalRemoved: Long,
    val finalBlocks: Int,
    val streamNs: Long,
)

private fun runRecomposeIteration(chunks: List<String>): RecomposeIterResult {
    val parser = MarkdownParser()
    var added = 0L
    var changed = 0L
    var skipped = 0L
    var removed = 0L
    // Snapshot of previous top-level blocks: stableKey -> contentHash
    var prev: HashMap<Int, Long> = HashMap()
    val streamNs = measureNanoTime {
        for (c in chunks) {
            val doc: Document = parser.append(c)
            val curr = HashMap<Int, Long>(doc.children.size * 2)
            for (block in doc.children) {
                val key = block.stableKey
                val hash = block.contentHash
                curr[key] = hash
                val prevHash = prev[key]
                when {
                    prevHash == null -> added++
                    prevHash != hash -> changed++
                    else -> skipped++
                }
            }
            for (oldKey in prev.keys) {
                if (!curr.containsKey(oldKey)) removed++
            }
            prev = curr
        }
        parser.endStream()
    }
    return RecomposeIterResult(added, changed, skipped, removed, prev.size, streamNs)
}

private data class RecomposeCli(
    val bytes: Int,
    val tokenSize: Int,
    val warmups: Int,
    val iterations: Int,
) {
    companion object {
        fun parse(args: Array<String>): RecomposeCli {
            var bytes = DEFAULT_BYTES
            var tokenSize = DEFAULT_TOKEN_SIZE
            var warmups = DEFAULT_WARMUPS
            var iterations = DEFAULT_ITERATIONS
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--bytes" -> bytes = args.getOrNull(i + 1)?.toIntOrNull() ?: bytes
                    "--tokenSize" -> tokenSize = args.getOrNull(i + 1)?.toIntOrNull() ?: tokenSize
                    "--warmups" -> warmups = args.getOrNull(i + 1)?.toIntOrNull() ?: warmups
                    "--iterations" -> iterations = args.getOrNull(i + 1)?.toIntOrNull() ?: iterations
                }
                i += 2
            }
            return RecomposeCli(
                bytes.coerceAtLeast(1024),
                tokenSize.coerceAtLeast(1),
                warmups.coerceAtLeast(0),
                iterations.coerceAtLeast(1),
            )
        }
    }
}

private fun Double.fmt(): String = "%.2f".format(this)

private fun buildLlmLikeDocumentForRecompose(targetBytes: Int): String = buildString(targetBytes + 1024) {
    var section = 0
    while (length < targetBytes) {
        section++
        append("## Section ").append(section).append(": Topic ").append(section).append('\n')
        append('\n')
        append("Here is a paragraph about *topic ").append(section)
            .append("* with some **emphasis** and `inline code` plus a [link](https://example.com/")
            .append(section).append("). It also references `foo()` and `bar(x, y)`.\n")
        append('\n')
        append("- bullet item ").append(section).append("-1 with **bold**\n")
        append("- bullet item ").append(section).append("-2 with [a link](https://example.com)\n")
        append("- bullet item ").append(section).append("-3 with `inline code`\n")
        append('\n')
        append("```kotlin\n")
        append("fun example").append(section).append("(x: Int): Int {\n")
        append("    val sum = (0..x).sum()\n")
        append("    println(\"sum=\$sum\")\n")
        append("    return sum\n")
        append("}\n")
        append("```\n")
        append('\n')
        append("| col A | col B | col C |\n")
        append("| ----- | ----- | ----- |\n")
        append("| a").append(section).append(" | b").append(section).append(" | c").append(section).append(" |\n")
        append("| x").append(section).append(" | y").append(section).append(" | z").append(section).append(" |\n")
        append('\n')
        append("> A blockquote line referencing section ").append(section).append(".\n")
        append('\n')
    }
}

private fun chunkLikeLlmForRecompose(text: String, tokenSize: Int, rng: Random): List<String> {
    val out = ArrayList<String>(text.length / tokenSize + 16)
    var pos = 0
    while (pos < text.length) {
        val r = rng.nextDouble()
        val size = when {
            r < 0.6 -> rng.nextInt(1, tokenSize + 1)
            r < 0.9 -> rng.nextInt(tokenSize, tokenSize * 4 + 1)
            else -> rng.nextInt(tokenSize * 4, tokenSize * 16 + 1)
        }
        val end = (pos + size).coerceAtMost(text.length)
        out.add(text.substring(pos, end))
        pos = end
    }
    return out
}
