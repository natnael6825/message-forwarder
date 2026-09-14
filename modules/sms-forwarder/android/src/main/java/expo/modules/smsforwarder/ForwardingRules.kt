package expo.modules.smsforwarder

import java.util.Locale

object ForwardingRules {
  const val MARKER = "[Relay] "

  fun normalize(value: String): String {
    if (value.any { it.isLetter() }) return value.trim().uppercase(Locale.ROOT)
    val compact = value.trim().replace(Regex("[\\s()\\-]"), "")
    return when {
      compact.startsWith("+") -> compact.drop(1)
      compact.startsWith("00") -> compact.drop(2)
      else -> compact
    }
  }

  fun validSender(value: String): Boolean {
    val trimmed = value.trim()
    return Regex("[0-9]{3,15}").matches(normalize(value)) ||
      (trimmed.length in 1..32 && trimmed.any { it.isLetter() } && trimmed.none { it.isISOControl() })
  }
  fun validRecipient(value: String) = Regex("(?:\\+[1-9][0-9]{7,14}|0[0-9]{7,14})").matches(value.trim())

  fun cleanBody(body: String, removals: List<String>): String {
    var cleaned = body
    removals.forEach { pattern -> cleaned = Regex(pattern, setOf(RegexOption.IGNORE_CASE)).replace(cleaned, "") }
    return cleaned.replace(Regex("[ \\t]{2,}"), " ").replace(Regex("\\n{3,}"), "\\n\\n").trim()
  }

  fun shouldForward(enabled: Boolean, senders: List<String>, recipient: String, sender: String, body: String): Boolean {
    val source = normalize(sender)
    return enabled && validRecipient(recipient) && validSender(sender) &&
      source != normalize(recipient) && senders.any { normalize(it) == source } &&
      body.isNotBlank() && !body.startsWith(MARKER)
  }
}
