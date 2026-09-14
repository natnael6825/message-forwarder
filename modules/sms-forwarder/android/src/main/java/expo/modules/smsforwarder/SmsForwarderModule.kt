package expo.modules.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import org.json.JSONArray
import org.json.JSONObject
import android.provider.Telephony

class SmsForwarderModule : Module() {
  private fun context() = requireNotNull(appContext.reactContext) { "App is not ready." }

  override fun definition() = ModuleDefinition {
    Name("SmsForwarder")
    AsyncFunction("getState") {
      val ctx = context()
      val configs = ForwarderStore.configs(ctx)
      NotificationHelper.update(ctx, configs)
      JSONObject().put("configs", configs)
        .put("history", ForwarderStore.history(ctx)).toString()
    }
    AsyncFunction("saveConfigs") { json: String ->
      val ctx = context()
      val configs = JSONArray(json)
      require(configs.length() <= 50) { "You can create up to 50 forwarding configurations." }
      for (i in 0 until configs.length()) {
        val config = configs.getJSONObject(i)
        val sendersJson = config.getJSONArray("senders")
        val senders = (0 until sendersJson.length()).map { sendersJson.getString(it).trim() }
        val recipientsJson = config.getJSONArray("recipients")
        val recipients = (0 until recipientsJson.length()).map { recipientsJson.getString(it).trim() }
        val removalsJson = config.optJSONArray("removals") ?: JSONArray()
        val removals = (0 until removalsJson.length()).map { removalsJson.getString(it).trim() }
        require(senders.size <= 50 && senders.all { ForwardingRules.validSender(it) }) { "Enter valid sender numbers or company names." }
        require(senders.map { ForwardingRules.normalize(it) }.distinct().size == senders.size) { "Remove duplicate senders." }
        require(recipients.size in 1..20 && recipients.all { ForwardingRules.validRecipient(it) }) { "Add one or more recipient numbers." }
        require(removals.size <= 20 && removals.all { it.isNotEmpty() && it.length <= 200 }) { "Cleanup patterns must be 1–200 characters." }
        removals.forEach { pattern -> try { Regex(pattern) } catch (_: Exception) { throw IllegalArgumentException("Invalid cleanup regex: $pattern") } }
        require(senders.none { source -> recipients.any { ForwardingRules.normalize(source) == ForwardingRules.normalize(it) } }) { "A recipient cannot also be a sender." }
        if (config.getBoolean("enabled")) require(senders.isNotEmpty() && recipients.isNotEmpty() && listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS).all {
          ctx.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }) { "Allow SMS permissions before enabling forwarding." }
      }
      ForwarderStore.saveConfigs(ctx, configs)
      NotificationHelper.update(ctx, configs)
      configs.toString()
    }
    AsyncFunction("getRecentMessages") {
      val ctx = context()
      require(ctx.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) { "Allow SMS access to choose a received message." }
      val result = JSONArray()
      val projection = arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE)
      ctx.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, "${Telephony.Sms.Inbox.DATE} DESC LIMIT 100")?.use { cursor ->
        val address = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
        val body = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
        val date = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)
        while (cursor.moveToNext()) result.put(JSONObject().put("sender", cursor.getString(address) ?: "Unknown").put("body", cursor.getString(body) ?: "").put("time", cursor.getLong(date)))
      }
      result.toString()
    }
    AsyncFunction("clearHistory") { ForwarderStore.clearHistory(context()) }
  }
}
