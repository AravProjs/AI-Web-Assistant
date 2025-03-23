package com.example.aiwebsummarizer.ui.summary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aiwebsummarizer.data.api.HuggingFaceApi
import com.example.aiwebsummarizer.data.api.WebScraperService
import com.example.aiwebsummarizer.data.model.Summary
import com.example.aiwebsummarizer.data.repository.SummaryRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SummaryViewModel(
    private val summaryRepository: SummaryRepository,
    private val analytics: FirebaseAnalytics
) : ViewModel() {

    private val _summaryState = MutableStateFlow<SummaryState>(SummaryState.Initial)
    val summaryState: StateFlow<SummaryState> = _summaryState

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Initial)
    val historyState: StateFlow<HistoryState> = _historyState

    // this would be securely stored
    private val apiKey = "HUGGING_FACE_API_KEY"

    fun summarizeUrl(url: String) {
        viewModelScope.launch {
            try {
                _summaryState.value = SummaryState.Loading

                // Log analytics event
                analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "url_summarization")
                    param(FirebaseAnalytics.Param.ITEM_ID, url)
                }

                // Use the actual repository to summarize URL
                val summary = summaryRepository.summarizeUrl(url, apiKey)
                _summaryState.value = SummaryState.Success(summary)

                // Load updated history
                loadSummaryHistory()

            } catch (e: Exception) {
                _summaryState.value = SummaryState.Error(e.message ?: "Failed to summarize URL")

                // Log error to analytics
                analytics.logEvent("summarization_error") {
                    param("error_message", e.message ?: "Unknown error")
                    param("url", url)
                }
            }
        }
    }

    fun loadSummaryHistory() {
        viewModelScope.launch {
            try {
                _historyState.value = HistoryState.Loading

                // Use the actual repository to get history
                val history = summaryRepository.getSummaryHistory()
                _historyState.value = HistoryState.Success(history)

            } catch (e: Exception) {
                _historyState.value = HistoryState.Error(e.message ?: "Failed to load history")
            }
        }
    }

    // Factory to create this ViewModel
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SummaryViewModel::class.java)) {
                // Create the network components
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api-inference.huggingface.co/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val huggingFaceApi = retrofit.create(HuggingFaceApi::class.java)
                val webScraperService = WebScraperService()

                // Get Firebase instances
                val firestore = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                val analytics = FirebaseAnalytics.getInstance(context)

                // Create repository
                val summaryRepository = SummaryRepository(
                    huggingFaceApi,
                    webScraperService,
                    firestore,
                    auth
                )

                return SummaryViewModel(summaryRepository, analytics) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class SummaryState {
    object Initial : SummaryState()
    object Loading : SummaryState()
    data class Success(val summary: Summary) : SummaryState()
    data class Error(val message: String) : SummaryState()
}

sealed class HistoryState {
    object Initial : HistoryState()
    object Loading : HistoryState()
    data class Success(val summaries: List<Summary>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}