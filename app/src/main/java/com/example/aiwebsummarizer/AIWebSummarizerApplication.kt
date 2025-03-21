package com.example.aiwebsummarizer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIWebSummarizerApplication : Application() {
    // Add an empty override to ensure the class is properly recognized
    override fun onCreate() {
        super.onCreate()
        // Log or add a simple statement to verify initialization
        println("AIWebSummarizerApplication initialized")
    }
}