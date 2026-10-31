package com.example.firetvhome

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = WebViewClient()
        web.loadUrl("file:///android_asset/index.html")
        web.requestFocus()
    }

    // Re-scan installed apps whenever you come back to the home screen
    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) web.evaluateJavascript("if(window.scan)scan()", null)
    }

    inner class Bridge {
        @JavascriptInterface
        fun launch(pkg: String) {
            if (pkg.startsWith("settings:")) {
                val a = if (pkg == "settings:wifi") android.provider.Settings.ACTION_WIFI_SETTINGS else android.provider.Settings.ACTION_SETTINGS
                startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return
            }
            val i = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                ?: packageManager.getLaunchIntentForPackage(pkg) ?: return
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        @JavascriptInterface
        fun getApps(): String {
            val pm = packageManager
            val out = JSONArray()
            val seen = HashSet<String>()
            for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
                val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
                for (ri in pm.queryIntentActivities(q, 0)) {
                    val pkg = ri.activityInfo.packageName
                    if (pkg == packageName || !seen.add(pkg)) continue
                    val d = ri.loadIcon(pm)
                    val bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
                    d.setBounds(0, 0, 160, 160); d.draw(Canvas(bmp))
                    val bos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, bos)
                    out.put(JSONObject().put("name", ri.loadLabel(pm).toString()).put("pkg", pkg)
                        .put("icon", Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)))
                }
            }
            return out.toString()
        }
    }
}
