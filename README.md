# Kotlin Multiplatform Markdown Rendering Library

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.1-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Android API](https://img.shields.io/badge/Android%20API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.huarangmeng/markdown-parser.svg)](https://central.sonatype.com/search?q=io.github.huarangmeng.markdown)

A high-performance Markdown parsing and rendering library built with Kotlin Multiplatform (KMP). It delivers consistent rendering across Android, iOS, Desktop (JVM), and Web (Wasm/JS) platforms using Compose Multiplatform.

[中文版本](./README_zh.md)

## 🌟 Key Features

- **High-Performance Parsing**: AST-based recursive descent parser with incremental update support.
- **Multi-platform Consistency**: Consistent rendering on Android, iOS, Desktop (JVM), and Web (Wasm/JS) via Compose Multiplatform.
- **Comprehensive Syntax Coverage**: 372/372 Markdown features supported (100% coverage), CommonMark Spec 0.31.2 fully compliant (652/652), plus GFM and popular extensions.
- **Built-in Image Loading**: Integrated Coil3 + Ktor3 for out-of-the-box network image loading with size specification and adaptive width. Custom image renderers are also supported.
- **Streaming Rendering**: First-class support for LLM token-by-token output. Incremental parsing and throttled rendering (5fps) eliminate flicker during streaming.
- **Customizable Theming**: Full theme system with 30+ configurable properties. Built-in light/dark themes (GitHub style) with automatic system theme detection.
- **LaTeX Math Support**: Inline (`$...$`) and block (`$$...$$`) math formulas via integrated LaTeX rendering engine.
- **Incremental Parsing**: Edit-aware parser that only re-parses affected regions for real-time editing scenarios.
- **Pagination Support**: Progressive rendering for ultra-long documents (500+ blocks) with automatic load-more on scroll.

## 📐 Supported Markdown Features (229+)

<details>
<summary><b>Block Elements</b> — headings, paragraphs, code blocks, lists, tables, and more</summary>

- **Headings**: ATX headings (`# ~ ######`), Setext headings (`===` / `---`), custom heading IDs (`{#id}`)
- **Paragraphs**: Multi-line merging, blank line separation, lazy continuation
- **Code Blocks**: Fenced (`` ``` `` / `~~~`) with language info string, indented (4 spaces/tab)
- **Block Quotes**: Nested quotes, lazy continuation, inner block elements
- **Lists**: Unordered (`-`, `*`, `+`), ordered (`1.`, `1)`), task lists (`- [ ]` / `- [x]`), nested lists, tight/loose distinction
- **Tables (GFM)**: Column alignment (`:---`, `:---:`, `---:`), inline elements in cells, escaped pipes
- **Thematic Breaks**: `---`, `***`, `___`
- **HTML Blocks**: All 7 CommonMark types
- **Link Reference Definitions**: Full support with title variants
</details>

<details>
<summary><b>Inline Elements</b> — emphasis, links, images, code, and more</summary>

- **Emphasis**: Bold (`**`/`__`), italic (`*`/`_`), bold-italic (`***`/`___`), nested emphasis
- **Strikethrough (GFM)**: `~~text~~`, `~text~`
- **Inline Code**: Single/multi backtick, space stripping, no inner parsing
- **Links**: Inline links, reference links (full/collapsed/shortcut), autolinks (URL/email/GFM bare URL)
- **Images**: Inline, reference, nested in links
- **Inline HTML**: Tags, comments, CDATA, processing instructions
- **Escapes & Entities**: Backslash escapes, named/numeric HTML entities
- **Hard/Soft Line Breaks**: Trailing spaces, backslash
</details>

<details>
<summary><b>Extensions</b> — math, footnotes, admonitions, and more</summary>

- **Math**: Inline `$...$`, block `$$...$$`
- **Footnotes**: `[^label]` references, `[^label]: content` definitions, multi-line, block elements in footnotes
- **Admonitions**: `> [!NOTE]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!WARNING]`, `> [!CAUTION]`
- **Highlight**: `==text==`
- **Superscript / Subscript**: `^text^`, `~text~`, `<sup>`, `<sub>`
- **Insert Text**: `++text++`
- **Emoji**: `:emoji_name:` shortcodes, Unicode emoji
- **Definition Lists**: Term + `: definition` format
- **Front Matter**: YAML (`---`) and TOML (`+++`)
</details>

## 🛠️ Usage

In a Compose Multiplatform project, use the `Markdown` composable directly:

```kotlin
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownTheme

