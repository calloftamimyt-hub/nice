package com.example.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Supabase
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import io.github.jan.supabase.auth.auth
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch

@Composable
fun WhatsOnYourMindSection(onNavigateToCreatePost: () -> Unit = {}) {
    var isUserLoggedIn by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("User") }
    var currentUserAvatar by remember { mutableStateOf<String?>(null) }
    
    val auth = remember { Supabase.client.auth }

    LaunchedEffect(Unit) {
        val user = auth.currentUserOrNull()
        isUserLoggedIn = user != null
        if (user != null) {
            currentUserName = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User"
            currentUserAvatar = user.userMetadata?.get("avatar_url")?.toString()?.replace("\"", "")
        }
    }

    if (!isUserLoggedIn) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(12.dp))
            .clickable { onNavigateToCreatePost() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile Logo Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(currentUserName.take(1).uppercase(), color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                "What's on your mind?", 
                color = TextGray, 
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha=0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, contentDescription = "Photo", tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Photo", color = TextDark, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoLibrary, contentDescription = "Video", tint = Color(0xFFF44336))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Video", color = TextDark, fontSize = 14.sp)
            }
        }
    }
    
    // Video Feed below What's on your mind
    VideoFeedSection()
}

@Composable
fun VideoFeedSection() {
    val globalVideos by com.example.social.GlobalVideoState.videos.collectAsState()
    var fetchedVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response: io.ktor.client.statement.HttpResponse = com.example.network.ApiClient.ktor.get("${com.example.network.ApiConfig.BASE_URL}api/videos")
            val data = response.body<VideoResponse>()
            fetchedVideos = data.videos
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val videos = globalVideos + fetchedVideos

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Shorts Feed & Recent Posts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isLoading && videos.isEmpty()) {
            CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (videos.isEmpty()) {
            Text("No posts available right now.", color = TextGray, fontSize = 14.sp)
        } else {
            videos.forEach { video ->
                VideoPostCard(video)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun VideoPostCard(video: VideoItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Mock user details
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("User", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text("Just now", fontSize = 12.sp, color = TextGray)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Uploaded video file: ${video.filename}", color = TextDark, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val isImage = video.filename.endsWith(".jpg", ignoreCase = true) || 
                      video.filename.endsWith(".jpeg", ignoreCase = true) ||
                      video.filename.endsWith(".png", ignoreCase = true)
                      
        if (isImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray.copy(alpha=0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = "Image Post", tint = PrimaryGreen, modifier = Modifier.size(48.dp))
            }
        } else {
            // Video Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VideoLibrary, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Like", color = TextGray, fontSize = 14.sp)
            Text("Comment", color = TextGray, fontSize = 14.sp)
            Text("Share", color = TextGray, fontSize = 14.sp)
            Text("Report", color = Color.Red, fontSize = 14.sp)
        }
    }
}

@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    
    val coroutineScope = rememberCoroutineScope()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedMediaUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Post", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
        }

        Divider(color = Color.LightGray.copy(alpha=0.5f))

        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            // Media Preview
            if (selectedMediaUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { context ->
                            android.widget.VideoView(context).apply {
                                setVideoURI(selectedMediaUri)
                                start()
                                setOnCompletionListener { start() }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGreen.copy(alpha = 0.05f))
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { mediaPickerLauncher.launch("*/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = "Add Media", tint = PrimaryGreen, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add Photo or Video", color = PrimaryGreen, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Title Field
            Text("Title", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter post title...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description Field
            Text("Description (Optional)", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Say something about this...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isUploading) {
                LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth().height(8.dp), color = PrimaryGreen)
                Spacer(modifier = Modifier.height(12.dp))
                if (processing) {
                    Text("Processing... After processing, your video will be available.", color = PrimaryGreen, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("Uploading to server... ${(uploadProgress * 100).toInt()}%", color = TextGray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Button(
                    onClick = { 
                        if (titleInput.isNotBlank() && selectedMediaUri != null) {
                             isUploading = true
                             coroutineScope.launch {
                                 // Simulate upload progress
                                 for (i in 1..100) {
                                     uploadProgress = i / 100f
                                     kotlinx.coroutines.delay(20)
                                 }
                                 processing = true
                                 // Simulate processing time
                                 kotlinx.coroutines.delay(1500)
                                 com.example.social.GlobalVideoState.addVideo(
                                    VideoItem(
                                        filename = titleInput,
                                        url = selectedMediaUri.toString()
                                    )
                                 )
                                 // "Data will be sent to the server"
                                 onNavigateBack()
                             }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    enabled = !isUploading && titleInput.isNotBlank() && selectedMediaUri != null
                ) {
                    Text("Publish Post", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
