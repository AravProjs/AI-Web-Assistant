package com.example.aiwebsummarizer.data.repository

import com.example.aiwebsummarizer.data.api.HuggingFaceApi
import com.example.aiwebsummarizer.data.api.WebScraperService
import com.example.aiwebsummarizer.data.model.Parameters
import com.example.aiwebsummarizer.data.model.Summary
import com.example.aiwebsummarizer.data.model.SummaryRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SummaryRepository(
    private val huggingFaceApi: HuggingFaceApi,
    private val webScraperService: WebScraperService,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun summarizeUrl(url: String, apiKey: String): Summary {
        // Step 1: Scrape content from the URL
        val content = webScraperService.scrapeWebContent(url)

        // Step 2: Send content to Hugging Face API for summarization
        val responseList = huggingFaceApi.getSummary(
            apiKey = "Bearer $apiKey",
            request = SummaryRequest(inputs = content, parameters = Parameters())
        )

        // Get summary text from response
        val summaryText = if (responseList.isNotEmpty()) {
            responseList[0].summary_text
        } else {
            "No summary generated"
        }

        // Step 3: Create the summary object
        val summary = Summary(
            url = url,
            originalText = content,
            summarizedText = summaryText
        )

        // Save to Firestore if user is authenticated
        auth.currentUser?.let { user ->
            val summaryWithId = summary.copy(id = firestore.collection("summaries").document().id)
            firestore.collection("users")
                .document(user.uid)
                .collection("summaries")
                .document(summaryWithId.id)
                .set(summaryWithId)
                .await()

            return summaryWithId
        }

        return summary
    }

    suspend fun getSummaryHistory(): List<Summary> {
        val summaries = mutableListOf<Summary>()

        auth.currentUser?.let { user ->
            val snapshots = firestore.collection("users")
                .document(user.uid)
                .collection("summaries")
                .orderBy("timestamp")
                .get()
                .await()

            for (document in snapshots.documents) {
                document.toObject(Summary::class.java)?.let {
                    summaries.add(it)
                }
            }
        }

        return summaries
    }
}