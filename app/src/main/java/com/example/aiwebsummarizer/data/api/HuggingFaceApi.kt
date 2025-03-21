package com.example.aiwebsummarizer.data.api

import com.example.aiwebsummarizer.data.model.SummaryRequest
import com.example.aiwebsummarizer.data.model.SummaryResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface HuggingFaceApi {
    @POST("models/facebook/bart-large-cnn")
    suspend fun getSummary(
        @Header("Authorization") apiKey: String,
        @Body request: SummaryRequest
    ): List<SummaryResponse>
}

data class SummaryRequest(
    val inputs: String,
    val parameters: Parameters = Parameters()
)

data class Parameters(
    val max_length: Int = 150,
    val min_length: Int = 30,
    val do_sample: Boolean = false
)

data class SummaryResponse(
    val summary_text: String
)