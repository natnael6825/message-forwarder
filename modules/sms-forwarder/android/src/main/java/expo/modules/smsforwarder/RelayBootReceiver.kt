package expo.modules.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RelayBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) NotificationHelper.update(context, ForwarderStore.configs(context))
  }
}
