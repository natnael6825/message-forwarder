package expo.modules.smsforwarder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
  private const val CHANNEL = "relay-forwarding"
  private const val ID = 8742

  fun update(context: Context, configs: org.json.JSONArray) {
    val enabled = (0 until configs.length()).count { configs.getJSONObject(it).optBoolean("enabled") }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (enabled == 0) { manager.cancel(ID); return }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(NotificationChannel(CHANNEL, "Relay forwarding", NotificationManager.IMPORTANCE_LOW))
    }
    val notification = NotificationCompat.Builder(context, CHANNEL)
      .setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle("Relay is forwarding")
      .setContentText("$enabled forwarding ${if (enabled == 1) "configuration" else "configurations"} active")
      .setOngoing(true).setCategory(NotificationCompat.CATEGORY_SERVICE).setShowWhen(false).build()
    manager.notify(ID, notification)
  }
}
