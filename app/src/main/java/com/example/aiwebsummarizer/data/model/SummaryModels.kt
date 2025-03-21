package com.example.aiwebsummarizer.data.model

data class Summary(
    val id: String = "",
    val url: String = "",
    val originalText: String = "",
    val summarizedText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

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