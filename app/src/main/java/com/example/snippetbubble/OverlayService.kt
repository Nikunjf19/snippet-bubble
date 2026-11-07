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

    private lateinit var wm: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var panelBox: LinearLayout
    private lateinit var listHolder: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun category(label: String) = when {
        label.startsWith("HTML") -> "HTML"
        label.startsWith("CSS") -> "CSS"
        else -> "JS"
    }

    private fun showCategory(cat: String) {
        listHolder.removeAllViews()
        allSnippets.filter { category(it.first) == cat }.forEach { (label, code) ->
            listHolder.addView(Button(this).apply {
                text = label
                isAllCaps = false
                setOnClickListener { copyCode(label, code) }
            })
        }
    }

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

        panelBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#EE10141A"))
            setPadding(dp(6), dp(6), dp(6), dp(6))
            layoutParams = LinearLayout.LayoutParams(dp(210), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("HTML", "CSS", "JS").forEach { cat ->
            tabs.addView(Button(this).apply {
                text = cat
                isAllCaps = false
                setOnClickListener { showCategory(cat) }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }

        listHolder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroller = ScrollView(this).apply {
            addView(listHolder)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240))
        }

        val close = Button(this).apply {
            text = "Close bubble"
            isAllCaps = false
            setOnClickListener { stopSelf() }
        }

        panelBox.addView(tabs)
        panelBox.addView(scroller)
        panelBox.addView(close)
        root.addView(bubble)
        root.addView(panelBox)
        showCategory("HTML")

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
                            panelBox.visibility =
                                if (panelBox.visibility == View.GONE) View.VISIBLE else View.GONE
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
