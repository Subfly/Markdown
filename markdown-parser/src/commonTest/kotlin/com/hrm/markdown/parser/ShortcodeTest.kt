package com.hrm.markdown.parser

import com.hrm.markdown.parser.ast.*
import com.hrm.markdown.parser.html.HtmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * tests for custom shortcode syntax: `{% tag arg1 "arg2" key=value %}`.
 */
class ShortcodeTest {

    private fun parse(input: String): Document {
        return MarkdownParser().parse(input)
    }

    private fun renderHtml(input: String): String {
        return HtmlRenderer.renderMarkdown(input)
    }

    // ---- block-level shortcodes ----

    @Test
    fun should_parse_self_closing_block_shortcode() {
        val doc = parse("{% youtube abc123 %}")
        val shortcode = doc.children.filterIsInstance<ShortcodeBlock>().firstOrNull()
        assertTrue(shortcode != null, "should produce a ShortcodeBlock")
        assertEquals("youtube", shortcode.tagName)
        assertEquals("abc123", shortcode.args["_0"])
    }

    @Test
    fun should_parse_block_shortcode_with_key_value_args() {
        val doc = parse("""{% include file="header.html" cache=true %}""")
        val shortcode = doc.children.filterIsInstance<ShortcodeBlock>().firstOrNull()
        assertTrue(shortcode != null, "should produce a ShortcodeBlock")
        assertEquals("include", shortcode.tagName)
        assertEquals("header.html", shortcode.args["file"])
        assertEquals("true", shortcode.args["cache"])
    }

    @Test
    fun should_parse_block_shortcode_with_content() {
        val doc = parse("""
{% alert %}
This is a warning message.
{% endalert %}
        """.trimIndent())
        val shortcode = doc.children.filterIsInstance<ShortcodeBlock>().firstOrNull()
        assertTrue(shortcode != null, "should produce a ShortcodeBlock")
        assertEquals("alert", shortcode.tagName)
        // content between opening and closing tag should be parsed as children
        assertTrue(shortcode.children.isNotEmpty(), "should have children content")
    }

    @Test
    fun should_render_block_shortcode_html() {
        val html = renderHtml("{% youtube abc123 %}")
        assertTrue(html.contains("data-shortcode=\"youtube\""), "html should contain data-shortcode")
    }

    @Test
    fun should_render_block_shortcode_with_args_html() {
        val html = renderHtml("""{% include file="header.html" %}""")
        assertTrue(html.contains("data-shortcode=\"include\""), "html should contain data-shortcode")
        assertTrue(html.contains("data-args="), "html should contain data-args")
    }

    @Test
    fun should_render_block_shortcode_with_content_html() {
        val html = renderHtml("""
{% note %}
Important stuff here.
{% endnote %}
        """.trimIndent())
        assertTrue(html.contains("data-shortcode=\"note\""))
        assertTrue(html.contains("Important stuff here."))
    }

    // ---- inline shortcodes ----

    @Test
    fun should_parse_inline_shortcode() {
        val doc = parse("Text with {% icon name=star %} inside.")
        val para = doc.children.filterIsInstance<Paragraph>().firstOrNull()
        assertTrue(para != null, "should have a paragraph")
        val shortcode = para.children.filterIsInstance<ShortcodeInline>().firstOrNull()
        assertTrue(shortcode != null, "should produce a ShortcodeInline")
        assertEquals("icon", shortcode.tagName)
        assertEquals("star", shortcode.args["name"])
    }

    @Test
    fun should_render_inline_shortcode_html() {
        val html = renderHtml("Text with {% icon name=star %} inside.")
        assertTrue(html.contains("data-shortcode=\"icon\""))
        assertTrue(html.contains("<span"))
    }

    // ---- edge cases ----

    @Test
    fun should_not_parse_invalid_shortcode_missing_close() {
        val doc = parse("Text with {% incomplete tag")
        val para = doc.children.filterIsInstance<Paragraph>().firstOrNull()
        assertTrue(para != null, "should have a paragraph")
        // should not have any shortcode nodes
        val shortcodes = para.children.filterIsInstance<ShortcodeInline>()
        assertEquals(0, shortcodes.size, "should not produce ShortcodeInline for unclosed tag")
    }

    @Test
    fun should_not_parse_end_tag_as_shortcode() {
        val doc = parse("{% endfoo %}")
        // end tags should not create block shortcodes
        val shortcodes = doc.children.filterIsInstance<ShortcodeBlock>()
        assertEquals(0, shortcodes.size, "end tags should not produce ShortcodeBlock")
    }

    @Test
    fun should_parse_shortcode_with_quoted_positional_args() {
        val doc = parse("""{% tag "hello world" %}""")
        val shortcode = doc.children.filterIsInstance<ShortcodeBlock>().firstOrNull()
        assertTrue(shortcode != null)
        assertEquals("tag", shortcode.tagName)
        assertEquals("hello world", shortcode.args["_0"])
    }
}
