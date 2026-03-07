package com.hrm.markdown.renderer.highlight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * 基于正则的代码语法高亮引擎。
 *
 * 根据语言类型对代码文本中的关键字、字符串、注释、数字、注解等进行着色，
 * 生成带样式的 [AnnotatedString]。
 *
 * 支持的语言：Kotlin, Java, Python, JavaScript/TypeScript, Swift, Go, Rust,
 * C/C++, HTML, CSS, SQL, Shell/Bash, JSON, XML, YAML, TOML, Ruby, PHP 等。
 */
object SyntaxHighlighter {

    /**
     * 对代码文本进行语法高亮。
     *
     * @param code 代码文本
     * @param language 语言标识（如 "kotlin", "java", "python" 等）
     * @param colorScheme 语法高亮配色方案
     * @return 带颜色标注的 AnnotatedString
     */
    fun highlight(code: String, language: String, colorScheme: SyntaxColorScheme): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString(code)
        val lang = language.lowercase().trim()
        val rules = languageRules[lang] ?: return AnnotatedString(code)
        return applyRules(code, rules, colorScheme)
    }

    private fun applyRules(
        code: String,
        rules: List<HighlightRule>,
        colorScheme: SyntaxColorScheme,
    ): AnnotatedString {
        // 收集所有匹配，按起始位置排序，不重叠
        val spans = mutableListOf<HighlightSpan>()

        for (rule in rules) {
            val matches = rule.pattern.findAll(code)
            for (match in matches) {
                val start: Int
                val end: Int
                if (rule.groupIndex > 0 && rule.groupIndex < match.groupValues.size) {
                    val groupValue = match.groupValues[rule.groupIndex]
                    if (groupValue.isNotEmpty()) {
                        // MatchGroup.range is not available in common; compute offset manually
                        val offset = match.value.indexOf(groupValue)
                        start = match.range.first + offset
                        end = start + groupValue.length
                    } else {
                        start = match.range.first
                        end = match.range.last + 1
                    }
                } else {
                    start = match.range.first
                    end = match.range.last + 1
                }
                spans.add(HighlightSpan(start, end, rule.tokenType))
            }
        }

        // 按起始位置排序，相同位置按长度降序（优先长匹配）
        spans.sortWith(compareBy<HighlightSpan> { it.start }.thenByDescending { it.end - it.start })

        // 去除重叠：后出现的 span 如果与已有 span 重叠则丢弃
        val nonOverlapping = mutableListOf<HighlightSpan>()
        var lastEnd = 0
        for (span in spans) {
            if (span.start >= lastEnd) {
                nonOverlapping.add(span)
                lastEnd = span.end
            }
        }

        val builder = AnnotatedString.Builder(code)
        for (span in nonOverlapping) {
            val style = colorScheme.styleFor(span.tokenType)
            if (style != null) {
                builder.addStyle(style, span.start, span.end)
            }
        }
        return builder.toAnnotatedString()
    }

    private data class HighlightSpan(val start: Int, val end: Int, val tokenType: TokenType)

    // ──────── 语言规则定义 ────────

    private val languageRules: Map<String, List<HighlightRule>> by lazy {
        buildMap {
            // C-family languages
            val cLikeKeywords = "auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|inline|int|long|register|return|short|signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while"
            val cRules = buildRules(cLikeKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasPreprocessor = true)
            put("c", cRules)
            put("h", cRules)

            val cppKeywords = "$cLikeKeywords|bool|catch|class|constexpr|decltype|delete|dynamic_cast|explicit|export|false|friend|mutable|namespace|new|noexcept|nullptr|operator|override|private|protected|public|reinterpret_cast|static_assert|static_cast|template|this|throw|true|try|typeid|typename|using|virtual"
            val cppRules = buildRules(cppKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasPreprocessor = true)
            put("cpp", cppRules)
            put("c++", cppRules)
            put("cc", cppRules)
            put("cxx", cppRules)
            put("hpp", cppRules)

            // Java
            val javaKeywords = "abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null|var|record|sealed|permits|yield"
            put("java", buildRules(javaKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasAnnotation = true))

            // Kotlin
            val kotlinKeywords = "abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|delegate|do|dynamic|else|enum|expect|external|false|field|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|it|lateinit|noinline|null|object|open|operator|out|override|package|param|private|property|protected|public|receiver|reified|return|sealed|set|setparam|super|suspend|tailrec|this|throw|true|try|typealias|typeof|val|value|var|vararg|when|where|while|yield"
            val kotlinRules = buildRules(kotlinKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasAnnotation = true, stringTemplate = true)
            put("kotlin", kotlinRules)
            put("kt", kotlinRules)
            put("kts", kotlinRules)

            // JavaScript/TypeScript
            val jsKeywords = "async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|false|finally|for|from|function|get|if|import|in|instanceof|let|new|null|of|return|set|static|super|switch|this|throw|true|try|typeof|undefined|var|void|while|with|yield"
            val jsRules = buildRules(jsKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasTemplateLiteral = true)
            put("javascript", jsRules)
            put("js", jsRules)
            put("jsx", jsRules)

            val tsKeywords = "$jsKeywords|any|as|boolean|declare|enum|implements|interface|keyof|module|namespace|never|number|object|package|private|protected|public|readonly|require|string|symbol|type|abstract|infer|unknown"
            val tsRules = buildRules(tsKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasTemplateLiteral = true)
            put("typescript", tsRules)
            put("ts", tsRules)
            put("tsx", tsRules)

            // Python
            val pythonKeywords = "False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield"
            val pythonRules = buildRules(pythonKeywords, lineComment = "#", hasTripleQuote = true, hasDecorator = true)
            put("python", pythonRules)
            put("py", pythonRules)

            // Swift
            val swiftKeywords = "actor|any|as|associatedtype|async|await|break|case|catch|class|continue|convenience|default|defer|deinit|didSet|do|dynamic|else|enum|extension|fallthrough|false|fileprivate|final|for|func|get|guard|if|import|in|indirect|infix|init|inout|internal|is|isolated|lazy|let|mutating|nil|nonisolated|nonmutating|open|operator|optional|override|postfix|precedencegroup|prefix|private|protocol|public|repeat|required|rethrows|return|self|Self|set|some|static|struct|subscript|super|switch|throw|throws|true|try|typealias|unowned|var|weak|where|while|willSet"
            put("swift", buildRules(swiftKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasAnnotation = true))

            // Go
            val goKeywords = "break|case|chan|const|continue|default|defer|else|fallthrough|false|for|func|go|goto|if|import|interface|iota|map|nil|package|range|return|select|struct|switch|true|type|var"
            val goBuiltins = "append|cap|close|complex|copy|delete|imag|len|make|new|panic|print|println|real|recover"
            val goRules = buildRules("$goKeywords|$goBuiltins", lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasRawString = true)
            put("go", goRules)
            put("golang", goRules)

            // Rust
            val rustKeywords = "as|async|await|break|const|continue|crate|dyn|else|enum|extern|false|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|self|Self|static|struct|super|trait|true|type|union|unsafe|use|where|while|abstract|become|box|do|final|macro|override|priv|try|typeof|unsized|virtual|yield"
            val rustRules = buildRules(rustKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", hasAnnotation = true, annotationChar = '#')
            put("rust", rustRules)
            put("rs", rustRules)

            // SQL
            val sqlKeywords = "ADD|ALL|ALTER|AND|AS|ASC|BETWEEN|BY|CASCADE|CASE|CHECK|COLUMN|CONSTRAINT|COUNT|CREATE|CROSS|DATABASE|DEFAULT|DELETE|DESC|DISTINCT|DROP|ELSE|END|EXEC|EXISTS|FOREIGN|FROM|FULL|GROUP|HAVING|IF|IN|INDEX|INNER|INSERT|INTO|IS|JOIN|KEY|LEFT|LIKE|LIMIT|NOT|NULL|OFFSET|ON|OR|ORDER|OUTER|PRIMARY|REFERENCES|RIGHT|ROLLBACK|SELECT|SET|TABLE|THEN|TOP|TRUNCATE|UNION|UNIQUE|UPDATE|VALUES|VIEW|WHEN|WHERE|WITH"
            put("sql", buildRules(sqlKeywords, lineComment = "--", blockCommentStart = "/\\*", blockCommentEnd = "\\*/", caseInsensitiveKeywords = true))

            // Shell/Bash
            val shellKeywords = "alias|bg|break|case|cd|continue|declare|do|done|echo|elif|else|esac|eval|exec|exit|export|false|fg|fi|for|function|getopts|if|in|jobs|local|printf|read|readonly|return|select|set|shift|source|then|time|trap|true|type|typeset|ulimit|umask|unalias|unset|until|wait|while"
            val shellRules = buildRules(shellKeywords, lineComment = "#", hasHereDoc = false)
            put("bash", shellRules)
            put("sh", shellRules)
            put("shell", shellRules)
            put("zsh", shellRules)

            // JSON
            put("json", listOf(
                HighlightRule(Regex("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"\\s*(?=:)"), TokenType.PROPERTY),
                HighlightRule(Regex(":\\s*\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""), TokenType.STRING),
                HighlightRule(Regex("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""), TokenType.STRING),
                HighlightRule(Regex("\\b(?:true|false|null)\\b"), TokenType.KEYWORD),
                HighlightRule(Regex("-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b"), TokenType.NUMBER),
            ))

            // YAML
            val yamlRules = listOf(
                HighlightRule(Regex("#[^\n]*"), TokenType.COMMENT),
                HighlightRule(Regex("^[\\w][\\w\\s.-]*(?=\\s*:)", RegexOption.MULTILINE), TokenType.PROPERTY),
                HighlightRule(Regex("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""), TokenType.STRING),
                HighlightRule(Regex("'[^']*'"), TokenType.STRING),
                HighlightRule(Regex("\\b(?:true|false|yes|no|on|off|null|~)\\b", RegexOption.IGNORE_CASE), TokenType.KEYWORD),
                HighlightRule(Regex("-?\\b\\d+(?:\\.\\d+)?\\b"), TokenType.NUMBER),
            )
            put("yaml", yamlRules)
            put("yml", yamlRules)

            // TOML
            put("toml", listOf(
                HighlightRule(Regex("#[^\n]*"), TokenType.COMMENT),
                HighlightRule(Regex("\\[[^\\]]+\\]"), TokenType.PROPERTY),
                HighlightRule(Regex("^[\\w.-]+(?=\\s*=)", RegexOption.MULTILINE), TokenType.PROPERTY),
                HighlightRule(Regex("\"\"\"[\\s\\S]*?\"\"\""), TokenType.STRING),
                HighlightRule(Regex("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""), TokenType.STRING),
                HighlightRule(Regex("'[^']*'"), TokenType.STRING),
                HighlightRule(Regex("\\b(?:true|false)\\b"), TokenType.KEYWORD),
                HighlightRule(Regex("-?\\b\\d+(?:\\.\\d+)?\\b"), TokenType.NUMBER),
            ))

            // HTML
            val htmlRules = listOf(
                HighlightRule(Regex("<!--[\\s\\S]*?-->"), TokenType.COMMENT),
                HighlightRule(Regex("</?[a-zA-Z][a-zA-Z0-9-]*"), TokenType.KEYWORD),
                HighlightRule(Regex("/?>"), TokenType.KEYWORD),
                HighlightRule(Regex("\\b[a-zA-Z-]+(?=\\s*=)"), TokenType.PROPERTY),
                HighlightRule(Regex("\"[^\"]*\""), TokenType.STRING),
                HighlightRule(Regex("'[^']*'"), TokenType.STRING),
                HighlightRule(Regex("&[a-zA-Z]+;|&#\\d+;"), TokenType.NUMBER),
            )
            put("html", htmlRules)
            put("htm", htmlRules)
            put("xml", htmlRules)
            put("svg", htmlRules)

            // CSS
            val cssRules = listOf(
                HighlightRule(Regex("/\\*[\\s\\S]*?\\*/"), TokenType.COMMENT),
                HighlightRule(Regex("[.#][a-zA-Z_][a-zA-Z0-9_-]*"), TokenType.PROPERTY),
                HighlightRule(Regex("@[a-zA-Z-]+"), TokenType.ANNOTATION),
                HighlightRule(Regex("[a-zA-Z-]+(?=\\s*:)"), TokenType.KEYWORD),
                HighlightRule(Regex("\"[^\"]*\""), TokenType.STRING),
                HighlightRule(Regex("'[^']*'"), TokenType.STRING),
                HighlightRule(Regex("#[0-9a-fA-F]{3,8}\\b"), TokenType.NUMBER),
                HighlightRule(Regex("-?\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|pt|cm|mm|in|ex|ch|deg|rad|grad|turn|s|ms)?\\b"), TokenType.NUMBER),
            )
            put("css", cssRules)
            put("scss", cssRules)
            put("less", cssRules)

            // Ruby
            val rubyKeywords = "alias|and|begin|break|case|class|def|defined|do|else|elsif|end|ensure|false|for|if|in|module|next|nil|not|or|redo|rescue|retry|return|self|super|then|true|undef|unless|until|when|while|yield|require|include|extend|raise|attr_accessor|attr_reader|attr_writer|puts|print"
            val rubyRules = buildRules(rubyKeywords, lineComment = "#", hasDecorator = false)
            put("ruby", rubyRules)
            put("rb", rubyRules)

            // PHP
            val phpKeywords = "abstract|and|array|as|break|callable|case|catch|class|clone|const|continue|declare|default|die|do|echo|else|elseif|empty|enddeclare|endfor|endforeach|endif|endswitch|endwhile|eval|exit|extends|false|final|finally|fn|for|foreach|function|global|goto|if|implements|include|include_once|instanceof|insteadof|interface|isset|list|match|namespace|new|null|or|print|private|protected|public|readonly|require|require_once|return|static|switch|this|throw|trait|true|try|unset|use|var|while|xor|yield"
            put("php", buildRules(phpKeywords, lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/"))

            // Markdown (minimal highlighting)
            val markdownRules = listOf(
                HighlightRule(Regex("^#{1,6}\\s.*$", RegexOption.MULTILINE), TokenType.KEYWORD),
                HighlightRule(Regex("\\*\\*[^*]+\\*\\*"), TokenType.KEYWORD),
                HighlightRule(Regex("\\*[^*]+\\*"), TokenType.STRING),
                HighlightRule(Regex("`[^`]+`"), TokenType.STRING),
                HighlightRule(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), TokenType.ANNOTATION),
            )
            put("markdown", markdownRules)
            put("md", markdownRules)

            // Dockerfile
            val dockerKeywords = "FROM|RUN|CMD|LABEL|MAINTAINER|EXPOSE|ENV|ADD|COPY|ENTRYPOINT|VOLUME|USER|WORKDIR|ARG|ONBUILD|STOPSIGNAL|HEALTHCHECK|SHELL"
            val dockerRules = buildRules(dockerKeywords, lineComment = "#", caseInsensitiveKeywords = false)
            put("dockerfile", dockerRules)
            put("docker", dockerRules)

            // Makefile
            val makeRules = listOf(
                HighlightRule(Regex("#[^\n]*"), TokenType.COMMENT),
                HighlightRule(Regex("^[a-zA-Z_][a-zA-Z0-9_.-]*(?=\\s*[:?+]?=)", RegexOption.MULTILINE), TokenType.PROPERTY),
                HighlightRule(Regex("^[a-zA-Z_][a-zA-Z0-9_.-]*(?=\\s*:)", RegexOption.MULTILINE), TokenType.KEYWORD),
                HighlightRule(Regex("\"[^\"]*\""), TokenType.STRING),
                HighlightRule(Regex("'[^']*'"), TokenType.STRING),
                HighlightRule(Regex("\\$[({][^)}]+[)}]|\\$\\w+"), TokenType.ANNOTATION),
            )
            put("makefile", makeRules)
            put("make", makeRules)

            // Gradle (Groovy/Kotlin DSL)
            put("gradle", kotlinRules)
            put("groovy", buildRules(javaKeywords + "|def|trait|in|as", lineComment = "//", blockCommentStart = "/\\*", blockCommentEnd = "\\*/"))
        }
    }

    private fun buildRules(
        keywords: String,
        lineComment: String? = null,
        blockCommentStart: String? = null,
        blockCommentEnd: String? = null,
        hasAnnotation: Boolean = false,
        annotationChar: Char = '@',
        hasPreprocessor: Boolean = false,
        hasTripleQuote: Boolean = false,
        hasDecorator: Boolean = false,
        stringTemplate: Boolean = false,
        hasTemplateLiteral: Boolean = false,
        hasRawString: Boolean = false,
        caseInsensitiveKeywords: Boolean = false,
        hasHereDoc: Boolean = false,
    ): List<HighlightRule> {
        val rules = mutableListOf<HighlightRule>()

        // 1. Comments (highest priority — match first)
        if (blockCommentStart != null && blockCommentEnd != null) {
            rules.add(HighlightRule(Regex("$blockCommentStart[\\s\\S]*?$blockCommentEnd"), TokenType.COMMENT))
        }
        if (lineComment != null) {
            rules.add(HighlightRule(Regex("${Regex.escape(lineComment)}[^\n]*"), TokenType.COMMENT))
        }

        // 2. Strings
        if (hasTripleQuote) {
            rules.add(HighlightRule(Regex("\"\"\"[\\s\\S]*?\"\"\""), TokenType.STRING))
            rules.add(HighlightRule(Regex("'''[\\s\\S]*?'''"), TokenType.STRING))
        }
        if (hasRawString) {
            rules.add(HighlightRule(Regex("`[^`]*`"), TokenType.STRING))
        }
        if (hasTemplateLiteral) {
            rules.add(HighlightRule(Regex("`[^`]*`"), TokenType.STRING))
        }
        rules.add(HighlightRule(Regex("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""), TokenType.STRING))
        rules.add(HighlightRule(Regex("'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'"), TokenType.STRING))

        // 3. Preprocessor
        if (hasPreprocessor) {
            rules.add(HighlightRule(Regex("^\\s*#[a-zA-Z]+[^\n]*", RegexOption.MULTILINE), TokenType.ANNOTATION))
        }

        // 4. Annotations / Decorators
        if (hasAnnotation) {
            rules.add(HighlightRule(Regex("${Regex.escape(annotationChar.toString())}[a-zA-Z_][a-zA-Z0-9_.]*"), TokenType.ANNOTATION))
        }
        if (hasDecorator) {
            rules.add(HighlightRule(Regex("@[a-zA-Z_][a-zA-Z0-9_.]*"), TokenType.ANNOTATION))
        }

        // 5. Numbers
        rules.add(HighlightRule(Regex("\\b0[xX][0-9a-fA-F_]+[lLuU]*\\b"), TokenType.NUMBER))
        rules.add(HighlightRule(Regex("\\b0[bB][01_]+[lLuU]*\\b"), TokenType.NUMBER))
        rules.add(HighlightRule(Regex("\\b\\d[\\d_]*\\.\\d[\\d_]*(?:[eE][+-]?\\d+)?[fFdD]?\\b"), TokenType.NUMBER))
        rules.add(HighlightRule(Regex("\\b\\d[\\d_]*[lLuUfFdD]?\\b"), TokenType.NUMBER))

        // 6. Keywords
        val keywordOptions = if (caseInsensitiveKeywords) setOf(RegexOption.IGNORE_CASE) else emptySet()
        rules.add(HighlightRule(Regex("\\b(?:$keywords)\\b", keywordOptions), TokenType.KEYWORD))

        return rules
    }
}

/**
 * 高亮规则：将匹配的文本区域标记为指定的 token 类型。
 */
internal data class HighlightRule(
    val pattern: Regex,
    val tokenType: TokenType,
    val groupIndex: Int = 0,
)

/**
 * Token 类型，对应不同的语法元素。
 */
enum class TokenType {
    KEYWORD,
    STRING,
    COMMENT,
    NUMBER,
    ANNOTATION,
    PROPERTY,
}

/**
 * 语法高亮配色方案。
 */
data class SyntaxColorScheme(
    val keyword: Color = Color(0xFFCF222E),
    val string: Color = Color(0xFF0A3069),
    val comment: Color = Color(0xFF6E7781),
    val number: Color = Color(0xFF0550AE),
    val annotation: Color = Color(0xFF8250DF),
    val property: Color = Color(0xFF953800),
) {
    fun styleFor(tokenType: TokenType): SpanStyle? = when (tokenType) {
        TokenType.KEYWORD -> SpanStyle(color = keyword, fontWeight = FontWeight.Medium)
        TokenType.STRING -> SpanStyle(color = string)
        TokenType.COMMENT -> SpanStyle(color = comment, fontStyle = FontStyle.Italic)
        TokenType.NUMBER -> SpanStyle(color = number)
        TokenType.ANNOTATION -> SpanStyle(color = annotation)
        TokenType.PROPERTY -> SpanStyle(color = property)
    }

    companion object {
        /** GitHub 风格的亮色主题。 */
        val GitHubLight = SyntaxColorScheme()

        /** GitHub 风格的暗色主题。 */
        val GitHubDark = SyntaxColorScheme(
            keyword = Color(0xFFFF7B72),
            string = Color(0xFFA5D6FF),
            comment = Color(0xFF8B949E),
            number = Color(0xFF79C0FF),
            annotation = Color(0xFFD2A8FF),
            property = Color(0xFFFFA657),
        )
    }
}
