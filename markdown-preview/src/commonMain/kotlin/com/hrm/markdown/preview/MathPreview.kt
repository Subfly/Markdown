package com.hrm.markdown.preview

import androidx.compose.runtime.Composable
import com.hrm.markdown.renderer.Markdown

internal val mathPreviewGroups = listOf(
    PreviewGroup(
        id = "inline_math",
        title = "行内公式",
        description = "行内 LaTeX 数学公式",
        items = listOf(
            PreviewItem(
                id = "inline_basic",
                title = "基础行内公式",
                content = {
                    Markdown(markdown = "质能方程 \$E = mc^2\$ 是物理学中最著名的公式之一。")
                }
            ),
            PreviewItem(
                id = "inline_multiple",
                title = "多个行内公式",
                content = {
                    Markdown(
                        markdown = "全量解析复杂度：\$O(n)\$，其中 \$n\$ 为文档总字符数。流式增量解析复杂度：\$O(k)\$，其中 \$k\$ 为尾部脏区域大小。"
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "block_math",
        title = "块级公式",
        description = "块级 LaTeX 数学公式",
        items = listOf(
            PreviewItem(
                id = "quadratic",
                title = "求根公式",
                content = {
                    Markdown(
                        markdown = """
$$
\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}
$$
                        """.trimIndent()
                    )
                }
            ),
            PreviewItem(
                id = "speedup",
                title = "加速比公式",
                content = {
                    Markdown(
                        markdown = """
$$
\text{Speedup} = \frac{T_{full}}{T_{incremental}} = \frac{O(n)}{O(k)} \approx \frac{n}{k}
$$
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "math_tag",
        title = "公式编号",
        description = "\\tag{N} 公式编号（LaTeX 原生渲染）",
        items = listOf(
            PreviewItem(
                id = "math_tag_basic",
                title = "基础公式编号",
                content = {
                    Markdown(
                        markdown = """
$$
E = mc^2 \tag{1}
$$

质能方程见公式(1)。
                        """.trimIndent()
                    )
                }
            ),
            PreviewItem(
                id = "math_tag_multiple",
                title = "多公式编号",
                content = {
                    Markdown(
                        markdown = """
$$
a^2 + b^2 = c^2 \tag{eq:pythagoras}
$$

$$
\frac{-b \pm \sqrt{b^2 - 4ac}}{2a} \tag{eq:quadratic}
$$

勾股定理见公式(eq:pythagoras)，求根公式见公式(eq:quadratic)。
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "math_in_context",
        title = "公式与文本混排",
        description = "公式嵌入在段落中",
        items = listOf(
            PreviewItem(
                id = "math_paragraph",
                title = "文本中的数学公式",
                content = {
                    Markdown(
                        markdown = """
流式增量解析的时间复杂度分析：

- 全量解析复杂度：${'$'}O(n)${'$'}，其中 ${'$'}n${'$'} 为文档总字符数
- 流式增量解析复杂度：${'$'}O(k)${'$'}，其中 ${'$'}k${'$'} 为尾部脏区域大小
- 稳定块复用率：通常 ${'$'}\frac{n - k}{n} \approx 95\%${'$'} 以上

块级公式 —— 增量解析加速比：

${'$'}${'$'}
\text{Speedup} = \frac{T_{full}}{T_{incremental}} = \frac{O(n)}{O(k)} \approx \frac{n}{k}
${'$'}${'$'}
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
)
