package com.example.aiwebsummarizer.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a user query and the generated response.
 */
data class Query(
    @DocumentId val id: String = "",
    val question: String = "",
    val answer: String = "",
    val sources: List<Source> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a source used to generate an answer.
 */
data class Source(
    val url: String = "",
    val title: String = "",
    val snippet: String = ""
)

/**
 * Request model for sending data to an LLM API.
 */
data class LLMRequest(
    val model: String,
    val prompt: String,
    val max_tokens: Int = 500,
    val temperature: Double = 0.5,
    val top_p: Double = 1.0
)

/**
 * Response model for receiving data from an LLM API.
 */
data class LLMResponse(
    val id: String,
    val model: String,
    val choices: List<LLMChoice>
)

/**
 * Represents an individual choice from the LLM API response.
 */
data class LLMChoice(
    val text: String,
    val index: Int,
    val finish_reason: String
)

/**
 * Encapsulates the search query, retrieved information, and generated answer.
 */
data class QueryResult(
    val query: String,
    val searchResults: List<SearchResult>,
    val answer: String,
    val sources: List<Source>
)

/**
 * Represents a single search result with content.
 */
data class SearchResult(
    val url: String,
    val title: String,
    val content: String,
    val relevance: Double
)