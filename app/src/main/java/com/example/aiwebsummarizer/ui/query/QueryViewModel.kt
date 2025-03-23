package com.example.aiwebsummarizer.ui.query

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aiwebsummarizer.data.api.LLMApiService
import com.example.aiwebsummarizer.data.api.SearchApiService
import com.example.aiwebsummarizer.data.api.SerpApiService
import com.example.aiwebsummarizer.data.model.Query
import com.example.aiwebsummarizer.data.model.QueryResult
import com.example.aiwebsummarizer.data.repository.QueryRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import io.github.cdimascio.dotenv.Dotenv


class QueryViewModel(
    private val queryRepository: QueryRepository,
    private val analytics: FirebaseAnalytics
) : ViewModel() {

    private val _queryState = MutableStateFlow<QueryState>(QueryState.Initial)
    val queryState: StateFlow<QueryState> = _queryState

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Initial)
    val historyState: StateFlow<HistoryState> = _historyState

    // In production, this should be securely stored
    private val dotenv: Dotenv = Dotenv.configure()
        .directory("/assets")
        .filename("env")
        .load()

    private val apiKey: String by lazy {
        dotenv["HUGGING_FACE_API_KEY"] ?: throw IllegalArgumentException("HUGGING_FACE_API_KEY not found in env file")
    }

    /**
     * Process a user query.
     *
     * @param questionText The text of the user's question
     */
    fun processQuery(questionText: String) {
        viewModelScope.launch {
            try {
                _queryState.value = QueryState.Loading

                // Log analytics event
                analytics.logEvent(FirebaseAnalytics.Event.SEARCH) {
                    param(FirebaseAnalytics.Param.SEARCH_TERM, questionText)
                }

                // Process the query using the repository
                val queryResult = queryRepository.processQuery(questionText, apiKey)
                _queryState.value = QueryState.Success(queryResult)

                // Load updated history
                loadQueryHistory()

            } catch (e: Exception) {
                _queryState.value = QueryState.Error(e.message ?: "Failed to process query")

                // Log error to analytics
                analytics.logEvent("query_error") {
                    param("error_message", e.message ?: "Unknown error")
                    param("query", questionText)
                }
            }
        }
    }

    /**
     * Load the user's query history.
     */
    fun loadQueryHistory() {
        viewModelScope.launch {
            try {
                _historyState.value = HistoryState.Loading

                // Get query history from repository
                val history = queryRepository.getQueryHistory()
                _historyState.value = HistoryState.Success(history)

            } catch (e: Exception) {
                _historyState.value = HistoryState.Error(e.message ?: "Failed to load history")
            }
        }
    }

    /**
     * Factory to create this ViewModel.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QueryViewModel::class.java)) {
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

                // Create services
                val searchApiService = SearchApiService(okHttpClient)
                val serpApiService = SerpApiService(okHttpClient)  // Create SerpApiService instance
                val llmApiService = LLMApiService(okHttpClient)

                // Get Firebase instances
                val firestore = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()
                val analytics = FirebaseAnalytics.getInstance(context)

                // Create repository with SerpApiService
                val queryRepository = QueryRepository(
                    searchApiService,
                    serpApiService,  // Pass SerpApiService instance
                    llmApiService,
                    firestore,
                    auth
                )

                return QueryViewModel(queryRepository, analytics) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class QueryState {
    object Initial : QueryState()
    object Loading : QueryState()
    data class Success(val result: QueryResult) : QueryState()
    data class Error(val message: String) : QueryState()
}

sealed class HistoryState {
    object Initial : HistoryState()
    object Loading : HistoryState()
    data class Success(val queries: List<Query>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}