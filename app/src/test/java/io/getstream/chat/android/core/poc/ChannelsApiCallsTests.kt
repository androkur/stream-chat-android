package io.getstream.chat.android.core.poc

import io.getstream.chat.android.core.poc.library.*
import io.getstream.chat.android.core.poc.library.errors.ChatHttpError
import io.getstream.chat.android.core.poc.library.rest.ChannelQueryRequest
import io.getstream.chat.android.core.poc.library.rest.ChannelResponse
import io.getstream.chat.android.core.poc.library.rest.HideChannelRequest
import io.getstream.chat.android.core.poc.library.rest.UpdateChannelRequest
import io.getstream.chat.android.core.poc.library.socket.ChatSocket
import io.getstream.chat.android.core.poc.library.socket.ConnectionData
import io.getstream.chat.android.core.poc.utils.RetroError
import io.getstream.chat.android.core.poc.utils.RetroSuccess
import io.getstream.chat.android.core.poc.utils.SuccessCall
import io.getstream.chat.android.core.poc.utils.SuccessTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ChannelsApiCallsTests {

    val user = User("test-id")
    val connection = ConnectionData("connection-id", user)
    val tokenProvider = SuccessTokenProvider()

    val channelType = "test-type"
    val channelId = "test-id"

    lateinit var api: ChatApi
    lateinit var socket: ChatSocket
    lateinit var client: ChatClient
    lateinit var retrofitApi: RetrofitApi

    val apiKey = "api-key"
    val serverErrorCode = 500

    @Before
    fun before() {
        api = Mockito.mock(ChatApi::class.java)
        socket = Mockito.mock(ChatSocket::class.java)
        retrofitApi = Mockito.mock(RetrofitApi::class.java)
        client = ChatClientImpl(ChatApiImpl(apiKey, retrofitApi), socket)

        Mockito.`when`(socket.connect(user, tokenProvider)).thenReturn(SuccessCall(connection))

        client.setUser(user, tokenProvider) {}
    }

    @Test
    fun queryChannelSuccess() {

        val response = Channel()

        Mockito.`when`(
            retrofitApi.queryChannel(
                channelType,
                channelId,
                apiKey,
                user.id,
                connection.connectionId,
                ChannelQueryRequest()
            )
        ).thenReturn(RetroSuccess(ChannelState().apply { channel = response }))

        val result = client.queryChannel(channelType, channelId, ChannelQueryRequest()).execute()

        verifySuccess(result, response)
    }

    @Test
    fun queryChannelError() {

        Mockito.`when`(
            retrofitApi.queryChannel(
                channelType,
                channelId,
                apiKey,
                user.id,
                connection.connectionId,
                ChannelQueryRequest()
            )
        ).thenReturn(RetroError(serverErrorCode))

        val result = client.queryChannel(channelType, channelId, ChannelQueryRequest()).execute()

        verifyError(result, serverErrorCode)
    }

    @Test
    fun showChannelSuccess() {

        Mockito.`when`(
            retrofitApi.showChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId,
                emptyMap()
            )
        ).thenReturn(RetroSuccess(CompletableResponse()))

        val result = client.showChannel(channelType, channelId).execute()

        verifySuccess(result, Unit)
    }

    @Test
    fun showChannelError() {

        Mockito.`when`(
            retrofitApi.showChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId,
                emptyMap()
            )
        ).thenReturn(RetroError(serverErrorCode))

        val result = client.showChannel(channelType, channelId).execute()

        verifyError(result, serverErrorCode)
    }

    @Test
    fun deleteChannelSuccess() {

        val response = Channel()

        Mockito.`when`(
            retrofitApi.deleteChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId
            )
        ).thenReturn(RetroSuccess(ChannelResponse().apply { channel = response }))


        val result = client.deleteChannel(channelType, channelId).execute()

        verifySuccess(result, response)
    }

    @Test
    fun deleteChannelError() {

        Mockito.`when`(
            retrofitApi.deleteChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId
            )
        ).thenReturn(RetroError(serverErrorCode))


        val result = client.deleteChannel(channelType, channelId).execute()

        verifyError(result, serverErrorCode)
    }

    @Test
    fun hideChannelSuccess() {

        Mockito.`when`(
            retrofitApi.hideChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId,
                HideChannelRequest()
            )
        ).thenReturn(RetroSuccess(CompletableResponse()))

        val result = client.hideChannel(channelType, channelId).execute()

        verifySuccess(result, Unit)
    }

    @Test
    fun hideChannelError() {

        Mockito.`when`(
            retrofitApi.hideChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId,
                HideChannelRequest()
            )
        ).thenReturn(RetroError(serverErrorCode))

        val result = client.hideChannel(channelType, channelId).execute()

        verifyError(result, serverErrorCode)
    }

    @Test
    fun updateChannelSuccess() {

        val updateMessage = Message().apply { text = "update-message" }
        val updateChannelData = mapOf<String, Any>()
        val responseChannel = Channel()

        Mockito.`when`(
            retrofitApi.updateChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId,
                UpdateChannelRequest(updateChannelData, updateMessage)
            )
        ).thenReturn(RetroSuccess(ChannelResponse().apply { channel = responseChannel }))

        val result =
            client.updateChannel(channelType, channelId, updateMessage, updateChannelData).execute()

        verifySuccess(result, responseChannel)
    }

    @Test
    fun updateChannelError() {

        val updateMessage = Message().apply { text = "update-message" }
        val updateChannelData = mapOf<String, Any>()

        Mockito.`when`(
            retrofitApi.updateChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId,
                UpdateChannelRequest(updateChannelData, updateMessage)
            )
        ).thenReturn(RetroError(serverErrorCode))

        val result =
            client.updateChannel(channelType, channelId, updateMessage, updateChannelData).execute()

        verifyError(result, serverErrorCode)
    }

    private fun <T> verifyError(result: Result<T>, statusCode: Int) {
        assertThat(result.isSuccess).isFalse()
        assertThat(result.error()).isInstanceOf(ChatHttpError::class.java)

        val error = result.error() as ChatHttpError
        assertThat(error.statusCode).isEqualTo(statusCode)
    }

    private fun <T> verifySuccess(result: Result<T>, equalsTo: T) {
        assertThat(result.isSuccess).isTrue()
        assertThat(result.data()).isEqualTo(equalsTo)
    }


}
