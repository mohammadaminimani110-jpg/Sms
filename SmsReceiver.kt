package com.ilya.banksmsreader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = context.getSharedPreferences("bank_sms", Context.MODE_PRIVATE)
        val saved = JSONArray(prefs.getString("transactions", "[]") ?: "[]")
        for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            val body = sms.messageBody ?: continue
            val sender = sms.originatingAddress ?: "نامشخص"
            val parsed = parse(body, sender, sms.timestampMillis) ?: continue
            var exists = false
            for (i in 0 until saved.length()) {
                if (saved.optJSONObject(i)?.optString("key") == parsed.optString("key")) exists = true
            }
            if (!exists) saved.put(parsed)
        }
        prefs.edit().putString("transactions", saved.toString()).apply()
    }

    private fun parse(body: String, sender: String, time: Long): JSONObject? {
        val normalized = body.replace(",", "").replace("،", "").replace("٬", "")
        val amount = Regex("([0-9]{3,})\\\\s*(?:ریال|تومان)?").find(normalized)
            ?.groupValues?.get(1)?.toLongOrNull() ?: return null
        val type = when {
            listOf("واریز", "دریافت", "افزایش موجودی").any { body.contains(it) } -> "واریز"
            listOf("برداشت", "خرید", "کسر", "انتقال", "پرداخت").any { body.contains(it) } -> "برداشت"
            else -> return null
        }
        val date = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(time))
        return JSONObject().apply {
            put("key", "$sender|$time|$amount|$type")
            put("type", type); put("amount", amount); put("sender", sender)
            put("date", date); put("text", body)
        }
    }
}
