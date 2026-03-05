package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.markdown.parser.ast.DiagramBlock
import com.hrm.markdown.renderer.LocalMarkdownTheme

/**
 * 图表块渲染器（Mermaid / PlantUML 等）。
 *
 * 由于在 Compose Multiplatform 中无法直接嵌入 Mermaid/PlantUML 渲染引擎，
 * 这里以美观的占位方式展示图表代码，标注图表类型，方便后续集成 WebView 或其他引擎。
 */
@Composable
internal fun DiagramBlockRenderer(
    node: DiagramBlock,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val typeName = node.diagramType.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.codeBlockCornerRadius))
            .background(Color(0xFFF0F4F8))
            .padding(theme.codeBlockPadding),
    ) {
        // 图表类型标签
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                text = "📊",
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = "$typeName Diagram",
                style = theme.bodyStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF57606A),
                ),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 图表源代码
        Text(
            text = node.literal.trimEnd('\n'),
            style = theme.codeBlockStyle.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
