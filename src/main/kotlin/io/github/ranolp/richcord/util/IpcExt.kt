package io.github.ranolp.richcord.util

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.entities.Callback
import com.jagrosh.discordipc.entities.RichPresence

var applicationId: Long = -1
lateinit var client: IPCClient
fun isClientInitialized(): Boolean = ::client.isInitialized

fun sendRichPresence(richPresence: RichPresence, success: () -> Unit = {}, failure: (String) -> Unit = {}) {
    if (isClientInitialized()) {
        client.close()
    }
    client = IPCClient(applicationId)
    client.connect()
    client.sendRichPresence(richPresence, Callback(success, failure))
}
