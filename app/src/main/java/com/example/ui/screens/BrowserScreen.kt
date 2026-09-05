package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.network.BrowserMediaItem
import com.example.data.network.GroundedSearchResponse
import com.example.ui.components.openExternalBrowser
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import com.example.ui.theme.VLinkViolet
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

enum class BrowserTabCategory {
    ALL, NEWS, SPORTS, VIDEOS, AI_SEARCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val browserUrl by viewModel.browserCurrentUrl.collectAsState()
    val isWebViewActive by viewModel.showBrowserWebView.collectAsState()

    val newsItems by viewModel.browserNewsItems.collectAsState()
    val sportsItems by viewModel.browserSportsItems.collectAsState()
    val videoItems by viewModel.browserVideoItems.collectAsState()
    val isMediaLoading by viewModel.isBrowserMediaLoading.collectAsState()
    val aiSearchResponse by viewModel.browserAiSearchResponse.collectAsState()
    val isAiSearchLoading by viewModel.isBrowserAiSearchLoading.collectAsState()

    var selectedCategory by remember { mutableStateOf(BrowserTabCategory.ALL) }
    var inputUrlText by remember { mutableStateOf("") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("V-Link Browser") }
    var pageProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Synchronize input text with loaded URL when active
    LaunchedEffect(browserUrl) {
        if (browserUrl.isNotBlank() && browserUrl != "about:blank") {
            inputUrlText = browserUrl
        }
    }

