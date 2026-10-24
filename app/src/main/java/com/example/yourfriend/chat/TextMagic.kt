package com.example.yourfriend.chat

private val sentenceBreak = Regex("(?<=[.!?…])\\s+")
private val extraSpaces = Regex("[ \\t]+")
private val lineBreaks = Regex("\\r?\\n+")
private val listMarkers = Regex("(?m)^\\s*[-*•]\\s+")

/**
 * Убирает служебные префиксы и markdown, оставляя живую реплику.
 * Вопрос не добавляется насильно: иногда другу достаточно просто поддержать.
 */
fun cleanReply(text: String, friendName: String): String {
    var clean = text.trim()
    if (clean.isEmpty()) return "Я здесь."

    val namePrefix = Regex(
        "^${Regex.escape(friendName)}\\s*[:\\-—]\\s*",
        RegexOption.IGNORE_CASE
    )
    clean = clean.replace(namePrefix, "")
    clean = clean.replace("**", "").replace("__", "")
    clean = clean.replace(listMarkers, "")
    clean = clean.replace(lineBreaks, " ")
    clean = clean.replace(extraSpaces, " ").trim()
    clean = clean.trim('"', '«', '»', '“', '”')

    return limitToOneQuestion(clean).ifEmpty { "Я здесь." }
}

fun limitToOneQuestion(text: String): String {
    val clean = text.trim()
    if (clean.isEmpty()) return ""

    val sentences = sentenceBreak.split(clean)
    val kept = mutableListOf<String>()
    var seenQuestion = false

    sentences.forEach { sentence ->
        if (!sentence.contains('?')) {
            kept += sentence
            return@forEach
        }
        if (seenQuestion) return@forEach

        val questionEnd = sentence.indexOf('?')
        kept += sentence.substring(0, questionEnd + 1).trim()
        seenQuestion = true
    }

    var result = kept.filter { it.isNotBlank() }.joinToString(" ").trim()
    if (result.isNotEmpty() &&
        !result.endsWith(".") &&
        !result.endsWith("!") &&
        !result.endsWith("?") &&
        !result.endsWith("…")
    ) {
        result += "."
    }
    return result.ifEmpty { clean }
}
