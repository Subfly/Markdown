package com.hrm.markdown.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.markdown.renderer.Markdown
import androidx.compose.foundation.rememberScrollState
import com.hrm.markdown.renderer.MarkdownConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val streamingPreviewGroups = listOf(
    PreviewGroup(
        id = "streaming_demo",
        title = "流式渲染演示",
        description = "模拟 LLM 逐 token 输出，实时增量解析与渲染",
        items = listOf(
            PreviewItem(
                id = "streaming_full",
                title = "完整流式渲染",
                content = { StreamingMarkdownDemo() }
            )
        )
    ),
    PreviewGroup(
        id = "streaming_issues",
        title = "Issues",
        description = "问题复现与回归验证示例",
        items = listOf(
            PreviewItem(
                id = "streaming_issue_19_list_flicker",
                title = "Issue #19 列表闪烁回归",
                markdown = issue19MarkdownString(),
                content = { Issue19StreamingDemo() }
            ),
        )
    ),
)

@Composable
private fun StreamingMarkdownDemo() {
    StringStreamingMarkdownDemo(
        markdown = streamingMarkdownString(),
        emptyHint = "点击「开始流式生成」按钮，模拟 LLM 逐 token 输出 Markdown\n\n" +
                "Markdown 组件内置流式优化：\n" +
                "• 自动节流渲染，避免高频更新导致的布局抖动\n" +
                "• 流式期间跳过 SelectionContainer，减少 intrinsic 测量\n" +
                "• 流式结束后自动恢复文本选择能力"
    )
}

@Composable
private fun Issue19StreamingDemo() {
    StringStreamingMarkdownDemo(
        markdown = issue19MarkdownString(),
        emptyHint = "该示例复现 Issue #19 的输入形态：\n" +
                "使用单个 markdownString 按字符流式输出，\n" +
                "不再依赖 `List<String>` token 序列。\n\n" +
                "期望行为：流式过程中不应短暂显示为 SetextHeading。"
    )
}

@Composable
private fun StringStreamingMarkdownDemo(
    markdown: String,
    emptyHint: String,
) {
    var text by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var streamFinished by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var autoFollow by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (!isRunning) {
                        text = ""
                        isRunning = true
                        streamFinished = false
                        scope.launch {
                            markdown.forEach { char ->
                                text += char.toString()
                                delay(8)
                            }
                            // 显式提交完整字符串，避免流式结束切换与最后几个字符落在同一帧时丢尾部。
                            text = markdown
                            isRunning = false
                            withFrameNanos { }
                            withFrameNanos { }
                            streamFinished = true
                        }
                    }
                },
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (isRunning) "生成中..."
                    else if (streamFinished) "重新生成"
                    else "开始流式生成"
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "模拟 markdownString 逐字符输出中...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (streamFinished) {
                Text(
                    text = "生成完毕",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (text.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            } else {
                Markdown(
                    markdown = text,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    config = MarkdownConfig.LlmStreaming,
                    scrollState = scrollState,
                    isStreaming = isRunning
                )
            }
        }
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        autoFollow = true

        launch {
            snapshotFlow { scrollState.isScrollInProgress to scrollState.value }
                .collect { (isScrolling, value) ->
                    if (isScrolling && value < scrollState.maxValue - 100) {
                        autoFollow = false
                    }
                }
        }

        launch {
            snapshotFlow { Triple(scrollState.value, scrollState.maxValue, autoFollow) }
                .collect { (value, maxValue, follow) ->
                    if (!follow && maxValue > 0 && value >= maxValue - 100) {
                        autoFollow = true
                    }
                }
        }

        launch {
            snapshotFlow { scrollState.maxValue to autoFollow }
                .collect { (maxValue, follow) ->
                    if (!follow) return@collect
                    withFrameNanos { }
                    if (kotlin.math.abs(scrollState.value - maxValue) > 6) {
                        scrollState.scrollTo(maxValue)
                    }
                }
        }
    }

    LaunchedEffect(streamFinished, text, markdown) {
        if (!streamFinished || text != markdown) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        scrollState.scrollTo(scrollState.maxValue)
        delay(50L)
        scrollState.animateScrollTo(scrollState.maxValue)
    }
}

