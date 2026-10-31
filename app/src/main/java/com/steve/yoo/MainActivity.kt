package com.steve.yoo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        val title = TextView(this).apply { text = "yoo"; textSize = 26f }
        val url = EditText(this).apply {
            setText(Store.serverUrl)
            hint = "URL du serveur Gemini"
        }
        val modeBtn = Button(this).apply {
            text = "Mode : ${Store.mode}"
            setOnClickListener {
                Store.mode = if (Store.mode == "auto") "suggestions" else "auto"
                text = "Mode : ${Store.mode}"
            }
        }
        val overlay = Button(this).apply {
            text = "1. Autoriser l'affichage par-dessus"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }
        val notif = Button(this).apply {
            text = "2. Accès aux notifications"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        val contacts = Button(this).apply {
            text = "3. Autoriser les contacts"
            setOnClickListener { requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 1) }
        }
        val start = Button(this).apply {
            text = "Démarrer la bulle"
            setOnClickListener {
                Store.serverUrl = url.text.toString().trim()
                Store.save(this@MainActivity)
                startForegroundService(Intent(this@MainActivity, FloatingService::class.java))
            }
        }

        listOf(title, url, modeBtn, overlay, notif, contacts, start).forEach { root.addView(it) }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
