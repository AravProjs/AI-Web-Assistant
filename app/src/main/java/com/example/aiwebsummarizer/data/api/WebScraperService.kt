package com.example.aiwebsummarizer.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class WebScraperService {
    suspend fun scrapeWebContent(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val document = Jsoup.connect(url).get()

                // Extract content based on site type
                val content = when {
                    // For news sites, focus on article content
                    url.contains("bbc.com") || url.contains("bbc.co.uk") -> {
                        val articleBody = document.select("article, .article-body, [data-component='text-block']")
                        if (articleBody.isNotEmpty()) {
                            articleBody.text()
                        } else {
                            document.select("p").text()
                        }
                    }
                    // Wikipedia specific handling
                    url.contains("wikipedia.org") -> {
                        document.select("#mw-content-text p").text()
                    }
                    // Generic article extraction
                    else -> {
                        val articleElements = document.select("article, .article, .post, .content, main")
                        if (articleElements.isNotEmpty()) {
                            articleElements.first()?.text() ?: ""
                        } else {
                            // Get meaningful paragraphs
                            document.select("p").text()
                        }
                    }
                }

                // Important: Limit content length to avoid token limit errors
                // BART model typically has a 1024 token limit, which is roughly 3000-4000 characters
                val truncatedContent = content.take(3000)

                // Log the length for debugging
                Log.d("WebScraperService", "Scraped length: ${content.length}, Truncated: ${truncatedContent.length}")

                return@withContext truncatedContent
            } catch (e: Exception) {
                Log.e("WebScraperService", "Scraping error: ${e.message}")
                throw Exception("Failed to scrape content: ${e.message}")
            }
        }
    }
}