private fun issue19MarkdownString(): String = """

截至2025年5月，根据我的知识，DeepSeek 是中国一家专注于通用人工智能（AGI）研究与应用的科技公司。以下是我了解的一些关键信息：

1. **公司背景**  
   - DeepSeek 由国内顶尖的AI研究团队创立，核心成员来自高校、科研机构或知名科技企业，致力于突破大模型与AGI领域的技术边界。 

2. **主营业务与产品**  
   - 专注于开发高性能、低成本的大语言模型（LLM）及多模态模型，曾推出多个开源或商用模型系列（如DeepSeek-VL、DeepSeek-Coder等），在代码生成、逻辑推理、长文本理解等领域表现突出。  
   - 提供API服务、企业级解决方案及垂直领域定制化应用（如金融、科研、教育等）。 

3. **技术特点**  
   - 强调算法与工程协同优化，在模型架构创新、训练效率提升、数据质量控制等方面有较多实践。  
   - 积极参与开源社区，部分模型在国际权威评测（如MMLU、GSM8K等）中位列前茅。 

4. **行业影响**  
   - 被视为中国AGI领域的重要参与者之一，与国内其他头部AI机构共同推动技术发展。  
   - 注重AI安全与伦理研究，探索大模型在产业落地中的可靠性方案。 

**需要说明的是**：  
- 我的信息可能无法涵盖2025年5月之后的最新动态，如需了解该公司近期进展（如新模型发布、合作伙伴、融资情况等），建议查阅最新权威报道或官方渠道。  
- 如果你有具体的技术细节、商业模式或某款产品想深入讨论，我可以结合已有知识进一步交流。 
""".trimIndent()

private fun streamingMarkdownString(): String = """
# Kotlin Multiplatform 完全指南

本文将全面介绍 **Kotlin Multiplatform**（KMP）的核心概念、架构设计、最佳实践以及在生产环境中的实际应用。无论你是刚接触 KMP 的新手，还是经验丰富的跨平台开发者，都能从中获得有价值的参考。

## 1. 什么是 Kotlin Multiplatform

Kotlin Multiplatform 是 JetBrains 推出的跨平台开发技术，允许在 **Android**、**iOS**、**Desktop**、**Web** 等平台之间共享业务逻辑代码，同时保留各平台的原生 UI 开发体验。

与其他跨平台方案（如 Flutter、React Native）不同，KMP 采用了 ***共享逻辑、原生 UI*** 的哲学：

- **共享层**：网络请求、数据库操作、业务逻辑、状态管理
- **平台层**：各平台原生 UI 框架（Jetpack Compose、SwiftUI、Compose for Desktop）
- **桥接层**：`expect/actual` 机制实现平台特定功能

### 1.1 expect/actual 机制

这是 KMP 最核心的特性之一。通过 `expect` 声明公共接口，通过 `actual` 提供平台实现：

```kotlin
expect class PlatformInfo() {
    val name: String
    val version: String
    fun getDeviceId(): String
}
```

## 2. 项目架构设计

一个良好的 KMP 项目架构是成功的关键。以下是推荐的模块化架构方案：

| 模块 | 职责 | 平台 | 依赖 |
|:---|:---|:---:|:---|
| `core-model` | 数据模型定义 | Common | 无 |
| `core-network` | 网络层封装 | Common | Ktor |
| `core-database` | 本地数据库 | Common | SQLDelight |
| `core-domain` | 业务逻辑 | Common | core-model |

## 3. 网络层实践

**Ktor** 是 KMP 生态中最成熟的网络库。以下展示如何构建一个健壮的网络层。

## 4. 总结

Kotlin Multiplatform 已经从实验性功能发展为生产就绪的跨平台解决方案。

> 提示：你可以继续扩展这个 demo，模拟更复杂的消息流。
""".trimIndent()
