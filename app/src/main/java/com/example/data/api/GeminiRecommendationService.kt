package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class ResponseFormatSchema(
    val type: String,
    val description: String? = null,
    val properties: Map<String, Any>? = null,
    val required: List<String>? = null,
    val items: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String,
    val schema: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: Map<String, Any>? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

@JsonClass(generateAdapter = true)
data class RecommendedSong(
    val title: String,
    val artist: String,
    val vibeDescription: String,
    val genre: String // e.g. "lofi", "synthwave", "acoustic", "classical"
)

object GeminiRecommendationService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getRecommendations(userVibePrompt: String): List<RecommendedSong> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder value.")
            throw IllegalStateException("API Key is missing. Please set your GEMINI_API_KEY in the Secrets Panel in AI Studio.")
        }

        val prompt = """
            You are SoundSpot's AI Music DJ. Based on the user's current mood or activity: "$userVibePrompt", 
            recommend exactly 5 interesting music tracks that perfectly fit this vibe.
            For each recommended track, provide the song's title, artist, a short vibeDescription explaining why this matches their mood, and a corresponding genre (strictly return one of these: "lofi", "synthwave", "acoustic", "classical").
            Make sure the artist names or song concepts feel creative and fitting.
        """.trimIndent()

        // JSON response schema definition using Map for simplicity and robustness
        val songSchema = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "title" to mapOf("type" to "STRING", "description" to "The name of the song"),
                "artist" to mapOf("type" to "STRING", "description" to "The name of the artist/band"),
                "vibeDescription" to mapOf("type" to "STRING", "description" to "Short, atmospheric sentence describing why this song matches the user mood"),
                "genre" to mapOf("type" to "STRING", "description" to "Must be one of the genres: lofi, synthwave, acoustic, classical")
            ),
            "required" to listOf("title", "artist", "vibeDescription", "genre")
        )

        val rootSchema = mapOf(
            "type" to "ARRAY",
            "items" to songSchema,
            "description" to "A list of recommended tracks matching the prompt"
        )

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "You are a professional music curator. You output high-quality custom music recommendations matching specific user input atmospheres."))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                responseSchema = rootSchema,
                temperature = 1.0f
            )
        )

        val requestAdapter = moshi.adapter(GeminiRequest::class.java)
        val responseAdapter = moshi.adapter(GeminiResponse::class.java)
        val songsListAdapter = moshi.adapter<List<RecommendedSong>>(
            Types.newParameterizedType(List::class.java, RecommendedSong::class.java)
        )

        val requestJson = requestAdapter.toJson(request)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toRequestBody(mediaType)

        val urlWithKey = "$BASE_URL?key=$apiKey"
        val httpRequest = Request.Builder()
            .url(urlWithKey)
            .post(requestBody)
            .build()

        try {
            okHttpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API failure: $errorBody")
                    throw Exception("API Error Code ${response.code}: $errorBody")
                }

                val responseJson = response.body?.string() ?: throw Exception("Empty response body")
                val geminiResponse = responseAdapter.fromJson(responseJson)
                val generatedText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No candidates received or unable to parse response.")

                songsListAdapter.fromJson(generatedText) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error matching vibes: ", e)
            throw e
        }
    }
}
