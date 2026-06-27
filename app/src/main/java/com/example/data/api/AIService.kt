package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

object AIService {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L
    private val minIntervalMs = 2000L

    private suspend fun acquireRateLimit() {
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < minIntervalMs) {
                kotlinx.coroutines.delay(minIntervalMs - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }

    suspend fun generateResponse(
        prompt: String,
        systemInstruction: String? = null,
        isJsonMode: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val proxyUrl = BuildConfig.PROXY_URL
        if (proxyUrl.isBlank()) {
            return@withContext "Error: Proxy URL no configurada."
        }

        try {
            acquireRateLimit()

            val url = "$proxyUrl/api/chat"

            val systemMsg = systemInstruction ?: "Regla estricta de sistema: NUNCA des respuestas largas. Mantén siempre todas tus respuestas breves, de 1 o 2 párrafos máximo. Ve directo al grano."

            val finalPrompt = if (isJsonMode) {
                "$prompt\n\nRESPOND ONLY WITH VALID JSON."
            } else {
                prompt
            }

            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", systemMsg))
            messages.put(JSONObject().put("role", "user").put("content", finalPrompt))

            val jsonBody = JSONObject()
            jsonBody.put("model", "meta/llama-3.1-70b-instruct")
            jsonBody.put("messages", messages)
            jsonBody.put("temperature", 0.2)
            jsonBody.put("top_p", 0.7)
            jsonBody.put("max_tokens", 1024)
            jsonBody.put("stream", false)

            if (isJsonMode) {
                val format = JSONObject().put("type", "json_object")
                jsonBody.put("response_format", format)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    return@withContext "Error: Demasiadas peticiones. Espera un momento e intenta de nuevo."
                }
                if (!response.isSuccessful) {
                    return@withContext "Error de API: ${response.code} ${response.message}"
                }

                val responseData = response.body?.string() ?: return@withContext "Respuesta vacía"
                val jsonResponse = JSONObject(responseData)

                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    return@withContext message?.optString("content") ?: "Sin contenido"
                }

                return@withContext "Estructura de respuesta inesperada"
            }
        } catch (e: Throwable) {
            val msg = e.localizedMessage ?: "Error desconocido"
            return@withContext "Error de red o procesamiento: $msg"
        }
    }

    data class MealAnalysisResult(
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val foods: List<FoodAnalysisResult>
    ) : Serializable

    data class FoodAnalysisResult(
        val name: String,
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    ) : Serializable

    suspend fun analyzeMeal(
        description: String
    ): MealAnalysisResult? = withContext(Dispatchers.IO) {
        val prompt = SystemPromptProvider.getMealAnalysisPrompt(description)

        val jsonResponse = generateResponse(
            prompt = prompt,
            isJsonMode = true
        )

        if (jsonResponse.startsWith("Error")) {
            return@withContext null
        }

        try {
            val analysisAdapter = moshi.adapter(MealAnalysisResult::class.java)
            var cleanJson = jsonResponse.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.removePrefix("```json")
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.removeSuffix("```")
            }
            cleanJson = cleanJson.trim()

            return@withContext analysisAdapter.fromJson(cleanJson)
        } catch (e: Throwable) {
            return@withContext null
        }
    }
}

object SystemPromptProvider {
    fun getMealAnalysisPrompt(description: String): String {
        return """
            Analiza la descripción de esta comida en español: "$description"
            Extrae las calorías totales (kcal), proteínas totales (g), carbohidratos totales (g), grasas totales (g).
            Además, divide la comida en sus alimentos individuales. Si el usuario reporta múltiples unidades del mismo alimento (ej. "3 panes"), agrupalos en UN SOLO elemento en el arreglo (ej. "name": "3 panes") con las calorías totales de esos 3 panes. NO crees elementos separados para el mismo alimento repetido. NUNCA devuelvas el arreglo 'foods' vacío.
            
            Devuelve OBLIGATORIAMENTE un JSON válido con el siguiente formato exacto. No omitas ningún campo:
            {
              "calories": 350,
              "protein": 24.5,
              "carbs": 30.0,
              "fat": 12.0,
              "foods": [
                {
                  "name": "3 huevos fritos",
                  "calories": 210,
                  "protein": 18.0,
                  "carbs": 1.5,
                  "fat": 15.0
                }
              ]
            }
            Asegúrate de que los valores de los macronutrientes sean números (pueden tener decimales usando punto, NUNCA coma). Si no sabes el valor exacto, usa una estimación realista. NUNCA devuelvas 0 en las macros a menos que realmente sea agua.
        """.trimIndent()
    }
}