    // Hardware back handler for in-app WebView
    BackHandler(enabled = isWebViewActive) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            viewModel.closeBrowserWebView()
        }
    }

    val onPerformSearch: (String) -> Unit = { query ->
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            keyboardController?.hide()
            if (selectedCategory == BrowserTabCategory.AI_SEARCH) {
                viewModel.performAiSearchWithGoogleGrounding(trimmed)
            } else {
                viewModel.navigateToUrlOrSearch(trimmed)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Browser Header / Address Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isWebViewActive) {
                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    } else {
                                        viewModel.closeBrowserWebView()
                                    }
                                },
                                modifier = Modifier.testTag("browser_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(VLinkCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Browser",
                                    tint = VLinkCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Search & Address Input Box
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isWebViewActive) VLinkCyan.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isWebViewActive) Icons.Outlined.Lock else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isWebViewActive) PulseGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                TextField(
                                    value = inputUrlText,
                                    onValueChange = { inputUrlText = it },
                                    placeholder = {
                                        Text(
                                            text = if (selectedCategory == BrowserTabCategory.AI_SEARCH) {
                                                "Ask Gemini with Google Search..."
                                            } else {
                                                "Search or type web address..."
                                            },
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = { onPerformSearch(inputUrlText) }
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("browser_address_input")
                                )

                                if (inputUrlText.isNotBlank()) {
                                    IconButton(
                                        onClick = { inputUrlText = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onPerformSearch(inputUrlText) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("browser_go_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Navigate",
                                        tint = VLinkCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Right action buttons
                        if (isWebViewActive) {
                            IconButton(
                                onClick = {
                                    webViewInstance?.reload()
                                },
                                modifier = Modifier.testTag("browser_refresh_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    val current = webViewInstance?.url ?: browserUrl
                                    openExternalBrowser(context, current)
                                },
                                modifier = Modifier.testTag("browser_open_external_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "Open in external browser",
                                    tint = VLinkViolet
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.closeBrowserWebView()
                                },
                                modifier = Modifier.testTag("browser_close_webview_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Discovery Home",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Loading Progress Indicator
                    if (isWebViewActive && pageProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { pageProgress / 100f },
                            color = VLinkCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                        )
                    }

                    // Category Pill Tabs (shown when not viewing a full webpage)
                    if (!isWebViewActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrowserCategoryChip(
                                title = "🌟 All",
                                isSelected = selectedCategory == BrowserTabCategory.ALL,
                                onClick = { selectedCategory = BrowserTabCategory.ALL }
                            )
                            BrowserCategoryChip(
                                title = "📰 News",
                                isSelected = selectedCategory == BrowserTabCategory.NEWS,
                                onClick = { selectedCategory = BrowserTabCategory.NEWS }
                            )
                            BrowserCategoryChip(
                                title = "⚽ Sports",
                                isSelected = selectedCategory == BrowserTabCategory.SPORTS,
                                onClick = { selectedCategory = BrowserTabCategory.SPORTS }
                            )
                            BrowserCategoryChip(
                                title = "🎥 Videos",
                                isSelected = selectedCategory == BrowserTabCategory.VIDEOS,
                                onClick = { selectedCategory = BrowserTabCategory.VIDEOS }
                            )
                            BrowserCategoryChip(
                                title = "🔍 Google Search (AI)",
                                isSelected = selectedCategory == BrowserTabCategory.AI_SEARCH,
                                onClick = { selectedCategory = BrowserTabCategory.AI_SEARCH }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWebViewActive) {
                // In-App WebView Component
                InAppWebViewComponent(
                    url = browserUrl,
                    onWebViewCreated = { webViewInstance = it },
                    onTitleChanged = { pageTitle = it },
                    onProgressChanged = { pageProgress = it },
                    onNavigationStateChanged = { back, forward ->
                        canGoBack = back
                        canGoForward = forward
                    },
                    onOpenUrl = { newUrl ->
                        inputUrlText = newUrl
                    }
                )
            } else {
                // Discovery Hub (News, Sports, Videos, AI Search)
                BrowserDiscoveryContent(
                    category = selectedCategory,
                    newsList = newsItems,
                    sportsList = sportsItems,
                    videoList = videoItems,
                    aiSearchResponse = aiSearchResponse,
                    isMediaLoading = isMediaLoading,
                    isAiSearchLoading = isAiSearchLoading,
                    onRefresh = { viewModel.refreshBrowserFeeds() },
                    onItemClick = { item ->
                        viewModel.openInAppBrowser(item.url)
                    },
                    onOpenAiSource = { source ->
                        viewModel.openInAppBrowser(source.url)
                    },
                    onQuickBookmarkClick = { bookmarkUrl ->
                        viewModel.openInAppBrowser(bookmarkUrl)
                    },
                    onSearchQuery = { query ->
                        onPerformSearch(query)
                    }
                )
            }
        }
    }
}

@Composable
fun BrowserCategoryChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) {
        VLinkCyan.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val border = if (isSelected) VLinkCyan else Color.Transparent
    val textCol = if (isSelected) VLinkCyan else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textCol,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppWebViewComponent(
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, currentUrl: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, currentUrl, favicon)
                        currentUrl?.let { onOpenUrl(it) }
                        onNavigationStateChanged(canGoBack(), canGoForward())
                    }

                    override fun onPageFinished(view: WebView?, currentUrl: String?) {
                        super.onPageFinished(view, currentUrl)
                        onNavigationStateChanged(canGoBack(), canGoForward())
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false
                        if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                            return false // Load inside this WebView
                        }
                        return try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                            ctx.startActivity(intent)
                            true
                        } catch (e: Exception) {
                            true
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            if (webView.url != url && url.isNotBlank()) {
                webView.loadUrl(url)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("in_app_webview")
    )
}

@Composable
fun BrowserDiscoveryContent(
    category: BrowserTabCategory,
    newsList: List<BrowserMediaItem>,
    sportsList: List<BrowserMediaItem>,
    videoList: List<BrowserMediaItem>,
    aiSearchResponse: GroundedSearchResponse?,
    isMediaLoading: Boolean,
    isAiSearchLoading: Boolean,
    onRefresh: () -> Unit,
    onItemClick: (BrowserMediaItem) -> Unit,
    onOpenAiSource: (com.example.data.network.GroundedSource) -> Unit,
    onQuickBookmarkClick: (String) -> Unit,
    onSearchQuery: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("browser_discovery_feed"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Bookmarks row
        item {
            QuickBookmarksSection(onBookmarkClick = onQuickBookmarkClick)
        }

        // Live Grounding Status Banner
        item {
            SearchGroundingBanner(
                isRefreshing = isMediaLoading,
                onRefresh = onRefresh
            )
        }

        // AI Search Result (if category is AI_SEARCH or response is present)
        if (category == BrowserTabCategory.AI_SEARCH || aiSearchResponse != null) {
            item {
                AiGroundedSearchResultCard(
                    response = aiSearchResponse,
                    isLoading = isAiSearchLoading,
                    onOpenSource = onOpenAiSource,
                    onSearchPrompt = onSearchQuery
                )
            }
        }

        // NEWS Section
        if (category == BrowserTabCategory.ALL || category == BrowserTabCategory.NEWS) {
            item {
                SectionHeader(
                    title = "Breaking News & Global Headlines",
                    icon = Icons.Outlined.Newspaper,
                    color = VLinkCyan
                )
            }

            items(newsList) { news ->
                NewsMediaCard(
                    item = news,
                    onClick = { onItemClick(news) }
                )
            }
        }

        // SPORTS Section
        if (category == BrowserTabCategory.ALL || category == BrowserTabCategory.SPORTS) {
            item {
                SectionHeader(
                    title = "Live Sports, Scores & Matches",
                    icon = Icons.Outlined.SportsSoccer,
                    color = PulseGreen
                )
            }

            items(sportsList) { sports ->
                SportsMediaCard(
                    item = sports,
                    onClick = { onItemClick(sports) }
                )
            }
        }

        // VIDEOS Section
        if (category == BrowserTabCategory.ALL || category == BrowserTabCategory.VIDEOS) {
            item {
                SectionHeader(
                    title = "Trending Videos & Highlights",
                    icon = Icons.Outlined.VideoLibrary,
                    color = VLinkViolet
                )
            }

            items(videoList) { video ->
                VideoMediaCard(
                    item = video,
                    onClick = { onItemClick(video) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
fun QuickBookmarksSection(
    onBookmarkClick: (String) -> Unit
) {
    val bookmarks = remember {
        listOf(
            Triple("Google", "https://www.google.com", Icons.Default.Search),
            Triple("YouTube", "https://www.youtube.com", Icons.Default.PlayCircle),
            Triple("BBC News", "https://www.bbc.com/news", Icons.Default.Newspaper),
            Triple("ESPN", "https://www.espn.com", Icons.Default.SportsBasketball),
            Triple("Wikipedia", "https://www.wikipedia.org", Icons.Default.MenuBook),
            Triple("Reuters", "https://www.reuters.com", Icons.Default.Public)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Popular Websites",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            bookmarks.forEach { (name, url, icon) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBookmarkClick(url) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("bookmark_$name")
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .border(1.dp, VLinkCyan.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = VLinkCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SearchGroundingBanner(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, VLinkCyan.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VLinkCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = VLinkCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Google Search Grounded Intelligence",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Live news, real-time sports results, and trending web videos powered by Gemini 2.5 Flash.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.testTag("refresh_grounding_feed_btn")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = VLinkCyan
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Feed",
                        tint = VLinkCyan
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NewsMediaCard(
    item: BrowserMediaItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("news_card_${item.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!item.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = item.source,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.source,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VLinkCyan
                    )
                    Text(
                        text = item.timestamp,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read in App →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VLinkCyan
                    )
                }
            }
        }
    }
}

@Composable
fun SportsMediaCard(
    item: BrowserMediaItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("sports_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PulseGreen.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.timestamp,
                            color = PulseGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.source,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VideoMediaCard(
    item: BrowserMediaItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("video_card_${item.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E2E))
                    )
                }

                // Play Button Overlay
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Video duration badge
                item.duration?.let { dur ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = dur,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.source,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = VLinkViolet
                    )
                    Text(
                        text = "Watch in App 🎬",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VLinkCyan
                    )
                }
            }
        }
    }
}

@Composable
fun AiGroundedSearchResultCard(
    response: GroundedSearchResponse?,
    isLoading: Boolean,
    onOpenSource: (com.example.data.network.GroundedSource) -> Unit,
    onSearchPrompt: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, VLinkCyan.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_grounding_result_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = VLinkCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini Search Grounding",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VLinkCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = VLinkCyan
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synthesizing live Google Search data...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (response != null) {
                Text(
                    text = "Query: \"${response.query}\"",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = response.answer,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (response.sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Google Search Sources & Citations:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    response.sources.forEach { source ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, VLinkCyan.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenSource(source) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = VLinkCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = source.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = VLinkCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Ask any question to get up-to-date answers and real-time links grounded by Google Search data.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionPromptChip("⚽ Champions League scores") { onSearchPrompt(it) }
                    SuggestionPromptChip("🚀 Latest AI developments") { onSearchPrompt(it) }
                    SuggestionPromptChip("📈 World market trends") { onSearchPrompt(it) }
                    SuggestionPromptChip("🏎️ Formula 1 race results") { onSearchPrompt(it) }
                }
            }
        }
    }
}

@Composable
fun SuggestionPromptChip(
    text: String,
    onClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(text.substring(2)) }
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
