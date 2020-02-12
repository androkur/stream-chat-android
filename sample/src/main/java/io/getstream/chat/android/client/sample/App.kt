package io.getstream.chat.android.client.sample

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.facebook.stetho.Stetho
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.RemoteMessage
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.ChatConfig
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.events.ChatEvent
import io.getstream.chat.android.client.logger.ChatLogger
import io.getstream.chat.android.client.logger.ChatLoggerHandler
import io.getstream.chat.android.client.logger.ChatLoggerImpl
import io.getstream.chat.android.client.logger.ChatLoggerLevel
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.notifications.ChatNotificationsManagerImpl
import io.getstream.chat.android.client.notifications.DeviceRegisteredListener
import io.getstream.chat.android.client.notifications.ChatNotificationConfig
import io.getstream.chat.android.client.notifications.NotificationMessageLoadListener
import io.getstream.chat.android.client.notifications.options.ChatNotificationOptions
import io.getstream.chat.android.client.notifications.options.NotificationIntentProvider
import io.getstream.chat.android.client.sample.cache.AppDatabase
import io.getstream.chat.android.client.sample.common.HomeActivity
import io.getstream.chat.android.client.sample.common.KeyValue
import io.getstream.chat.android.client.sample.repositories.ChannelsRepositoryLive
import io.getstream.chat.android.client.sample.repositories.ChannelsRepositoryRx
import io.getstream.chat.android.client.sample.repositories.ChannelsRepositorySync
import kotlin.time.ExperimentalTime

class App : Application() {

    companion object {
        lateinit var client: ChatClient
        lateinit var channelsRepositorySync: ChannelsRepositorySync
        lateinit var channelsRepositoryRx: ChannelsRepositoryRx
        lateinit var channelsRepositoryLive: ChannelsRepositoryLive
        lateinit var db: AppDatabase
        lateinit var cache: ChannelsCache
        lateinit var keyValue: KeyValue
        lateinit var logger: ChatLogger

        private const val EXTRA_CHANNEL_TYPE = "io.getstream.chat.example.CHANNEL_TYPE"
        private const val EXTRA_CHANNEL_ID = "io.getstream.chat.example.CHANNEL_ID"
    }

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)

        FirebaseApp.initializeApp(this)

        logger = provideLogger()
        db = AppDatabase.getInstance(this)

        val config = ChatConfig.Builder()
            .apiKey("qk4nn7rpcn75")
            .baseUrl("chat-us-east-staging.stream-io-api.com")
            .cdnUrl("chat-us-east-staging.stream-io-api.com")
            .baseTimeout(10000)
            .cdnTimeout(10000)
            .token("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoiYmVuZGVyIn0.3KYJIoYvSPgTURznP8nWvsA2Yj2-vLqrm-ubqAeOlcQ")
            .build()

        val notificationConfig = ChatNotificationConfig.Builder()
            .options(provideNotificationOptions())
            .registerListener(provideDeviceRegisteredListener())
            .messageLoadListener(provideNotificationMessageLoadListener())
            .build()

        client = ChatClient.init(
            ChatClient.Builder()
                .config(config)
                .logger(logger)
                .notification(notificationConfig)
        )

        keyValue = KeyValue(this)
        cache = ChannelsCache(db.channels())
        channelsRepositorySync = ChannelsRepositorySync(client, cache)
        channelsRepositoryRx = ChannelsRepositoryRx(client, cache)
        channelsRepositoryLive = ChannelsRepositoryLive(client, cache)
    }

    private fun provideLogger(): ChatLogger {
        val loggerHandler: ChatLoggerHandler = object : ChatLoggerHandler {
            override fun logT(throwable: Throwable) {
                // display throwable logs here
            }

            override fun logT(className: String, throwable: Throwable) {
                // display throwable logs here
            }

            override fun logI(className: String, message: String) {
                // display info logs here
            }

            override fun logD(className: String, message: String) {
                // display debug logs here
            }

            override fun logW(className: String, message: String) {
                // display warning logs here
            }

            override fun logE(className: String, message: String) {
                // display error logs here
            }
        }

        return ChatLoggerImpl.Builder()
            .level(if (BuildConfig.DEBUG) ChatLoggerLevel.ALL else ChatLoggerLevel.NOTHING)
            .handler(loggerHandler)
            .build()
    }


    @UseExperimental(ExperimentalTime::class)
    private fun provideNotificationOptions() = ChatNotificationOptions().apply {
        setNotificationIntentProvider(
            object : NotificationIntentProvider {
                override fun getIntentForFirebaseMessage(
                    context: Context,
                    remoteMessage: RemoteMessage
                ): PendingIntent {
                    val payload = remoteMessage.data
                    val intent = Intent(context, HomeActivity::class.java)
                    intent.apply {
                        putExtra(
                            EXTRA_CHANNEL_TYPE,
                            payload[ChatNotificationsManagerImpl.CHANNEL_TYPE_KEY]
                        )
                        putExtra(
                            EXTRA_CHANNEL_ID,
                            payload[ChatNotificationsManagerImpl.CHANNEL_ID_KEY]
                        )
                    }
                    return PendingIntent.getActivity(
                        context, 999,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                override fun getIntentForWebSocketEvent(
                    context: Context,
                    event: ChatEvent
                ): PendingIntent {
                    val intent = Intent(context, HomeActivity::class.java)
                    intent.apply {
                        putExtra(EXTRA_CHANNEL_TYPE, event.message.type)
                        putExtra(EXTRA_CHANNEL_ID, event.message.id)
                    }
                    return PendingIntent.getActivity(
                        context, 999,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            }
        )
    }

    private fun provideDeviceRegisteredListener() = object : DeviceRegisteredListener {
        override fun onDeviceRegisteredSuccess() { // Device successfully registered on server
            logger.logI(this, "Device registered successfully")
        }

        override fun onDeviceRegisteredError(error: ChatError) {
            logger.logE(this, "onDeviceRegisteredError: ${error.message}")
        }
    }

    private fun provideNotificationMessageLoadListener() =
        object : NotificationMessageLoadListener {
            override fun onLoadMessageSuccess(message: Message) {
                logger.logD(this, "On message loaded. Message:$message")
            }

            override fun onLoadMessageFail(messageId: String) {
                logger.logD(this, "Message from notification load fails. MessageId:$messageId")
            }
        }
}