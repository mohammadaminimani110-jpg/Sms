package com.ilya.banksmsreader

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private val prefs by lazy { getSharedPreferences("bank_sms", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            layoutDirection = LinearLayout.LayoutDirection.RTL
        }
        root.addView(TextView(this).apply { text = "خوانشگر پیامک بانکی"; textSize = 24f })
        root.addView(TextView(this).apply { text = "پیامک‌های جدید بانکی روی همین گوشی بررسی می‌شوند."; textSize = 14f })
        root.addView(Button(this).apply {
            text = "فعال‌کردن دسترسی پیامک"
            setOnClickListener {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECEIVE_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECEIVE_SMS), 10)
                } else refresh()
            }
        })
        root.addView(Button(this).apply { text = "به‌روزرسانی فهرست"; setOnClickListener { refresh() } })
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    private fun refresh() {
        val arr = JSONArray(prefs.getString("transactions", "[]") ?: "[]")
        var deposits = 0L
        var withdrawals = 0L
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            if (item.optString("type") == "واریز") deposits += item.optLong("amount")
            else withdrawals += item.optLong("amount")
        }
        while (root.childCount > 4) root.removeViewAt(4)
        root.addView(TextView(this).apply {
            text = "مجموع واریز: $deposits ریال\nمجموع برداشت: $withdrawals ریال\n\nتراکنش‌ها:"
            textSize = 17f
        })
        for (i in arr.length() - 1 downTo 0) {
            val item = arr.getJSONObject(i)
            root.addView(TextView(this).apply {
                text = "${item.optString("type")} | ${item.optLong("amount")} ریال\n${item.optString("sender")} • ${item.optString("date")}\n${item.optString("text")}"
                textSize = 13f
                setPadding(10, 14, 10, 14)
            })
        }
    }
}
