package com.example.aiwebsummarizer.data.api

import com.example.aiwebsummarizer.data.model.LLMChoice
import com.example.aiwebsummarizer.data.model.LLMRequest
import com.example.aiwebsummarizer.data.model.LLMResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Service to interact with language model APIs for generating text-based responses.
 */
class LLMApiService(private val client: OkHttpClient) {

    companion object {
        // Using Hugging Face Inference API as fallback
        private const val HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn"
        private const val DEFAULT_MODEL = "bart-large-cnn"
    }

    /**
     * Generate an answer based on a question and contextual information.
     *
     * @param query The user's question
     * @param context Information gathered from web searches to provide context
     * @param apiKey API key for the LLM service
     * @return The generated answer
     */
    suspend fun generateAnswer(query: String, context: String, apiKey: String): String = withContext(Dispatchers.IO) {
        try {
            // Construct a prompt that uses the context to answer the question
            val prompt = buildPrompt(query, context)

            // Create the request body
            val requestBody = buildRequestBody(prompt)

            // Create the request
            val request = Request.Builder()
                .url(HUGGING_FACE_API_URL)
                .post(requestBody)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .build()

            // Execute the request
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response body")

            parseResponse(responseBody)
        } catch (e: Exception) {
            throw Exception("Failed to generate answer: ${e.message}")
        }
    }

    /**
     * Build a prompt for the LLM using the question and context.
     */
    private fun buildPrompt(query: String, context: String): String {
        return """
            
            Question: $query
            
            Context:
            $context
            
            Answer:
        """.trimIndent()
    }

    /**
     * Build the JSON request body for the API call.
     */
    private fun buildRequestBody(prompt: String): okhttp3.RequestBody {
        val jsonObject = JSONObject().apply {
            put("inputs", prompt)
            put("parameters", JSONObject().apply {
                put("max_length", 150)
                put("min_length", 50)
                put("do_sample", false)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        return jsonObject.toString().toRequestBody(mediaType)
    }

    /**
     * Parse the response from the LLM API.
     */
    private fun parseResponse(responseBody: String): String {
        try {
            // For Hugging Face API format
            val jsonArray = org.json.JSONArray(responseBody)
            if (jsonArray.length() > 0) {
                val firstResult = jsonArray.getJSONObject(0)
                if (firstResult.has("summary_text")) {
                    return firstResult.getString("summary_text")
                }
            }

            throw Exception("Could not parse response")
        } catch (e: Exception) {
            throw Exception("Failed to parse LLM response: ${e.message}")
        }
    }
}