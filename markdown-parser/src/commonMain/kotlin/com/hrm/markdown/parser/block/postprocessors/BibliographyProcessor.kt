package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.*

/**
 * 参考文献后处理器。
 *
 * 在 AST 构建完成后：
 * 1. 扫描所有 `FootnoteDefinition`，查找 label 为 "bibliography" 的定义。
 * 2. 将其转换为 `BibliographyDefinition` 节点，解析其内容为 BibEntry 条目。
 * 3. 将文献条目存储到 Document 上，供行内 `CitationReference` 查找。
 */
internal class BibliographyProcessor : PostProcessor {
    override val priority: Int = 180

    override fun process(document: Document) {
        val bibDefs = mutableListOf<FootnoteDefinition>()
        collectBibliographyDefs(document, bibDefs)

        for (footnote in bibDefs) {
            val bibDef = BibliographyDefinition()
            bibDef.lineRange = footnote.lineRange
            bibDef.sourceRange = footnote.sourceRange

            // 解析脚注内容中的文献条目
            // 格式：key: Author, "Title", Year
            val entries = parseBibEntries(footnote)
            for (entry in entries) {
                bibDef.entries[entry.key] = entry
            }

            // 替换原脚注节点
            val parent = footnote.parent as? ContainerNode ?: continue
            parent.replaceChild(footnote, bibDef)

            // 从 footnoteDefinitions 中移除
            document.footnoteDefinitions.remove("bibliography")
        }
    }

    private fun collectBibliographyDefs(node: Node, result: MutableList<FootnoteDefinition>) {
        if (node is FootnoteDefinition && node.label.equals("bibliography", ignoreCase = true)) {
            result.add(node)
        }
        if (node is ContainerNode) {
            for (child in node.children.toList()) {
                collectBibliographyDefs(child, result)
            }
        }
    }

    private fun parseBibEntries(footnote: FootnoteDefinition): List<BibEntry> {
        val entries = mutableListOf<BibEntry>()

        // 收集脚注子节点中的文本内容
        for (child in footnote.children) {
            if (child is Paragraph) {
                val text = extractPlainText(child)
                // 逐行解析 key: content 格式
                for (line in text.lines()) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx > 0) {
                        val key = trimmed.substring(0, colonIdx).trim()
                        val content = trimmed.substring(colonIdx + 1).trim()
                        if (key.isNotEmpty() && content.isNotEmpty()) {
                            entries.add(BibEntry(key = key, content = content))
                        }
                    }
                }
            }
        }

        return entries
    }

    private fun extractPlainText(node: Node): String = when (node) {
        is Text -> node.literal
        is SoftLineBreak -> "\n"
        is HardLineBreak -> "\n"
        is ContainerNode -> node.children.joinToString("") { extractPlainText(it) }
        is LeafNode -> node.literal
    }
}
