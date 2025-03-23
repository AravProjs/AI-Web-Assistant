package com.example.aiwebsummarizer.di

import com.example.aiwebsummarizer.data.api.HuggingFaceApi
import com.example.aiwebsummarizer.data.api.LLMApiService
import com.example.aiwebsummarizer.data.api.SearchApiService
import com.example.aiwebsummarizer.data.api.SerpApiService
import com.example.aiwebsummarizer.data.api.WebScraperService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api-inference.huggingface.co/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHuggingFaceApi(retrofit: Retrofit): HuggingFaceApi {
        return retrofit.create(HuggingFaceApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWebScraperService(): WebScraperService {
        return WebScraperService()
    }

    @Provides
    @Singleton
    fun provideSearchApiService(okHttpClient: OkHttpClient): SearchApiService {
        return SearchApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideLLMApiService(okHttpClient: OkHttpClient): LLMApiService {
        return LLMApiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideSerpApiService(okHttpClient: OkHttpClient): SerpApiService {
        return SerpApiService(okHttpClient)
    }
}