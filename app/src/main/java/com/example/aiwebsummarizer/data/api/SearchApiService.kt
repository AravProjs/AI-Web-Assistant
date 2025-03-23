package com.example.aiwebsummarizer.data.api

import com.example.aiwebsummarizer.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

/**
 * Service to handle search queries and retrieve relevant information from the web.
 */
class SearchApiService(private val client: OkHttpClient) {

    companion object {
        private const val GOOGLE_SEARCH_URL = "https://www.google.com/search?q="
        private const val MAX_RESULTS = 5
        private const val MAX_CONTENT_LENGTH = 5000
    }

    /**
     * Search for information based on a user query.
     *
     * @param query The user's question or search query
     * @return A list of SearchResult objects containing relevant information
     */
    suspend fun searchForQuery(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            // Encode the query for URL
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = GOOGLE_SEARCH_URL + encodedQuery

            // Perform the search and get result links
            val searchResultLinks = extractSearchResultLinks(searchUrl)

            // Limit the number of links to process
            val limitedLinks = searchResultLinks.take(MAX_RESULTS)

            // Fetch content from each link in parallel
            val searchResults = limitedLinks.map { link ->
                async {
                    try {
                        val content = fetchContentFromUrl(link)
                        SearchResult(
                            url = link,
                            title = extractTitle(link, content),
                            content = content.content,
                            relevance = calculateRelevance(query, content.content)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            // Sort by relevance score (highest first)
            searchResults.sortedByDescending { it.relevance }
        } catch (e: Exception) {
            throw Exception("Failed to perform search: ${e.message}")
        }
    }

    /**
     * Extract search result links from Google search results page.
     */
    private suspend fun extractSearchResultLinks(searchUrl: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""

            val document = Jsoup.parse(html)

            // Extract links from search results
            val links = document.select("a[href]")
                .map { it.attr("href") }
                .filter { it.startsWith("/url?q=") }
                .map { it.removePrefix("/url?q=").substringBefore("&") }
                .filter { !it.contains("google.com") && !it.contains("youtube.com") }
                .distinct()

            links
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch content from a specific URL.
     */
    private suspend fun fetchContentFromUrl(url: String): ScrapedContent = withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()

            val content = extractRelevantContent(document)

            ScrapedContent(
                title = document.title(),
                content = content.take(MAX_CONTENT_LENGTH)
            )
        } catch (e: Exception) {
            throw Exception("Failed to fetch content from $url: ${e.message}")
        }
    }

    /**
     * Extract the most relevant content from an HTML document.
     */
    private fun extractRelevantContent(document: Document): String {
        // Remove unnecessary elements
        document.select("script, style, nav, footer, header, .header, .footer, .nav, .ads, .ad, .menu, .sidebar").remove()

        // Extract main content based on common patterns
        val contentElements = listOf(
            document.select("article").text(),
            document.select(".content, .main, main, #content, #main").text(),
            document.select("p").text()
        )

        // Use the first non-empty content source
        return contentElements.firstOrNull { it.isNotEmpty() } ?: document.text()
    }

    /**
     * Extract a meaningful title from the URL and content.
     */
    private fun extractTitle(url: String, content: ScrapedContent): String {
        return if (content.title.isNotEmpty()) {
            content.title
        } else {
            // Extract domain name as title
            val domain = url.substringAfter("//").substringBefore("/")
            domain
        }
    }

    /**
     * Calculate relevance score for sorting search results.
     */
    private fun calculateRelevance(query: String, content: String): Double {
        val queryWords = query.lowercase().split(" ", ".", ",", "?", "!")
            .filter { it.length > 3 }  // Filter out short words

        var score = 0.0

        queryWords.forEach { word ->
            // Count occurrences of query words
            val wordCount = content.lowercase().split(" ", ".", ",").count { it == word }
            score += wordCount * 0.5

            // Boost score if the word appears in the first paragraph
            val firstParagraph = content.substringBefore("\n\n").lowercase()
            if (word in firstParagraph) {
                score += 2.0
            }
        }

        return score
    }
}

/**
 * Represents scraped content from a web page.
 */
data class ScrapedContent(
    val title: String,
    val content: String
)

// SearchResult class is now imported from data.model package