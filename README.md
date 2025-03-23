# AI Web Assistant

<p align="center">
  Web content summarization and intelligent search in your pocket
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#technologies">Technologies</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#setup">Setup</a> •
  <a href="#usage">Usage</a> •
  <a href="#license">License</a>
</p>

## Features

AI Web Assistant is an Android application that leverages modern language models to provide two core features:

* **Web Content Summarization**: Extract and condense content from any website into concise, readable summaries using the BART-large-CNN model.

* **Intelligent Web Search**: Ask natural language questions and receive comprehensive answers generated from web search results, with proper source attribution.

Additional features include:

* **User Authentication**: Secure login and registration system using Firebase Authentication.
* **History Tracking**: View past summaries and searches, stored securely in Firebase Firestore.
* **Offline Support**: View previously generated summaries and answers even without an internet connection.
* **Clean UI**: Modern, intuitive interface built with Jetpack Compose and Material Design 3.

## Technologies

The application is built using modern Android development technologies:

* **Kotlin**: Primary language with extensive use of coroutines, flows, and language features
* **Jetpack Compose**: For building the declarative UI
* **Firebase**:
  * Authentication for user management
  * Firestore for data persistence
  * Analytics for usage insights
* **Hugging Face API**: Integration with the BART-large-CNN model for text summarization and answer generation
* **SerpAPI**: For high-quality web search capabilities
* **JSoup**: For HTML parsing and content extraction
* **Hilt**: For dependency injection
* **Retrofit/OkHttp**: For network operations
* **MVVM Architecture**: With clean separation of concerns

## Architecture

The application follows clean architecture principles with the following layers:

### Data Layer
* **API Services**: Handles communication with external APIs (Hugging Face, SerpAPI)
* **Repositories**: Acts as a single source of truth for data operations
* **Data Models**: Defines the structure of data entities

### Domain Layer
* **Use Cases**: Implements business logic and coordinates data operations
* **Domain Models**: Core business models independent of data sources

### Presentation Layer
* **ViewModels**: Manages UI state and handles user interactions
* **UI States**: Represents different states of the UI (loading, success, error)
* **Composable UI**: Implementation of the user interface

The app uses a unidirectional data flow pattern:
1. User interactions trigger events in the UI
2. Events are processed by ViewModels
3. ViewModels update UI state based on repository results
4. UI recomposes based on the new state

## Setup

### Prerequisites

* Android Studio Arctic Fox or newer
* Android SDK 24 or higher
* Kotlin 1.5.0 or higher
* Google Services JSON file from Firebase
* API keys for Hugging Face and SerpAPI

### Configuration

1. Clone the repository:
```bash
git clone https://github.com/AravProjs/AI-Web-Assistant.git
```

2. Open the project in Android Studio

3. Create a `env` file in the `assets` directory with the following content:
```
HUGGING_FACE_API_KEY=your_hugging_face_api_key
SERP_API_KEY=your_serp_api_key
```

4. Place the `google-services.json` file from Firebase in the `app` directory

5. Build the project:
```bash
./gradlew build
```

### Firebase Setup

1. Create a new Firebase project at [firebase.google.com](https://firebase.google.com)
2. Add an Android app to your Firebase project
3. Enable Authentication (Email/Password method)
4. Enable Firestore Database
5. Download the `google-services.json` file and place it in the `app` directory

## Usage

### Web Content Summarization

1. Enter a URL in the text field on the Summary screen
2. Tap the "Summarize" button
3. View the generated summary
4. Toggle "Previous Summaries" to see your history

### Intelligent Web Search

1. Navigate to the Search screen
2. Enter a question in the text field
3. Tap the "Search & Answer" button
4. View the generated answer and source information
5. See evidence used
6. Toggle "Previous Questions" to view your search history

## Performance Considerations

The application implements several optimizations:

* **Asynchronous Processing**: Uses Kotlin Coroutines for non-blocking operations
* **Lazy Loading**: Efficiently renders lists of history items
* **Content Limiting**: Implements smart truncation to reduce memory usage
* **Efficient Database Queries**: Retrieves only necessary data from Firestore


## Contact

Your Name - [aravverma15@gmail.com](mailto:aravverma15@gmail.com)

Project Link: [https://github.com/AravProjs/ai-web-assistant](https://github.com/AravProjs/ai-web-assistant)

---

<p align="center">
  <i>Built with ❤️ using Kotlin and Jetpack Compose</i>
</p>
