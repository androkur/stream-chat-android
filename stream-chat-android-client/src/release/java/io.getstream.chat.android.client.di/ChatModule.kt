package io.getstream.chat.android.client.di

import android.content.Context
import io.getstream.chat.android.client.api.ChatClientConfig
import io.getstream.chat.android.client.notifications.handler.ChatNotificationHandler
import io.getstream.chat.android.client.token.TokenManager
import io.getstream.chat.android.client.uploader.FileUploader

/**
 * Release variant of [BaseChatModule].
 */
internal class ChatModule(
    appContext: Context,
    config: ChatClientConfig,
    notificationsHandler: ChatNotificationHandler,
    uploader: FileUploader?,
    tokenManager: TokenManager,
) : BaseChatModule(appContext, config, notificationsHandler, uploader, tokenManager)
