package com.steve.yoo

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifService : NotificationListenerService() {

    private val watched = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = watched[sbn.packageName] ?: return
        val n = sbn.notification
        val sender = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        if (sender.isBlank() || text.isBlank()) return
        if (!Store.isContact(this, sender)) return

        // Action "Répondre" de la notification, nécessaire pour envoyer une réponse.
        val action = n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
        if (action != null) Store.replyActions[sender] = action

        Thread {
            val reply = try {
                Ai.reply(sender, text, Store.mode)
            } catch (e: Exception) {
                ""
            }
            var auto = false
            if (Store.mode == "auto" && reply.isNotBlank() && action != null) {
                auto = sendReply(this, action, reply)
            }
            Store.messages.add(Msg(app, sender, text, reply, auto))
            FloatingService.instance?.onNewMessage()
        }.start()
    }

    companion object {
        fun sendReply(ctx: Context, action: Notification.Action, text: String): Boolean {
            return try {
                val inputs = action.remoteInputs
                val bundle = Bundle()
                inputs.forEach { bundle.putCharSequence(it.resultKey, text) }
                val intent = Intent()
                RemoteInput.addResultsToIntent(inputs, intent, bundle)
                action.actionIntent.send(ctx, 0, intent)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
