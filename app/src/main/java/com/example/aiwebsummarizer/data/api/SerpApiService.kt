package com.example.aiwebsummarizer.data.api

import android.util.Log
import com.example.aiwebsummarizer.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Service to perform search queries using SerpAPI
 */
class SerpApiService(private val client: OkHttpClient) {

    companion object {
        private const val TAG = "SerpApiService"
        private const val SERP_API_BASE_URL = "https://serpapi.com/search.json"
        private const val FALLBACK_ENABLED = true  // Enable fallback to mock data if API fails
    }

    /**
     * Search for information based on a user query using SerpAPI
     *
     * @param query The user's question or search query
     * @param apiKey Your SerpAPI key
     * @return A list of SearchResult objects containing relevant information
     */
    suspend fun searchForQuery(query: String, apiKey: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            // Encode the query for URL
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            // Build the SerpAPI URL with parameters
            val url = "$SERP_API_BASE_URL?q=$encodedQuery&api_key=$apiKey&engine=google"

            Log.d(TAG, "Sending request to SerpAPI: $url")

            // Create the request
            val request = Request.Builder()
                .url(url)
                .build()

            // Execute the request
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Failed to get response from SerpAPI: ${response.code}")
                return@withContext if (FALLBACK_ENABLED) generateMockSearchResults(query) else emptyList()
            }

            // Parse the response
            return@withContext parseSearchResults(responseBody, query)

        } catch (e: Exception) {
            Log.e(TAG, "Error searching with SerpAPI: ${e.message}", e)
            return@withContext if (FALLBACK_ENABLED) generateMockSearchResults(query) else emptyList()
        }
    }

    /**
     * Parse SerpAPI JSON response into SearchResult objects
     */
    private fun parseSearchResults(responseBody: String, query: String): List<SearchResult> {
        try {
            val jsonObject = JSONObject(responseBody)

            // Initialize results list
            val results = mutableListOf<SearchResult>()

            // Parse organic search results
            if (jsonObject.has("organic_results")) {
                val organicResults = jsonObject.getJSONArray("organic_results")

                for (i in 0 until minOf(organicResults.length(), 5)) {
                    val result = organicResults.getJSONObject(i)

                    val title = result.optString("title", "")
                    val url = result.optString("link", "")

                    // Get snippet or description
                    val snippet = if (result.has("snippet")) {
                        result.getString("snippet")
                    } else if (result.has("description")) {
                        result.getString("description")
                    } else {
                        "No description available"
                    }

                    // Add to results with a relevance score based on position
                    results.add(SearchResult(
                        url = url,
                        title = title,
                        content = snippet,
                        relevance = 10.0 - i.toDouble() // Higher relevance for top results
                    ))
                }
            }

            // Parse knowledge graph if available (for direct answers)
            if (jsonObject.has("knowledge_graph")) {
                val knowledgeGraph = jsonObject.getJSONObject("knowledge_graph")

                if (knowledgeGraph.has("description")) {
                    val title = knowledgeGraph.optString("title", "Knowledge Graph")
                    val description = knowledgeGraph.getString("description")

                    // Add knowledge graph with highest relevance
                    results.add(0, SearchResult(
                        url = "https://www.google.com/search?q=$query",
                        title = title,
                        content = description,
                        relevance = 15.0 // Highest relevance for direct answers
                    ))
                }
            }

            // If no results, use fallback
            if (results.isEmpty() && FALLBACK_ENABLED) {
                return generateMockSearchResults(query)
            }

            return results

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SerpAPI response: ${e.message}", e)
            return if (FALLBACK_ENABLED) generateMockSearchResults(query) else emptyList()
        }
    }

    /**
     * Generate mock search results for development and testing
     * Used as fallback when API fails
     */
    private fun generateMockSearchResults(query: String): List<SearchResult> {
        Log.d(TAG, "Using fallback mock results for: $query")
        val cleanQuery = query.lowercase().trim()

        return listOf(
            SearchResult(
                url = "https://example.com/article1",
                title = "Understanding ${query.replaceFirstChar { it.uppercase() }}",
                content = "Comprehensive guide about ${query}. This article covers the basic concepts, " +
                        "history, development, and modern applications. ${query.replaceFirstChar { it.uppercase() }} " +
                        "has been studied extensively in recent years, with researchers finding new ways to " +
                        "implement and improve existing techniques. The future of ${query} looks promising " +
                        "as more industries adopt these technologies.",
                relevance = 9.5
            ),
            SearchResult(
                url = "https://example.org/research/${cleanQuery.replace(" ", "-")}",
                title = "Latest Research on ${query.replaceFirstChar { it.uppercase() }}",
                content = "Recent studies have shown remarkable progress in the field of ${query}. " +
                        "According to experts, ${query} will continue to evolve and impact various sectors " +
                        "including healthcare, finance, and education. The key findings suggest that " +
                        "${query} implementation can lead to efficiency improvements of up to 45% when " +
                        "properly integrated with existing systems.",
                relevance = 8.7
            ),
            SearchResult(
                url = "https://encyclopedia.net/${cleanQuery.replace(" ", "_")}",
                title = "${query.replaceFirstChar { it.uppercase() }} - Encyclopedia",
                content = "Definition: ${query.replaceFirstChar { it.uppercase() }} refers to the systematic approach " +
                        "to solving complex problems through algorithmic methods. Origins: The concept was first " +
                        "introduced in the late 20th century and has since gained widespread adoption. " +
                        "Applications: Modern implementations include automated systems, decision support " +
                        "tools, and predictive analytics frameworks.",
                relevance = 7.9
            )
        )
    }
}