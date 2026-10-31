package com.steve.yoo

import android.app.Notification
import android.content.Context
import android.provider.ContactsContract

data class Msg(val app: String, val sender: String, val text: String, val reply: String, val auto: Boolean)

object Store {
    var serverUrl = "http://127.0.0.1:3000/reply"
    var mode = "suggestions" // "suggestions" ou "auto"
    val messages = mutableListOf<Msg>()
    val replyActions = mutableMapOf<String, Notification.Action>()

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("yoo", Context.MODE_PRIVATE)
        serverUrl = p.getString("url", serverUrl) ?: serverUrl
        mode = p.getString("mode", mode) ?: mode
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("yoo", Context.MODE_PRIVATE).edit()
            .putString("url", serverUrl)
            .putString("mode", mode)
            .apply()
    }

    // Sans permission de contacts, on affiche tout.
    fun isContact(ctx: Context, name: String): Boolean {
        return try {
            ctx.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
                arrayOf(name), null
            )?.use { it.moveToFirst() } ?: false
        } catch (e: SecurityException) {
            true
        }
    }
}
