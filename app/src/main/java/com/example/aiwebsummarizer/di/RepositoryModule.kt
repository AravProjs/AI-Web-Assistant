package com.example.aiwebsummarizer.di

import com.example.aiwebsummarizer.data.api.HuggingFaceApi
import com.example.aiwebsummarizer.data.api.WebScraperService
import com.example.aiwebsummarizer.data.repository.SummaryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSummaryRepository(
        huggingFaceApi: HuggingFaceApi,
        webScraperService: WebScraperService,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): SummaryRepository {
        return SummaryRepository(huggingFaceApi, webScraperService, firestore, auth)
    }
}