@Composable
fun MyScreen() {
    Markdown(
        markdown = """
            # Hello World
            
            This is a paragraph with **bold** and *italic* text.
            
            - Item 1
            - Item 2
            
            ```kotlin
            fun hello() = println("Hello")
            ```
        """.trimIndent(),
        modifier = Modifier.fillMaxSize(),
        theme = MarkdownTheme.auto(), // Automatically follows system light/dark mode
    )
}
```

### Streaming Rendering (LLM Integration)

For LLM token-by-token output scenarios, use the `isStreaming` parameter to enable incremental parsing and throttled rendering:

```kotlin
var text by remember { mutableStateOf("") }
var isStreaming by remember { mutableStateOf(true) }

LaunchedEffect(Unit) {
    tokens.collect { token ->
        text += token
    }
    isStreaming = false
}

Markdown(
    markdown = text,
    isStreaming = isStreaming,
)
```

### Customizing Theme

```kotlin
// Use built-in themes
Markdown(markdown = text, theme = MarkdownTheme.light())  // GitHub Light
Markdown(markdown = text, theme = MarkdownTheme.dark())   // GitHub Dark
Markdown(markdown = text, theme = MarkdownTheme.auto())   // Follow system

// Or fully customize
Markdown(
    markdown = markdownText,
    theme = MarkdownTheme(
        headingStyles = listOf(
            TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            // h2 ~ h6 ...
        ),
        bodyStyle = TextStyle(fontSize = 16.sp),
        codeBlockBackground = Color(0xFFF5F5F5),
        // 30+ configurable properties...
    ),
    onLinkClick = { url -> /* handle link click */ }
)
```

### Image Loading

The library includes built-in image loading powered by Coil3. Images in Markdown are automatically fetched and rendered:

```markdown
![Alt text](https://example.com/image.png)
![With size](https://example.com/image.png =200x100)
```

To customize image rendering (e.g., add loading placeholders, error states), use the `imageContent` parameter:

```kotlin
Markdown(
    markdown = markdownText,
    imageContent = { data, modifier ->
        // Access data.url, data.altText, data.width, data.height, etc.
        AsyncImage(
            model = data.url,
            contentDescription = data.altText,
            modifier = modifier,
        )
    },
)
```

## 📦 Installation

Add dependencies in `gradle/libs.versions.toml`:

```toml
[versions]
markdown = "1.0.0"

[libraries]
markdown-parser = { module = "io.github.huarangmeng:markdown-parser", version.ref = "markdown" }
markdown-renderer = { module = "io.github.huarangmeng:markdown-renderer", version.ref = "markdown" }
```

Reference in your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.markdown.parser)
    implementation(libs.markdown.renderer)
}
```

> `markdown-renderer` includes Coil3 + Ktor3 for image loading, which are automatically included as transitive dependencies.

## 🏗️ Project Structure

- `:markdown-parser` — Core parsing engine. Converts Markdown strings into AST (Abstract Syntax Tree).
- `:markdown-renderer` — Rendering engine. Maps AST nodes to Compose UI components.
- `:markdown-preview` — Preview/showcase module. Provides interactive demonstrations for all supported Markdown features with a categorized navigation UI.
- `:composeApp` — Cross-platform Demo application (Android/iOS/Desktop/Web).
- `:androidApp` — Android-specific Demo application.

## 🚀 Quick Start

### Running the Demo App

- **Android**: `./gradlew :composeApp:assembleDebug`
- **Desktop**: `./gradlew :composeApp:run`
- **Web (Wasm)**: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- **Web (JS)**: `./gradlew :composeApp:jsBrowserDevelopmentRun`
- **iOS**: Open the `iosApp` directory in Xcode and run.

### Running Tests

```bash
# Parser module tests
./gradlew :markdown-parser:jvmTest

# Renderer module tests
./gradlew :markdown-renderer:jvmTest

# All tests
./gradlew jvmTest
```

## 📊 Roadmap & Coverage

For a detailed feature support list, see: [PARSER_COVERAGE_ANALYSIS.md](./markdown-parser/PARSER_COVERAGE_ANALYSIS.md)

| Category | Coverage |
|----------|----------|
| Headings | 100% |
| Paragraphs | 100% |
| Code Blocks | 100% |
| Block Quotes | 100% |
| Lists | 100% |
| Tables (GFM) | 100% |
| Emphasis | 100% |
| Links | 100% |
| Inline Extensions | 100% |
| Incremental Parsing | 100% |
| Linting / WCAG | 100% |
| Shortcodes | 100% |
| **Overall** | **100%** |

**CommonMark Spec 0.31.2: 652/652 (100%)**

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2026 huarangmeng

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
