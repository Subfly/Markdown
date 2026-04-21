package com.hrm.markdown.benchmark

import com.hrm.markdown.parser.MarkdownParser
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * Main-thread jank benchmark.
 *
 * 验证假设："LLM 流式 append 在主线程跑会挤占帧预算"是不是真问题。
 *
 * 模型（接近真实 Compose 场景）：
 *   - "main" 线程 = 单线程 executor，按 16.67ms 周期跑"frame"任务（每帧做 ~3ms 仿渲染计算）
 *   - producer 线程 = 高频喂 token 到队列
 *   - Mode A (BASELINE):  每来一个 chunk，把 append 任务 submit 到 main executor
 *                         → append 与 frame 在同一队列竞争
 *   - Mode B (OFFLOADED): 来 chunk 后把 append 放到 background pool 跑，跑完 submit 一个轻量
 *                         "set document" 回 main → 模拟把解析切到 Dispatchers.Default 的效果
 *
 * 输出：每模式的 frame interval 分布（avg / p50 / p95 / p99 / max）和 jank count。
 *
 * Usage:
 *   ./gradlew :markdown-benchmark:mainThreadJankBenchmark \
 *       --args="--bytes 30000 --tokenSize 4 --frameMs 16 --frameWorkMs 3"
 */
private const val NS_PER_MS = 1_000_000L

fun main(args: Array<String>) {
    val cli = JankCli.parse(args)
    val docText = buildJankDoc(cli.bytes)
    val chunks = chunkJankDoc(docText, cli.tokenSize, Random(0xA17BAB1EL))

    println("Main-Thread Jank Benchmark")
    println("docBytes=${docText.length}, chunks=${chunks.size}, avgChunk=${docText.length / chunks.size}")
    println("frameTargetMs=${cli.frameMs}, frameWorkMs=${cli.frameWorkMs}, tokenIntervalMs=${cli.tokenIntervalMs}")
    println()

    println("Warmup...")
    repeat(1) {
        runScenario(chunks, cli, offload = false, silent = true)
        runScenario(chunks, cli, offload = true, silent = true)
    }

    println("=== Mode A: append on main (BASELINE) ===")
    val a = runScenario(chunks, cli, offload = false, silent = false)
    println()
    println("=== Mode B: append offloaded to background (Dispatchers.Default equivalent) ===")
    val b = runScenario(chunks, cli, offload = true, silent = false)
    println()

    println("=== Summary ===")
    println("                       Mode A (main)   Mode B (offload)   delta")
    println("avg frame interval ms  ${fmt5(a.avgMs)}           ${fmt5(b.avgMs)}              ${fmtDelta(a.avgMs, b.avgMs)}")
    println("p99 frame interval ms  ${fmt5(a.p99Ms)}           ${fmt5(b.p99Ms)}              ${fmtDelta(a.p99Ms, b.p99Ms)}")
    println("max frame interval ms  ${fmt5(a.maxMs)}           ${fmt5(b.maxMs)}              ${fmtDelta(a.maxMs, b.maxMs)}")
    println("frames > target (jank) ${a.jankCount.toString().padEnd(15)} ${b.jankCount.toString().padEnd(18)} ${a.jankCount - b.jankCount}")
    println("frames > 2x target     ${a.severeCount.toString().padEnd(15)} ${b.severeCount.toString().padEnd(18)} ${a.severeCount - b.severeCount}")
    println("total frames           ${a.totalFrames.toString().padEnd(15)} ${b.totalFrames}")
    println("total wall ms          ${fmt5(a.wallMs)}          ${fmt5(b.wallMs)}")
}

private data class JankResult(
    val avgMs: Double,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val maxMs: Double,
    val jankCount: Int,
    val severeCount: Int,
    val totalFrames: Int,
    val wallMs: Double,
)

private data class JankCli(
    val bytes: Int,
    val tokenSize: Int,
    val frameMs: Long,
    val frameWorkMs: Long,
    val tokenIntervalMs: Long,
) {
    companion object {
        fun parse(args: Array<String>): JankCli {
            var bytes = 30_000
            var tokenSize = 4
            var frameMs = 16L
            var frameWorkMs = 3L
            var tokenIntervalMs = 5L
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--bytes" -> { bytes = args[i + 1].toInt(); i++ }
                    "--tokenSize" -> { tokenSize = args[i + 1].toInt(); i++ }
                    "--frameMs" -> { frameMs = args[i + 1].toLong(); i++ }
                    "--frameWorkMs" -> { frameWorkMs = args[i + 1].toLong(); i++ }
                    "--tokenIntervalMs" -> { tokenIntervalMs = args[i + 1].toLong(); i++ }
                }
                i++
            }
            return JankCli(bytes, tokenSize, frameMs, frameWorkMs, tokenIntervalMs)
        }
    }
}

