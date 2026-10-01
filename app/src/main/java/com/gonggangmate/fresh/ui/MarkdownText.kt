package com.gonggangmate.fresh.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Renders the compact Markdown subset returned by the campus companion. */
@Composable
fun MarkdownText(
    value: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 22.sp,
) {
    val annotated = remember(value, fontSize) { markdownAnnotatedString(value, fontSize) }
    Text(annotated, modifier = modifier, color = color, fontSize = fontSize, lineHeight = lineHeight)
}

private fun markdownAnnotatedString(markdown: String, baseSize: TextUnit): AnnotatedString = buildAnnotatedString {
    val inlinePattern = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\*(.+?)\\*")
    markdown.replace("\r\n", "\n").lines().forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()
        val heading = Regex("^(#{1,3})\\s+(.+)$").matchEntire(line)
        val bullet = Regex("^\\s*[-*+]\\s+(.+)$").matchEntire(line)
        val numbered = Regex("^\\s*(\\d+[.)])\\s+(.+)$").matchEntire(line)
        when {
            line.isBlank() -> Unit
            heading != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = baseSize * 1.15f)) {
                    appendInline(heading.groupValues[2], inlinePattern)
                }
            }
            bullet != null -> {
                append("•  ")
                appendInline(bullet.groupValues[1], inlinePattern)
            }
            numbered != null -> {
                append(numbered.groupValues[1])
                append("  ")
                appendInline(numbered.groupValues[2], inlinePattern)
            }
            else -> appendInline(line, inlinePattern)
        }
        if (index != markdown.replace("\r\n", "\n").lines().lastIndex) append("\n")
    }
}

private fun AnnotatedString.Builder.appendInline(value: String, pattern: Regex) {
    var cursor = 0
    pattern.findAll(value).forEach { match ->
        append(value.substring(cursor, match.range.first))
        val bold = match.groups[1]?.value
        val code = match.groups[2]?.value
        val italic = match.groups[3]?.value
        when {
            bold != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            code != null -> withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(code) }
            italic != null -> withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(italic) }
        }
        cursor = match.range.last + 1
    }
    append(value.substring(cursor))
}
