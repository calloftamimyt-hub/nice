package com.example.social

import kotlinx.serialization.Serializable

@Serializable
data class VideoResponse(
    val videos: List<VideoItem> = emptyList()
)

@Serializable
data class VideoItem(
    val filename: String,
    val url: String
)

@Serializable
data class UploadResponse(
    val message: String,
    val video: VideoItem
)

object GlobalVideoState {
    private val _videos = kotlinx.coroutines.flow.MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: kotlinx.coroutines.flow.StateFlow<List<VideoItem>> = _videos

    fun addVideo(video: VideoItem) {
        _videos.value = listOf(video) + _videos.value
    }
}
