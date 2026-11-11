package io.github.calcplus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

class GeminiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun solveMathProblem(
        apiKey: String,
        modelId: String,
        problemText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanModel = modelId.trim()

        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("API-ключ не задан. Открой настройки и вставь свой ключ из Google AI Studio.")
            )
        }
        if (cleanModel.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("ID модели пустой. Открой настройки и впиши точный ID модели (например, gemini-2.5-flash-lite).")
            )
        }
        if (problemText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Напиши условие задачи."))
        }

        val prompt = """
            Ты — точный математический помощник в приложении CalcPlus.
            Реши задачу пошагово, кратко и понятно на русском языке.
            В самом конце выдели итоговый ответ строкой "Ответ: ...".
            Задача: $problemText
        """.trimIndent()

        val payload = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )
        val requestBody = json.encodeToString(payload)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", cleanKey)
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                when (response.code) {
                    200 -> {
                        val parsed = json.decodeFromString<GeminiResponse>(rawBody)
                        val answer = parsed.candidates
                            ?.firstOrNull()
                            ?.content
                            ?.parts
                            ?.firstOrNull()
                            ?.text
                        if (answer.isNullOrBlank()) {
                            Result.failure(IllegalStateException("Модель вернула пустой ответ."))
                        } else {
                            Result.success(answer.trim())
                        }
                    }
                    400 -> Result.failure(IllegalStateException("Ошибка 400: Неверный формат запроса или некорректный API-ключ."))
                    403 -> Result.failure(IllegalStateException("Ошибка 403: Доступ запрещён. Проверь правильность API-ключа в настройках."))
                    404 -> Result.failure(IllegalStateException("Ошибка 404: Модель '$cleanModel' не найдена. Проверь точный ID модели в Google AI Studio."))
                    429 -> Result.failure(IllegalStateException("Ошибка 429: Превышен лимит запросов (Rate Limit). Подожди минуту и попробуй снова."))
                    else -> Result.failure(IllegalStateException("Ошибка сервера (${response.code}). Попробуй позже."))
                }
            }
        } catch (e: IOException) {
            Result.failure(IllegalStateException("Нет подключения к сети или сервер недоступен."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Не удалось обработать ответ: ${e.localizedMessage}"))
        }
    }
}