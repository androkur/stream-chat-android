package io.getstream.chat.android.core.poc.library.events

import com.google.gson.annotations.SerializedName
import io.getstream.chat.android.core.poc.library.*

class ConnectionEvent : RemoteEvent() {

    val cid: String = ""
    val user: User? = null
    lateinit var me: User
    val member: Member? = null
    var message: Message? = null
    val reaction: Reaction? = null
    val channel: Channel? = null
    var online = false

    @SerializedName("connection_id")
    var connectionId: String = ""
    @SerializedName("client_id")
    var clientId: String = ""
    @SerializedName("total_unread_count")
    val totalUnreadCount: Number = 0
    @SerializedName("unread_channels")
    val unreadChannels: Number = 0
    @SerializedName("watcher_count")
    val watcherCount: Number = 0
    @SerializedName("clear_history")
    var clearHistory: Boolean = false

    val isChannelEvent: Boolean
        get() = cid != "*"


    val isAnonymous: Boolean
        get() = if (me != null) {
            me.id == "!anon"
        } else true
}