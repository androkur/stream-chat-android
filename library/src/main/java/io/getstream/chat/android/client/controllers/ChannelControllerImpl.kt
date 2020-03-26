package io.getstream.chat.android.client.controllers

import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.models.ChannelQueryRequest
import io.getstream.chat.android.client.api.models.ChannelWatchRequest
import io.getstream.chat.android.client.call.Call
import io.getstream.chat.android.client.events.ChatEvent
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.Reaction
import io.getstream.chat.android.client.utils.ProgressCallback
import io.getstream.chat.android.client.utils.observable.ChatObservable
import java.io.File

internal class ChannelControllerImpl(
    val channelType: String,
    val channelId: String,
    val client: ChatClient
) : ChannelController {

    private val cid = "$channelType:$channelId"

    override fun events(): ChatObservable {
        return client.events().filter { event ->
            event.isFrom(cid)
        }
    }

    override fun query(request: ChannelQueryRequest): Call<Channel> {
        return client.queryChannel(channelType, channelId, request)
    }

    override fun watch(request: ChannelWatchRequest): Call<Channel> {
        return client.queryChannel(channelType, channelId, request)
    }

    override fun watch(): Call<Channel> {
        return client.queryChannel(channelType, channelId, ChannelWatchRequest())
    }

    override fun stopWatching(): Call<Unit> {
        return client.stopWatching(channelType, channelId)
    }

    override fun getMessage(messageId: String): Call<Message> {
        return client.getMessage(messageId)
    }

    override fun updateMessage(message: Message): Call<Message> {
        return client.updateMessage(message)
    }

    override fun deleteMessage(messageId: String): Call<Message> {
        return client.deleteMessage(messageId)
    }

    override fun sendMessage(message: Message): Call<Message> {
        return client.sendMessage(channelType, channelId, message)
    }

    override fun banUser(targetId: String, reason: String, timout: Int): Call<Unit> {
        return client.banUser(targetId, channelType, channelId, reason, timout)
    }

    override fun unBanUser(targetId: String, reason: String, timout: Int): Call<Unit> {
        return client.unBanUser(targetId, channelType, channelId)
    }

    override fun markMessageRead(messageId: String): Call<Unit> {
        return client.markMessageRead(channelType, channelId, messageId)
    }

    override fun markRead(): Call<ChatEvent> {
        return client.markAllRead()
    }

    override fun delete(): Call<Channel> {
        return client.deleteChannel(channelType, channelId)
    }

    override fun show(): Call<Unit> {
        return client.showChannel(channelType, channelId)
    }

    override fun hide(clearHistory: Boolean): Call<Unit> {
        return client.hideChannel(channelType, channelId, clearHistory)
    }

    override fun sendFile(file: File): Call<String> {
        return client.sendFile(channelType, channelId, file)
    }

    override fun sendImage(file: File): Call<String> {
        return client.sendImage(channelType, channelId, file)
    }

    override fun sendFile(file: File, callback: ProgressCallback): Call<String> {
        return client.sendFile(channelType, channelId, file)
    }

    override fun sendImage(file: File, callback: ProgressCallback): Call<String> {
        return client.sendImage(channelType, channelId, file)
    }

    override fun sendReaction(reaction: Reaction): Call<Reaction> {
        return client.sendReaction(reaction)
    }

    override fun deleteReaction(messageId: String, reactionType: String): Call<Message> {
        return client.deleteReaction(messageId, reactionType)
    }

    override fun getReactions(messageId: String, offset: Int, limit: Int): Call<List<Reaction>> {
        return client.getReactions(messageId, offset, limit)
    }

    override fun getReactions(messageId: String, firstReactionId: String, limit: Int): Call<List<Message>> {
        return client.getRepliesMore(messageId, firstReactionId, limit)
    }

    override fun update(message: Message, extraData: Map<String, Any>): Call<Channel> {
        return client.updateChannel(channelType, channelId, message, extraData)
    }

    override fun addMembers(vararg userIds: String): Call<Channel> {
        return client.addMembers(channelType, channelId, userIds.toList())
    }

    override fun removeMembers(vararg userIds: String): Call<Channel> {
        return client.removeMembers(channelType, channelId, userIds.toList())
    }
}