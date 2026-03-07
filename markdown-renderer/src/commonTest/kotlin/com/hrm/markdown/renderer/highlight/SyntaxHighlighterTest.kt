package com.hrm.markdown.renderer.highlight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyntaxHighlighterTest {

    private val colorScheme = SyntaxColorScheme.GitHubLight

    @Test
    fun should_highlight_kotlin_keywords() {
        val code = "fun main() { val x = 42 }"
        val result = SyntaxHighlighter.highlight(code, "kotlin", colorScheme)
        assertEquals(code, result.text)
        // Should have annotations for keywords "fun", "val" and number "42"
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles for syntax highlighting")
    }

    @Test
    fun should_highlight_kotlin_strings() {
        val code = """val s = "hello world""""
        val result = SyntaxHighlighter.highlight(code, "kotlin", colorScheme)
        assertTrue(result.spanStyles.any { code.substring(it.start, it.end) == "\"hello world\"" })
    }

    @Test
    fun should_highlight_kotlin_comments() {
        val code = "// this is a comment\nval x = 1"
        val result = SyntaxHighlighter.highlight(code, "kotlin", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_java() {
        val code = "public class Main { public static void main(String[] args) {} }"
        val result = SyntaxHighlighter.highlight(code, "java", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_python() {
        val code = "def hello():\n    print(\"Hello\")\n    x = 42"
        val result = SyntaxHighlighter.highlight(code, "python", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_javascript() {
        val code = "const x = 'hello';\nfunction test() { return true; }"
        val result = SyntaxHighlighter.highlight(code, "javascript", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_json() {
        val code = """{"key": "value", "num": 42, "bool": true}"""
        val result = SyntaxHighlighter.highlight(code, "json", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_sql() {
        val code = "SELECT * FROM users WHERE id = 1 AND name = 'test'"
        val result = SyntaxHighlighter.highlight(code, "sql", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_html() {
        val code = """<div class="test">Hello</div>"""
        val result = SyntaxHighlighter.highlight(code, "html", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_yaml() {
        val code = "key: value\nnum: 42\nbool: true"
        val result = SyntaxHighlighter.highlight(code, "yaml", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_return_plain_text_for_unknown_language() {
        val code = "some random text"
        val result = SyntaxHighlighter.highlight(code, "unknown_lang", colorScheme)
        assertEquals(code, result.text)
        assertTrue(result.spanStyles.isEmpty(), "Unknown language should have no highlighting")
    }

    @Test
    fun should_return_plain_text_for_empty_language() {
        val code = "some text"
        val result = SyntaxHighlighter.highlight(code, "", colorScheme)
        assertEquals(code, result.text)
    }

    @Test
    fun should_handle_empty_code() {
        val result = SyntaxHighlighter.highlight("", "kotlin", colorScheme)
        assertEquals("", result.text)
    }

    @Test
    fun should_highlight_shell_comments() {
        val code = "# comment\necho 'hello'\nexport PATH=/usr/bin"
        val result = SyntaxHighlighter.highlight(code, "bash", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_css() {
        val code = ".class { color: #ff0000; font-size: 16px; }"
        val result = SyntaxHighlighter.highlight(code, "css", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_handle_language_aliases() {
        val code = "fun test() {}"
        val kt = SyntaxHighlighter.highlight(code, "kt", colorScheme)
        val kotlin = SyntaxHighlighter.highlight(code, "kotlin", colorScheme)
        assertEquals(kt.spanStyles.size, kotlin.spanStyles.size)
    }

    @Test
    fun should_highlight_go() {
        val code = "func main() {\n    fmt.Println(\"hello\")\n    x := 42\n}"
        val result = SyntaxHighlighter.highlight(code, "go", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_rust() {
        val code = "fn main() {\n    let x = 42;\n    println!(\"hello\");\n}"
        val result = SyntaxHighlighter.highlight(code, "rust", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_swift() {
        val code = "func hello() -> String {\n    let x = 42\n    return \"hello\"\n}"
        val result = SyntaxHighlighter.highlight(code, "swift", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_cpp() {
        val code = "#include <iostream>\nint main() {\n    std::cout << \"hello\";\n    return 0;\n}"
        val result = SyntaxHighlighter.highlight(code, "cpp", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun should_highlight_toml() {
        val code = "[package]\nname = \"test\"\nversion = \"1.0\"\n# comment"
        val result = SyntaxHighlighter.highlight(code, "toml", colorScheme)
        assertTrue(result.spanStyles.isNotEmpty())
    }
}
