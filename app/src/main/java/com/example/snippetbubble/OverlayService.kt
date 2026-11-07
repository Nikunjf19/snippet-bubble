package com.example.snippetbubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class OverlayService : Service() {

    // ADD YOUR OWN SNIPPETS HERE: one line each (label to code)
    private val snippets = listOf(
        "HTML skeleton" to "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title></title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  \n  <script src=\"script.js\"></script>\n</body>\n</html>",
        "CSS font-size" to "font-size: ;",
        "CSS color" to "color: ;",
        "CSS background" to "background-color: ;",
        "CSS font-family" to "font-family: , sans-serif;",
        "CSS text-align" to "text-align: center;",
        "CSS margin" to "margin: ;",
        "CSS padding" to "padding: ;",
        "CSS border" to "border: 1px solid ;",
        "CSS radius" to "border-radius: ;",
        "CSS width+height" to "width: ;\nheight: ;",
        "CSS shadow" to "box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);",
        "CSS flex center" to "display: flex;\njustify-content: center;\nalign-items: center;",
        "CSS flex column" to "display: flex;\nflex-direction: column;\ngap: ;",
        "CSS grid" to "display: grid;\ngrid-template-columns: repeat(3, 1fr);\ngap: ;",
        "CSS position" to "position: absolute;\ntop: ;\nleft: ;",
        "CSS hover" to ":hover {\n  \n}",
        "CSS media query" to "@media (max-width: 600px) {\n  \n}",
        "querySelectorAll" to "document.querySelectorAll(\"\");",
        "createElement" to "const el = document.createElement(\"\");",
        "innerHTML" to "document.getElementById(\"\").innerHTML = \"\";",
        "input value" to "document.getElementById(\"\").value;",
        "classList add" to "document.getElementById(\"\").classList.add(\"\");",
        "classList remove" to "document.getElementById(\"\").classList.remove(\"\");",
        "style color" to "document.getElementById(\"\").style.color = \"\";",
        "for loop" to "for (let i = 0; i < ; i++) {\n  \n}",
        "function" to "function () {\n  \n}",
        "arrow function" to "const  = () => {\n  \n};",
        "if else" to "if () {\n  \n} else {\n  \n}",
        "setTimeout" to "setTimeout(() => {\n  \n}, 1000);",
        "async fetch" to "async function getData() {\n  const res = await fetch(\"\");\n  const data = await res.json();\n  console.log(data);\n}",
        "localStorage set" to "localStorage.setItem(\"\", \"\");",
        "localStorage get" to "localStorage.getItem(\"\");",
        "console.log" to "console.log();",
        "getElementById" to "document.getElementById(\"\");",
        "querySelector" to "document.querySelector(\"\");",
        "click event" to "document.getElementById(\"\").addEventListener(\"click\", () => {\n  \n});",
        "set text" to "document.getElementById(\"\").textContent = \"\";",
        "toggle class" to "document.getElementById(\"\").classList.toggle(\"\");",
        "fetch" to "fetch(\"\")\n  .then(res => res.json())\n  .then(data => console.log(data));"
    )

    private lateinit var wm: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var scroller: ScrollView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val bubble = TextView(this).apply {
            text = "{ }"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2F6FED"))
            }
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EE10141A"))
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        snippets.forEach { (label, code) ->
            panel.addView(Button(this).apply {
                text = label
                isAllCaps = false
                setOnClickListener { copyCode(label, code) }
            })
        }
        panel.addView(Button(this).apply {
            text = "Close bubble"
            isAllCaps = false
            setOnClickListener { stopSelf() }
        })

        scroller = ScrollView(this).apply {
            visibility = View.GONE
            addView(panel)
            layoutParams = LinearLayout.LayoutParams(dp(190), dp(280))
        }

        root.addView(bubble)
        root.addView(scroller)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dp(200)
        }

        bubble.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = e.rawX
                        touchY = e.rawY
                        moved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - touchX).toInt()
                        val dy = (e.rawY - touchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) moved = true
                        params.x = startX + dx
                        params.y = startY + dy
                        wm.updateViewLayout(root, params)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            scroller.visibility =
                                if (scroller.visibility == View.GONE) View.VISIBLE else View.GONE
                        }
                    }
                }
                return true
            }
        })

        wm.addView(root, params)
    }

    private fun copyCode(label: String, code: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, code))
        Toast.makeText(this, "Copied: $label", Toast.LENGTH_SHORT).show()
    }

    private fun startAsForeground() {
        val channelId = "snippet_bubble"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Snippet bubble", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, channelId)
            .setContentTitle("Snippet bubble is running")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()
        startForeground(1, n)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        if (::root.isInitialized) wm.removeView(root)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