private fun runScenario(
    chunks: List<String>,
    cli: JankCli,
    offload: Boolean,
    silent: Boolean,
): JankResult {
    val parser = MarkdownParser()
    parser.beginStream()

    val mainExec = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "fake-main").apply { isDaemon = true } }
    val bgExec = Executors.newSingleThreadExecutor { r -> Thread(r, "fake-bg").apply { isDaemon = true } }

    val frameIntervalsNs = LongArray(2048)
    var frameCount = 0
    var lastFrameNs = 0L
    val docRef = AtomicReference<Any?>()
    val done = CountDownLatch(1)
    val totalChunks = chunks.size
    var consumedChunks = 0
    val chunkQueue = LinkedBlockingQueue<String>().apply { addAll(chunks) }

    val frameWorkNs = cli.frameWorkMs * NS_PER_MS

    val wallStart = System.nanoTime()

    val frameTask = object : Runnable {
        override fun run() {
            val now = System.nanoTime()
            if (lastFrameNs != 0L) {
                val delta = now - lastFrameNs
                if (frameCount < frameIntervalsNs.size) {
                    frameIntervalsNs[frameCount] = delta
                    frameCount++
                }
            }
            lastFrameNs = now
            // Simulate Compose frame work (recomposition + measure + draw)
            val deadline = System.nanoTime() + frameWorkNs
            var x = 0L
            while (System.nanoTime() < deadline) {
                x += System.nanoTime() xor 0x9E3779B97F4A7C15UL.toLong()
            }
            if (x == Long.MIN_VALUE) println(x) // prevent dead code elimination
        }
    }

    val frameFuture = mainExec.scheduleAtFixedRate(frameTask, 0, cli.frameMs, TimeUnit.MILLISECONDS)

    // Producer: 把 chunk 在 cli.tokenIntervalMs 间隔下推送
    val producer = Thread({
        while (true) {
            val chunk = chunkQueue.poll() ?: break
            if (offload) {
                bgExec.submit {
                    val newDoc = parser.append(chunk)
                    mainExec.submit {
                        docRef.set(newDoc)
                        consumedChunks++
                        if (consumedChunks == totalChunks) done.countDown()
                    }
                }
            } else {
                mainExec.submit {
                    val newDoc = parser.append(chunk)
                    docRef.set(newDoc)
                    consumedChunks++
                    if (consumedChunks == totalChunks) done.countDown()
                }
            }
            try {
                Thread.sleep(cli.tokenIntervalMs)
            } catch (_: InterruptedException) {
                return@Thread
            }
        }
    }, "producer").apply { isDaemon = true }
    producer.start()

    if (!done.await(20, TimeUnit.SECONDS)) {
        if (!silent) println("  TIMEOUT, partial result (consumed=$consumedChunks/$totalChunks)")
    }
    val wallNs = System.nanoTime() - wallStart

    frameFuture.cancel(false)
    mainExec.shutdownNow()
    bgExec.shutdownNow()
    mainExec.awaitTermination(2, TimeUnit.SECONDS)
    bgExec.awaitTermination(2, TimeUnit.SECONDS)
    producer.interrupt()
    producer.join(1000)
    parser.endStream()

    val sorted = frameIntervalsNs.copyOf(frameCount).sortedArray()
    fun pct(p: Double): Long = if (sorted.isEmpty()) 0 else sorted[((sorted.size - 1) * p).toInt()]
    val avg = if (sorted.isEmpty()) 0.0 else sorted.average()
    val targetNs = cli.frameMs * NS_PER_MS
    val jank = sorted.count { it > targetNs * 1.25 } // 25% over budget = visible stutter
    val severe = sorted.count { it > targetNs * 2 }

    val result = JankResult(
        avgMs = avg / NS_PER_MS,
        p50Ms = pct(0.50).toDouble() / NS_PER_MS,
        p95Ms = pct(0.95).toDouble() / NS_PER_MS,
        p99Ms = pct(0.99).toDouble() / NS_PER_MS,
        maxMs = (sorted.lastOrNull() ?: 0L).toDouble() / NS_PER_MS,
        jankCount = jank,
        severeCount = severe,
        totalFrames = frameCount,
        wallMs = wallNs.toDouble() / NS_PER_MS,
    )

    if (!silent) {
        println("  frames=${result.totalFrames}, wallMs=${fmt5(result.wallMs)}")
        println("  frame interval ms: avg=${fmt5(result.avgMs)} p50=${fmt5(result.p50Ms)} p95=${fmt5(result.p95Ms)} p99=${fmt5(result.p99Ms)} max=${fmt5(result.maxMs)}")
        println("  jank (>${(cli.frameMs * 1.25).toInt()}ms)=${result.jankCount}, severe (>${cli.frameMs * 2}ms)=${result.severeCount}")
    }
    return result
}

private fun fmt5(v: Double): String = "%7.3f".format(v)
private fun fmtDelta(a: Double, b: Double): String {
    val diff = b - a
    val pct = if (a == 0.0) 0.0 else diff / a * 100.0
    return "%+7.3f (%+.1f%%)".format(diff, pct)
}

private fun buildJankDoc(targetBytes: Int): String {
    val sb = StringBuilder(targetBytes + 4096)
    var i = 0
    val codeFence = "```kotlin\nfun fib(n: Int): Int = if (n < 2) n else fib(n - 1) + fib(n - 2)\nval xs = (0..20).map { fib(it) }\n```\n\n"
    val table = "| col a | col b | col c |\n|------|------|------|\n| 1 | 2 | 3 |\n| 4 | 5 | 6 |\n\n"
    while (sb.length < targetBytes) {
        sb.append("# Section ").append(i).append("\n\n")
        sb.append("Paragraph ").append(i).append(" with **bold**, *italic*, `code`, and a [link](https://example.com/").append(i).append("). Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n\n")
        sb.append("- item one ").append(i).append("\n- item two ").append(i).append("\n- item three ").append(i).append("\n\n")
        if (i % 3 == 0) sb.append(codeFence)
        if (i % 5 == 0) sb.append(table)
        if (i % 4 == 0) sb.append("> blockquote ").append(i).append(" with some inline `code` and **emphasis**.\n\n")
        i++
    }
    return sb.toString()
}

private fun chunkJankDoc(text: String, tokenSize: Int, rng: Random): List<String> {
    val out = ArrayList<String>(text.length / tokenSize + 1)
    var pos = 0
    while (pos < text.length) {
        val size = (tokenSize - 1 + rng.nextInt(3)).coerceAtLeast(1)
        val end = (pos + size).coerceAtMost(text.length)
        out.add(text.substring(pos, end))
        pos = end
    }
    return out
}
