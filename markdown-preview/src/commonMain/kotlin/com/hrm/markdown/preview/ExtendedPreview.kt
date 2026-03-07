package com.hrm.markdown.preview

import com.hrm.markdown.renderer.Markdown

internal val extendedPreviewGroups = listOf(
    PreviewGroup(
        id = "footnotes",
        title = "脚注",
        description = "脚注引用与定义",
        items = listOf(
            PreviewItem(
                id = "basic_footnote",
                title = "基础脚注",
                content = {
                    Markdown(
                        markdown = """
这是一段带有脚注的文本[^1]。

[^1]: 这是脚注的内容。
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "definition_list",
        title = "定义列表",
        description = "术语与定义",
        items = listOf(
            PreviewItem(
                id = "basic_dl",
                title = "基础定义列表",
                content = {
                    Markdown(
                        markdown = """
Kotlin
: 一种现代的编程语言，运行在 JVM 上。

Compose Multiplatform
: JetBrains 的跨平台 UI 框架。

  支持 Android、iOS、Desktop、Web 多平台开发。
                        """.trimIndent()
                    )
                }
            ),
            PreviewItem(
                id = "rich_dl",
                title = "包含代码和列表的定义",
                content = {
                    Markdown(
                        markdown = """
Compose Multiplatform
: JetBrains 的跨平台 UI 框架。

  ```kotlin
  @Composable
  fun Greeting(name: String) {
      Text("Hello, ${'$'}name!")
  }
  ```

  - 声明式 UI
  - 跨平台复用
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "emoji",
        title = "Emoji",
        description = "Emoji 短代码支持、Unicode 映射",
        items = listOf(
            PreviewItem(
                id = "emoji_basic",
                title = "Emoji 短代码（Unicode 映射）",
                content = {
                    Markdown(markdown = "支持 Emoji 短代码自动映射为 Unicode：:smile: :heart: :rocket: :fire: :thumbsup: :tada:")
                }
            ),
            PreviewItem(
                id = "emoji_multiple",
                title = "多种 Emoji",
                content = {
                    Markdown(markdown = "表情：:grin: :wink: :sunglasses: :thinking:\n\n动物：:cat: :dog: :penguin: :turtle:\n\n物品：:coffee: :pizza: :gift: :trophy:")
                }
            ),
            PreviewItem(
                id = "emoji_unknown",
                title = "未知短代码",
                content = {
                    Markdown(markdown = "已知 :smile: vs 未知 :unknown_code: — 未知的保留原始文本。")
                }
            ),
        )
    ),
    PreviewGroup(
        id = "keyboard",
        title = "键盘按键",
        description = "<kbd> 标签",
        items = listOf(
            PreviewItem(
                id = "kbd_basic",
                title = "键盘快捷键",
                content = {
                    Markdown(markdown = "使用 <kbd>Ctrl</kbd>+<kbd>C</kbd> 复制，<kbd>Ctrl</kbd>+<kbd>V</kbd> 粘贴。")
                }
            ),
        )
    ),
    PreviewGroup(
        id = "abbreviation",
        title = "缩写",
        description = "缩写标签与悬浮提示",
        items = listOf(
            PreviewItem(
                id = "abbr_basic",
                title = "缩写示例",
                content = {
                    Markdown(
                        markdown = """
*[HTML]: Hyper Text Markup Language
*[CSS]: Cascading Style Sheets

HTML 和 CSS 是 Web 开发的基础技术。HTML 定义页面结构，CSS 负责样式。
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "custom_container",
        title = "自定义容器",
        description = "::: 围栏容器与嵌套",
        items = listOf(
            PreviewItem(
                id = "container_note",
                title = "Note 容器",
                content = {
                    Markdown(
                        markdown = """
::: note "提示信息"
这是一个使用 `:::` 围栏语法创建的自定义容器。

支持**完整的 Markdown 语法**，包括列表：
- 项目一
- 项目二
:::
                        """.trimIndent()
                    )
                }
            ),
            PreviewItem(
                id = "container_warning",
                title = "Warning 容器",
                content = {
                    Markdown(
                        markdown = """
::: warning
未指定标题时，容器类型名作为默认标题显示。
:::
                        """.trimIndent()
                    )
                }
            ),
            PreviewItem(
                id = "container_nested",
                title = "嵌套容器",
                content = {
                    Markdown(
                        markdown = """
:::: card
::: note
容器支持**嵌套**，外层使用更多冒号（`::::`）。
:::
::::
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "thematic_break",
        title = "分隔线",
        description = "水平分隔线",
        items = listOf(
            PreviewItem(
                id = "hr",
                title = "分隔线",
                content = {
                    Markdown(
                        markdown = """
上面的内容

---

下面的内容
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "columns_layout",
        title = "多列布局",
        description = "基于自定义容器的多列布局",
        items = listOf(
            PreviewItem(
                id = "two_columns",
                title = "两列布局",
                content = {
                    Markdown(
                        markdown = """
:::columns
:::column "width=50%"
### 左列

这是左列的内容，包含 **粗体** 和 *斜体*。

- 列表项 1
- 列表项 2
:::column "width=50%"
### 右列

这是右列的内容。

> 引用块也可以放在列中。
:::
                        """.trimIndent()
                    )
                }
            ),
            PreviewItem(
                id = "three_columns",
                title = "三列布局",
                content = {
                    Markdown(
                        markdown = """
:::columns
:::column
**第一列**

简短内容。
:::column
**第二列**

中间的列。
:::column
**第三列**

最后一列。
:::
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "toc_advanced",
        title = "目录高级配置",
        description = "TOC 深度、排除、排序",
        items = listOf(
            PreviewItem(
                id = "toc_depth",
                title = "TOC 深度限制",
                content = {
                    Markdown(
                        markdown = """
[TOC]
:depth=2-3

# 一级标题

## 二级标题

### 三级标题

#### 四级标题（不会出现在目录中）

## 另一个二级标题
                        """.trimIndent()
                    )
                }
            ),
            PreviewItem(
                id = "toc_full_config",
                title = "TOC 完整配置",
                content = {
                    Markdown(
                        markdown = """
[TOC]
:depth=1-3
:order=asc

# 概述

## 安装

### 环境要求

## 使用指南

### 快速开始

# 参考
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "styled_text",
        title = "自定义行内样式",
        description = "[文本]{.class style=\"...\"} 属性语法",
        items = listOf(
            PreviewItem(
                id = "styled_class",
                title = "CSS 类样式",
                content = {
                    Markdown(markdown = "这是 [红色文本]{.red} 和 [粗体文本]{.bold} 以及 [红色粗体]{.red .bold}。")
                }
            ),
            PreviewItem(
                id = "styled_inline",
                title = "行内 style 属性",
                content = {
                    Markdown(markdown = "自定义背景：[高亮文本]{style=\"background:yellow\"} 和 [蓝色文本]{style=\"color:blue\"}。")
                }
            ),
            PreviewItem(
                id = "styled_nested",
                title = "嵌套格式",
                content = {
                    Markdown(markdown = "组合：[**粗体** *斜体* 混合]{.red .underline}")
                }
            ),
        )
    ),
    PreviewGroup(
        id = "page_break",
        title = "分页符",
        description = "PDF 导出/打印用分页标记",
        items = listOf(
            PreviewItem(
                id = "pagebreak_basic",
                title = "基础分页符",
                content = {
                    Markdown(
                        markdown = """
# 第一页内容

这是第一页的内容。

***pagebreak***

# 第二页内容

这是第二页的内容。
                        """.trimIndent()
                    )
                }
            ),
        )
    ),
)
