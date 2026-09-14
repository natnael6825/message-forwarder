package expo.modules.smsforwarder

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

class SmsSentReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val id = intent.getStringExtra("id") ?: return
    val part = intent.getIntExtra("part", -1)
    if (part < 0) return
    val code = resultCode
    ForwarderStore.update(context, id) { entry ->
      val completed = entry.getJSONArray("completedParts")
      if ((0 until completed.length()).none { completed.getInt(it) == part }) completed.put(part)
      if (code != Activity.RESULT_OK) {
        val reason = when (code) {
          SmsManager.RESULT_ERROR_NO_SERVICE -> "No mobile service."
          SmsManager.RESULT_ERROR_RADIO_OFF -> "Mobile radio is off."
          SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "The phone's SMS sending limit was reached."
          else -> "SMS failed (code $code). Check your default SMS SIM, signal, and balance."
        }
        entry.put("status", "failed").put("detail", "$reason Some parts may have sent; no automatic retry.")
      } else if (completed.length() == entry.optInt("totalParts") && entry.getString("status") != "failed") {
        entry.put("status", "sent").put("detail", "Sent to the mobile network; delivery is not confirmed.")
      }
    }
  }
}
