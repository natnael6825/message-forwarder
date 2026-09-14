package expo.modules.smsforwarder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class IncomingSmsReceiver : BroadcastReceiver() {
  @Suppress("DEPRECATION")
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
    if (messages.isNullOrEmpty()) return
    val sender = messages.first().originatingAddress ?: return
    if (messages.any { it.originatingAddress != sender }) return
    val body = messages.joinToString("") { it.messageBody ?: "" }
    val configs = ForwarderStore.configs(context)
    for (configIndex in 0 until configs.length()) {
      val config = configs.getJSONObject(configIndex)
      val sendersJson = config.optJSONArray("senders") ?: JSONArray()
      val senders = (0 until sendersJson.length()).map { sendersJson.getString(it) }
      val recipientsJson = config.optJSONArray("recipients") ?: JSONArray()
      if (!config.optBoolean("enabled") || recipientsJson.length() == 0 || !senders.any { ForwardingRules.normalize(it) == ForwardingRules.normalize(sender) } || body.isBlank() || body.startsWith(ForwardingRules.MARKER)) continue
      val removalsJson = config.optJSONArray("removals") ?: JSONArray()
      val removals = (0 until removalsJson.length()).map { removalsJson.getString(it) }
      val cleanedBody = ForwardingRules.cleanBody(body, removals)
      if (cleanedBody.isBlank()) continue
      for (recipientIndex in 0 until recipientsJson.length()) {
        val recipient = recipientsJson.getString(recipientIndex)
        val id = UUID.randomUUID().toString()
        val fingerprint = MessageDigest.getInstance("SHA-256").digest("${config.optString("id")}|$sender|${messages.first().timestampMillis}|$body|$recipient".toByteArray()).joinToString("") { "%02x".format(it) }
        val entry = JSONObject().put("id", id).put("configId", config.optString("id")).put("configName", config.optString("name", "Forwarding rule"))
          .put("sender", sender).put("recipient", recipient).put("time", System.currentTimeMillis()).put("status", "pending")
          .put("detail", "Waiting for the mobile network").put("message", cleanedBody).put("completedParts", JSONArray())
        if (!ForwarderStore.reserve(context, fingerprint, entry)) continue
        send(context, recipient, sender, cleanedBody, id, entry)
      }
    }
  }

  private fun send(context: Context, recipient: String, sender: String, body: String, id: String, entry: JSONObject) {
    try {
      check(context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
        "SMS permission is missing. Open Relay to enable it again."
      }
      // Uses the user's default SMS SIM. No READ_PHONE_STATE or inbox access needed.
      val manager = SmsManager.getDefault()
      val parts = manager.divideMessage("${ForwardingRules.MARKER}From $sender\n$body")
      ForwarderStore.update(context, id) { it.put("totalParts", parts.size) }
      val callbacks = ArrayList<PendingIntent>()
      parts.indices.forEach { index ->
        val callback = Intent(context, SmsSentReceiver::class.java)
          .setAction("${context.packageName}.SMS_SENT.$id.$index")
          .putExtra("id", id).putExtra("part", index)
        callbacks.add(PendingIntent.getBroadcast(context, 0, callback,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
      }
      manager.sendMultipartTextMessage(recipient, null, parts, callbacks, null)
    } catch (error: Exception) {
      ForwarderStore.update(context, id) {
        it.put("status", "failed").put("detail", error.message ?: "Could not send SMS.")
      }
    }
  }
}
