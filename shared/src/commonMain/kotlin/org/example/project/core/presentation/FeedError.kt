package org.example.project.core.presentation

sealed class FeedError(val message: String) {
    class Network(message: String = "Network error") : FeedError(message)
    class Timeout(message: String = "Request timed out") : FeedError(message)
    class Offline(message: String = "No internet connection") : FeedError(message)
    class Authentication(message: String = "Authentication failed") : FeedError(message)
    class Server(message: String = "Server error") : FeedError(message)
    class Parsing(message: String = "Data parsing error") : FeedError(message)
    class Unknown(message: String = "An unknown error occurred") : FeedError(message)
}
