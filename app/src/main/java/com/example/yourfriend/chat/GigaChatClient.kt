package com.example.yourfriend.chat

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.yourfriend.BuildConfig
import com.example.yourfriend.data.ChatMessage
import com.example.yourfriend.data.FriendProfile
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class GigaChatException(message: String, cause: Throwable? = null) : Exception(message, cause)

class GigaChatClient(context: Context) : AutoCloseable {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "gigachat-network").apply { isDaemon = true }
    }

    private val authKey = BuildConfig.GIGACHAT_AUTH_KEY
    private val scope = BuildConfig.GIGACHAT_SCOPE
    private val model = BuildConfig.GIGACHAT_MODEL

    private val httpClient: OkHttpClient

    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var accessTokenExpiresAt: Long = 0L

    init {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        httpClient = try {
            val (socketFactory, trustManager) = GigaChatTls.create(appContext)
            builder.sslSocketFactory(socketFactory, trustManager).build()
        } catch (error: Throwable) {
            Log.e(TAG, "Не удалось загрузить сертификаты Минцифры", error)
            builder.build()
        }
    }

    fun isConfigured(): Boolean = authKey.isNotBlank()

    fun reply(
        profile: FriendProfile,
        history: List<ChatMessage>,
        callback: (Result<String>) -> Unit
    ) {
        if (!isConfigured()) {
            callback(
                Result.failure(
                    GigaChatException("GigaChat не настроен: добавь ключ в gigachat.properties.")
                )
            )
            return
        }

        val turns = GigaChatPrompt.build(profile, history)
        executor.execute {
            val result = try {
                Result.success(requestReply(turns))
            } catch (error: Throwable) {
                Log.e(TAG, "Не удалось получить ответ", error)
                Result.failure(toFriendlyError(error))
            }
            mainHandler.post { callback(result) }
        }
    }

    private fun requestReply(turns: List<GigaChatTurn>): String {
        val token = getToken()
        return try {
            requestCompletion(token, turns)
        } catch (unauthorized: UnauthorizedException) {
            accessToken = null
            requestCompletion(getToken(forceRefresh = true), turns)
        }
    }

    private fun getToken(forceRefresh: Boolean = false): String {
        val cached = accessToken
        if (!forceRefresh && cached != null &&
            System.currentTimeMillis() < accessTokenExpiresAt - TOKEN_REFRESH_MARGIN_MS
        ) {
            return cached
        }

        val form = FormBody.Builder()
            .add("scope", scope)
            .build()
        val request = Request.Builder()
            .url(OAUTH_URL)
            .header("Accept", "application/json")
            .header("RqUID", UUID.randomUUID().toString())
            .header("Authorization", "Basic $authKey")
            .post(form)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    401, 403 -> "GigaChat не принял ключ авторизации. Возможно, он отозван — обнови ключ в gigachat.properties."
                    429 -> "Слишком много запросов к GigaChat. Подожди немного и попробуй снова."
                    in 500..599 -> "GigaChat временно недоступен. Попробуй позже."
                    else -> "GigaChat не принял ключ авторизации (${response.code})."
                }
                throw GigaChatException(message)
            }
            val json = JSONObject(body)
            val token = json.optString("access_token")
            if (token.isBlank()) {
                throw GigaChatException("GigaChat не вернул токен доступа.")
            }
            val expiresAt = json.optLong("expires_at", 0L)
            accessToken = token
            accessTokenExpiresAt = if (expiresAt > 0) {
                expiresAt
            } else {
                System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MS
            }
            return token
        }
    }

    private fun requestCompletion(token: String, turns: List<GigaChatTurn>): String {
        val messages = JSONArray()
        turns.forEach { turn ->
            messages.put(
                JSONObject()
                    .put("role", turn.role)
                    .put("content", turn.content)
            )
        }
        val payload = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.82)
            .put("top_p", 0.9)
            .put("max_tokens", 240)
            .put("stream", false)

        val request = Request.Builder()
            .url(CHAT_URL)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.code == 401) {
                throw UnauthorizedException()
            }
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    402 -> "Закончились токены GigaChat. Пополни баланс в кабинете или смени модель в gigachat.properties."
                    403 -> "GigaChat запретил запрос. Проверь ключ и тариф."
                    429 -> "Слишком много запросов к GigaChat. Подожди немного и попробуй снова."
                    in 500..599 -> "GigaChat временно недоступен. Попробуй позже."
                    else -> "GigaChat ответил ошибкой ${response.code}. Попробуй ещё раз."
                }
                throw GigaChatException(message)
            }
            val content = JSONObject(body)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            if (content.isBlank()) {
                throw GigaChatException("GigaChat вернул пустой ответ.")
            }
            return content
        }
    }

    fun cancelPending() {
        httpClient.dispatcher.cancelAll()
    }

    override fun close() {
        cancelPending()
        executor.shutdownNow()
        httpClient.connectionPool.evictAll()
    }

    private fun toFriendlyError(error: Throwable): Throwable {
        return when (error) {
            is GigaChatException -> error
            is UnauthorizedException -> GigaChatException(
                "Ключ GigaChat не принят. Возможно, он отозван — обнови gigachat.properties.",
                error
            )
            is SSLException -> GigaChatException(
                "Не удалось установить защищённое соединение с GigaChat.",
                error
            )
            is IOException -> GigaChatException(
                "Нет связи с GigaChat. Проверь интернет.",
                error
            )
            else -> GigaChatException(
                "Не получилось получить ответ. Попробуй ещё раз.",
                error
            )
        }
    }

    private class UnauthorizedException : Exception()

    private companion object {
        const val TAG = "GigaChat"
        const val OAUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
        const val CHAT_URL = "https://api.giga.chat/v1/chat/completions"
        const val TOKEN_REFRESH_MARGIN_MS = 60_000L
        const val DEFAULT_TOKEN_LIFETIME_MS = 25 * 60_000L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
