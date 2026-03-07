package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.IndentedCodeBlock
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.highlight.SyntaxColorScheme
import com.hrm.markdown.renderer.highlight.SyntaxHighlighter

/**
 * 围栏代码块渲染器 (``` 或 ~~~)
 *
 * ## Text + AnnotatedString 方案
 *
 * 使用 [BasicText] + [AnnotatedString] 渲染代码文本，
 * 与项目中其他渲染器（ParagraphRenderer、HeadingRenderer）保持一致的风格。
 * 根据 [FencedCodeBlock.language] 进行语法高亮着色。
 *
 * ### 架构
 *
 * ```
 * ┌─ Box (fillMaxWidth, 圆角背景, clipToBounds) ───────────┐
 * │  ┌─ BasicText (softWrap=false, horizontalScroll) ─────┐ │
 * │  │  AnnotatedString 渲染代码文本（语法高亮着色）       │ │
 * │  │  不自动换行，超出内容通过水平滚动查看               │ │
 * │  │  heightIn(min = stableMinHeight) 保证高度只增不减   │ │
 * │  └────────────────────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────┘
 * ```
 *
 * ### 特性
 *
 * - **语法高亮**：根据代码块的 language（info string）自动对代码着色。
 * - **外层宽度固定**：Box 始终 fillMaxWidth，不受代码行宽度变化影响。
 * - **水平滚动**：长代码行通过 horizontalScroll 左右滚动查看。
 * - **高度只增不减**：流式追加内容时高度只会增加不会缩小，避免高度抖动。
 * - **原生选中复制**：BasicText 天然支持 SelectionContainer 的文本选择。
 */
@Composable
internal fun FencedCodeBlockRenderer(
    node: FencedCodeBlock,
    modifier: Modifier = Modifier,
) {
    CodeBlockText(
        text = node.literal.ifEmpty { " " },
        language = node.language,
        modifier = modifier,
    )
}

/**
 * 缩进代码块渲染器
 */
@Composable
internal fun IndentedCodeBlockRenderer(
    node: IndentedCodeBlock,
    modifier: Modifier = Modifier,
) {
    CodeBlockText(
        text = node.literal.ifEmpty { " " },
        language = "",
        modifier = modifier,
    )
}

/**
 * 代码块的 Text + AnnotatedString 渲染实现。
 *
 * @param text 代码文本内容（来自 AST 节点的 literal）
 * @param language 语言标识，用于语法高亮
 */
@Composable
private fun CodeBlockText(
    text: String,
    language: String,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val density = LocalDensity.current
    val colorScheme = theme.syntaxColorScheme

    // 构建 AnnotatedString（带语法高亮）
    val annotatedString = remember(text, language, colorScheme) {
        if (language.isNotEmpty()) {
            SyntaxHighlighter.highlight(text, language, colorScheme)
        } else {
            AnnotatedString(text)
        }
    }

    // 高度只增不减：记录历史最小高度（dp），避免流式场景下因末尾换行导致的高度波动
    var stableMinHeight by remember { mutableStateOf(0.dp) }

    // 水平滚动状态
    val horizontalScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.codeBlockCornerRadius))
            .clipToBounds()
            .background(theme.codeBlockBackground)
            // 高度只增不减：设置最小高度为历史最大值
            .heightIn(min = stableMinHeight),
    ) {
        BasicText(
            text = annotatedString,
            style = theme.codeBlockStyle,
            softWrap = false, // 不自动换行，通过水平滚动查看长行
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .padding(theme.codeBlockPadding)
                .onSizeChanged { size ->
                    // 将实际渲染高度（含 padding）转换为 dp，更新最小高度
                    val currentHeightDp: Dp = with(density) {
                        (size.height + theme.codeBlockPadding.roundToPx() * 2).toDp()
                    }
                    if (currentHeightDp > stableMinHeight) {
                        stableMinHeight = currentHeightDp
                    }
                },
        )
    }
}