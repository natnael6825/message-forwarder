package expo.modules.smsforwarder

import org.junit.Assert.*
import org.junit.Test

class ForwardingRulesTest {
  private val target = "+254798765432"
  @Test fun onlySelectedSenderIsForwarded() {
    assertTrue(ForwardingRules.shouldForward(true, listOf("+254712345678"), target, "00254 712-345678", "Hello"))
    assertFalse(ForwardingRules.shouldForward(true, listOf("+254712345678"), target, "+254711111111", "Hello"))
    assertFalse(ForwardingRules.shouldForward(true, listOf("+254712345678"), target, "0712345678", "Hello"))
  }
  @Test fun pausedEmptyAndInvalidRulesNeverForward() {
    assertFalse(ForwardingRules.shouldForward(false, listOf("12345"), target, "12345", "Hello"))
    assertFalse(ForwardingRules.shouldForward(true, emptyList(), target, "12345", "Hello"))
    assertFalse(ForwardingRules.shouldForward(true, listOf("12345"), "invalid", "12345", "Hello"))
    assertFalse(ForwardingRules.shouldForward(true, listOf("12345"), target, "12345", ""))
  }
  @Test fun loopsAreRejected() {
    assertFalse(ForwardingRules.shouldForward(true, listOf(target), target, target, "Hello"))
    assertFalse(ForwardingRules.shouldForward(true, listOf("12345"), target, "12345", "[Relay] From someone\nHello"))
  }
  @Test fun shortCodesAndInternationalRecipients() {
    assertTrue(ForwardingRules.shouldForward(true, listOf("12345"), target, "12345", "Hello"))
    assertTrue(ForwardingRules.validRecipient("0798765432"))
    assertFalse(ForwardingRules.validRecipient("0798"))
    assertFalse(ForwardingRules.validRecipient("+0001234567"))
    assertTrue(ForwardingRules.validRecipient(target))
  }
  @Test fun companyNamesMatchExactlyIgnoringCase() {
    assertTrue(ForwardingRules.shouldForward(true, listOf(" Safaricom "), target, "SAFARICOM", "Hello"))
    assertTrue(ForwardingRules.validSender("M-PESA"))
    assertFalse(ForwardingRules.shouldForward(true, listOf("M-PESA"), target, "MPESA", "Hello"))
    assertFalse(ForwardingRules.shouldForward(true, listOf("BANK"), target, "BANK-FAKE", "Hello"))
    assertFalse(ForwardingRules.validSender("BANK\nOTHER"))
    assertFalse(ForwardingRules.validSender(""))
  }
  @Test fun cleanupPatternsRemoveSensitiveBalanceText() {
    val pattern = "balance\\s+is\\s+(?:ETB\\s*)?\\d+(?:\\.\\d+)?"
    assertEquals("Your is now available.", ForwardingRules.cleanBody("Your balance is ETB 234 is now available.", listOf(pattern)))
    assertEquals("Alert", ForwardingRules.cleanBody("Alert 40", listOf("\\s+\\d+$")))
  }
}
