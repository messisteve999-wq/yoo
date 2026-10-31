package com.steve.yoo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class FloatingService : Service() {

    companion object {
        var instance: FloatingService? = null
    }

    private lateinit var wm: WindowManager
    private lateinit var bubble: DemonView
    private lateinit var bubbleLp: WindowManager.LayoutParams
    private var panel: LinearLayout? = null
    private var logView: LinearLayout? = null
    private var input: EditText? = null
    private var selected: String? = null
    private val ui = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()

        bubble = DemonView(this)
        bubbleLp = overlayParams(dp(64), dp(64), false).apply { x = 0; y = dp(200) }
        wm.addView(bubble, bubbleLp)

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = bubbleLp.x; startY = bubbleLp.y
                    touchX = e.rawX; touchY = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    bubbleLp.x = startX + (e.rawX - touchX).toInt()
                    bubbleLp.y = startY + (e.rawY - touchY).toInt()
                    moved = true
                    wm.updateViewLayout(bubble, bubbleLp)
                }
                MotionEvent.ACTION_UP -> if (!moved) togglePanel()
            }
            true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        instance = null
        wm.removeView(bubble)
        panel?.let { wm.removeView(it) }
        panel = null
        super.onDestroy()
    }

    // Appelé depuis NotifService quand un message arrive.
    fun onNewMessage() {
        ui.post {
            RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play()
            bubble.alert = true
            ui.postDelayed({ bubble.alert = false }, 3000)
            refreshLog()
        }
    }

    private fun togglePanel() {
        val current = panel
        if (current == null) {
            val box = buildPanel()
            val lp = overlayParams(dp(300), dp(460), true).apply {
                x = bubbleLp.x
                y = bubbleLp.y + dp(70)
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
            wm.addView(box, lp)
            panel = box
        } else {
            wm.removeView(current)
            panel = null
            logView = null
            input = null
        }
    }

    private fun buildPanel(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(17, 20, 24))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val title = TextView(this).apply {
            text = "yoo"
            setTextColor(Color.WHITE)
            textSize = 18f
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        logView = list

        val field = EditText(this).apply {
            hint = "Réponse..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        input = field

        val send = Button(this).apply { text = "Envoyer"; setOnClickListener { sendManual() } }

        box.addView(title)
        box.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        box.addView(field)
        box.addView(send)
        refreshLog()
        return box
    }

    private fun refreshLog() {
        val list = logView ?: return
        list.removeAllViews()
        Store.messages.takeLast(50).forEach { m ->
            val tv = TextView(this).apply {
                val suggestion = if (m.reply.isNotBlank()) "\n💬 ${m.reply}" else ""
                val autoTag = if (m.auto) "\n(envoyé automatiquement)" else ""
                text = "${m.app} · ${m.sender}\n${m.text}$suggestion$autoTag"
                setTextColor(Color.WHITE)
                setPadding(0, dp(6), 0, dp(6))
                setOnClickListener {
                    selected = m.sender
                    input?.setText(m.reply)
                }
            }
            list.addView(tv)
        }
    }

    private fun sendManual() {
        val field = input ?: return
        val sender = selected ?: return
        val action = Store.replyActions[sender] ?: return
        val text = field.text.toString().trim()
        if (text.isEmpty()) return
        NotifService.sendReply(this, action, text)
        Store.messages.add(Msg("envoyé", sender, "", text, false))
        field.setText("")
        refreshLog()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("yoo", "yoo", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "yoo")
            .setContentTitle("yoo est actif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, n)
    }

    private fun overlayParams(w: Int, h: Int, focusable: Boolean): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        val flags = if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        return WindowManager.LayoutParams(w, h, type, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
