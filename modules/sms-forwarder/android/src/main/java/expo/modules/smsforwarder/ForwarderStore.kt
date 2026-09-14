package expo.modules.smsforwarder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ForwarderStore {
  private fun prefs(context: Context) = context.getSharedPreferences("relay-private", Context.MODE_PRIVATE)
  private fun array(context: Context, key: String) = JSONArray(prefs(context).getString(key, "[]") ?: "[]")

  @Synchronized fun configs(context: Context): JSONArray {
    val stored = prefs(context).getString("configs", null)
    // An explicitly saved empty array means the user deleted every rule.
    // Do not fall back to the legacy single-rule key in that case.
    if (stored != null) return JSONArray(stored)
    val legacy = prefs(context).getString("config", null)
    val migrated = JSONArray()
    if (legacy != null) {
      val old = JSONObject(legacy)
      migrated.put(JSONObject().put("id", "legacy").put("name", "My forwarding rule")
        .put("enabled", old.optBoolean("enabled", false)).put("senders", old.optJSONArray("senders") ?: JSONArray())
        .put("recipients", JSONArray().put(old.optString("recipient", ""))).put("removals", JSONArray()))
    }
    if (migrated.length() > 0) prefs(context).edit().putString("configs", migrated.toString()).apply()
    return migrated
  }

  @Synchronized fun saveConfigs(context: Context, configs: JSONArray) {
    check(prefs(context).edit().putString("configs", configs.toString()).commit()) { "Could not save settings." }
  }

  @Synchronized fun notificationConfig(context: Context): JSONArray = configs(context)

  @Synchronized fun history(context: Context): JSONArray = array(context, "history")

  @Synchronized fun clearHistory(context: Context) {
    prefs(context).edit().putString("history", "[]").commit()
  }

  // Reserve before sending: duplicate broadcasts cannot send the same SMS twice.
  @Synchronized fun reserve(context: Context, fingerprint: String, entry: JSONObject): Boolean {
    val seen = array(context, "seen")
    if ((0 until seen.length()).any { seen.getString(it) == fingerprint }) return false
    val nextSeen = JSONArray().put(fingerprint)
    for (i in 0 until minOf(seen.length(), 199)) nextSeen.put(seen.get(i))
    val old = history(context)
    val next = JSONArray().put(entry)
    for (i in 0 until minOf(old.length(), 49)) next.put(old.get(i))
    return prefs(context).edit().putString("seen", nextSeen.toString()).putString("history", next.toString()).commit()
  }

  @Synchronized fun update(context: Context, id: String, change: (JSONObject) -> Unit) {
    val entries = history(context)
    for (i in 0 until entries.length()) {
      val entry = entries.getJSONObject(i)
      if (entry.getString("id") == id) change(entry)
    }
    prefs(context).edit().putString("history", entries.toString()).commit()
  }
}
