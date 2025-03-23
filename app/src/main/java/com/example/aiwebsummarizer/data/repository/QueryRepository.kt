package com.example.aiwebsummarizer.data.repository

import android.util.Log
import com.example.aiwebsummarizer.data.api.LLMApiService
import com.example.aiwebsummarizer.data.api.SearchApiService
import com.example.aiwebsummarizer.data.api.SerpApiService
import com.example.aiwebsummarizer.data.model.Query
import com.example.aiwebsummarizer.data.model.QueryResult
import com.example.aiwebsummarizer.data.model.SearchResult
import com.example.aiwebsummarizer.data.model.Source
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import io.github.cdimascio.dotenv.Dotenv


/**
 * Repository to handle query processing, including search, answer generation, and storage.
 */
class QueryRepository(
    private val searchApiService: SearchApiService,
    private val serpApiService: SerpApiService,
    private val llmApiService: LLMApiService,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "QueryRepository"
        private const val MAX_CONTENT_LENGTH = 1500
        private const val USE_SERP_API = true // Set to true to use SerpAPI

        // Replace with your actual SerpAPI key
        private val dotenv: Dotenv = Dotenv.configure()
            .directory("/assets")
            .filename("env")
            .load()

        private val SERP_API_KEY: String by lazy {
            dotenv["SERP_API_KEY"] ?: throw IllegalArgumentException("SERP_API_KEY not found in env file")
        }

    }

    /**
     * Process a user query, search for information, generate an answer, and save to Firestore.
     *
     * @param queryText The user's question
     * @param apiKey API key for the LLM service
     * @return A QueryResult containing the answer and sources
     */
    suspend fun processQuery(queryText: String, apiKey: String): QueryResult {
        try {
            // Step 1: Search for relevant information
            Log.d(TAG, "Searching for information about: $queryText")

            val searchResults = if (USE_SERP_API) {
                // Use SerpAPI for search
                serpApiService.searchForQuery(queryText, SERP_API_KEY)
            } else {
                // Use web scraper for search
                searchApiService.searchForQuery(queryText)
            }

            if (searchResults.isEmpty()) {
                Log.e(TAG, "No search results found for query: $queryText")
                throw Exception("No search results found. Please try a different question.")
            }

            // Step 2: Prepare context for the LLM
            val context = buildContext(searchResults)

            // Step 3: Generate an answer using the LLM
            Log.d(TAG, "Generating answer with context length: ${context.length}")
            val answer = try {
                llmApiService.generateAnswer(queryText, context, apiKey)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating answer: ${e.message}")
                // Fallback answer if LLM fails
                "Based on the search results, I found some information that might help answer your question about ${queryText}. " +
                        "However, I couldn't generate a complete answer. Please check the sources below for more details."
            }

            // Step 4: Extract sources for attribution
            val sources = searchResults.map { result ->
                Source(
                    url = result.url,
                    title = result.title,
                    snippet = result.content.take(200) + "..." // Short preview
                )
            }

            // Step 5: Try to save the query and response to Firestore
            val queryObject = try {
                saveQueryToFirestore(queryText, answer, sources)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to Firestore: ${e.message}")
                // Create a local Query object even if Firestore fails
                Query(
                    id = "",
                    question = queryText,
                    answer = answer,
                    sources = sources,
                    timestamp = System.currentTimeMillis()
                )
            }

            // Return the query result
            return QueryResult(
                query = queryText,
                searchResults = searchResults,
                answer = answer,
                sources = sources
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing query: ${e.message}")
            throw Exception("Failed to process query: ${e.message}")
        }
    }

    /**
     * Build context for the LLM by combining search results.
     */
    private fun buildContext(searchResults: List<SearchResult>): String {
        val contextBuilder = StringBuilder()

        // Add each search result as a separate section in the context
        searchResults.forEachIndexed { index, result ->
            contextBuilder.append("Source ${index + 1} (${result.title} - ${result.url}):\n")

            // Limit the content length per source
            val contentPreview = result.content.take(MAX_CONTENT_LENGTH / searchResults.size)
            contextBuilder.append(contentPreview)
            contextBuilder.append("\n\n")
        }

        return contextBuilder.toString()
    }

    /**
     * Save the query and response to Firestore.
     */
    private suspend fun saveQueryToFirestore(
        questionText: String,
        answerText: String,
        sources: List<Source>
    ): Query {
        auth.currentUser?.let { user ->
            // Create a new query document
            val queryDocRef = firestore.collection("users")
                .document(user.uid)
                .collection("queries")
                .document()

            // Create the query object
            val query = Query(
                id = queryDocRef.id,
                question = questionText,
                answer = answerText,
                sources = sources,
                timestamp = System.currentTimeMillis()
            )

            // Save to Firestore
            queryDocRef.set(query).await()

            return query
        } ?: throw Exception("User not authenticated")
    }

    /**
     * Get the query history for the current user.
     */
    suspend fun getQueryHistory(): List<Query> {
        val queries = mutableListOf<Query>()

        auth.currentUser?.let { user ->
            val snapshots = firestore.collection("users")
                .document(user.uid)
                .collection("queries")
                .orderBy("timestamp")
                .get()
                .await()

            for (document in snapshots.documents) {
                document.toObject(Query::class.java)?.let {
                    queries.add(it)
                }
            }
        }

        return queries
    }
}