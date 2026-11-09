package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.RepeatModeUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.ChannelEngagementStats
import com.example.data.model.ChannelServer
import com.example.data.model.PlaybackRequest
import com.example.data.model.PlayerGuideCategory
import com.example.data.model.PlayerGuideChannel
import com.example.ui.player.PlaybackRecoveryManager
import com.example.ui.player.PlayerDiagnosticEvent
import com.example.ui.player.PlayerDiagnostics
import com.example.ui.player.RecoveryDecision
import com.example.ui.player.SeriesEpisodeNavigator
import com.example.ui.player.StreamSourceResolver
import com.example.ui.player.UnifiedPlayerManager
import com.example.ui.player.VodPlayerPreferencesManager
import com.example.ui.player.WatchProgressManager
import com.example.ui.player.WatchdogPlayerSnapshot
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG_CUSTOM_VOLUME_BTN = "exo_custom_volume_btn"
private const val TAG_CUSTOM_PIP_BTN = "exo_custom_pip_btn"
private const val TAG_CUSTOM_ASPECT_BTN = "exo_custom_aspect_btn"
private const val TAG_CUSTOM_MULTIVIEW_BTN = "exo_custom_multiview_btn"

@OptIn(UnstableApi::class)
@Composable
fun LibraryVideoPlayerScreen(
    playbackRequest: PlaybackRequest,
    channelStats: ChannelEngagementStats? = null,
    showChannelViewsAndLikes: Boolean = true,
    guideCategories: List<PlayerGuideCategory> = emptyList(),
    guideChannels: List<PlayerGuideChannel> = emptyList(),
    onSelectGuideChannel: (PlayerGuideChannel) -> Unit = {},
    onToggleChannelLike: () -> Unit = {},
    onSelectVodEpisode: ((com.example.data.model.SeriesEpisode, Int) -> Unit)? = null,
    onSelectVodRecommendedItem: ((com.example.data.model.LibraryMediaItem) -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize preferences & watch progress for Library VOD items
    remember(context) {
        VodPlayerPreferencesManager.initialize(context)
        WatchProgressManager.initialize(context)
        true
    }
    val vodPrefs by VodPlayerPreferencesManager.preferencesFlow.collectAsState()
    val seekStepMs = remember(vodPrefs.forwardSeekSeconds) {
        vodPrefs.forwardSeekMs
    }

    // Lock into Landscape mode, keep screen awake, and hide system bars (Immersive Fullscreen)
    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else null
        insetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = originalOrientation
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Normalize server list so both Live Channels and Library (Movies/Series) always have structured servers for the Top Bar
    val displayServers: List<ChannelServer> = remember(playbackRequest) {
        val explicitServers = playbackRequest.servers.filter { it.streamUrl.isNotBlank() }
        if (explicitServers.isNotEmpty()) {
            explicitServers
        } else {
            playbackRequest.candidateUrls
                .filter { it.isNotBlank() }
                .mapIndexed { idx, url ->
                    val ext = url.substringBefore('?').substringAfterLast('.', "").uppercase()
                    ChannelServer(
                        id = "srv_${idx + 1}",
                        name = "سيرفر ${idx + 1}",
                        streamType = ext.lowercase().ifBlank {
                            if (playbackRequest.isLiveChannel) "m3u8" else "mp4"
                        },
                        streamUrl = url.trim()
                    )
                }
        }
    }

    val candidateUrls = remember(displayServers, playbackRequest.candidateUrls) {
        if (displayServers.isNotEmpty()) {
            displayServers.map { it.streamUrl.trim() }
        } else {
            playbackRequest.candidateUrls.map { it.trim() }.filter { it.isNotBlank() }
        }
    }

    // Unified Media3 ExoPlayer instance + Automatic Recovery Manager
    val playerManager = remember(context) { UnifiedPlayerManager(context) }
    val exoPlayer = playerManager.exoPlayer
    val recoveryManager = remember { PlaybackRecoveryManager(maxRetriesPerServer = 2) }
    val coroutineScope = rememberCoroutineScope()
    var scheduledRecoveryJob by remember { mutableStateOf<Job?>(null) }

    val initialServerIndex = remember(playbackRequest, displayServers, playerManager.deviceCapabilities) {
        StreamSourceResolver.selectOptimalInitialServerIndex(
            servers = displayServers,
            isLiveChannel = playbackRequest.isLiveChannel,
            deviceCapabilities = playerManager.deviceCapabilities
        )
    }
    var currentUrlIndex by remember(playbackRequest) { mutableIntStateOf(initialServerIndex) }
    var reloadTrigger by remember(playbackRequest) { mutableIntStateOf(0) }
    var downgradeBitrateOnReload by remember(playbackRequest, currentUrlIndex) { mutableStateOf(false) }
    var allExtensionsFailed by remember(playbackRequest) { mutableStateOf(false) }
    var errorDescriptionAr by remember(playbackRequest) { mutableStateOf("تعذر تشغيل هذا المصدر حالياً") }

    var isMuted by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Synchronized visibility & alpha with Media3 PlayerView's internal controls
    var isControllerVisible by remember { mutableStateOf(true) }
    var controllerAlpha by remember { mutableFloatStateOf(1f) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // FocusRequesters for seamless Android TV D-Pad navigation between Top Bar and PlayerView
    val topBackFocusRequester = remember { FocusRequester() }
    val activeServerFocusRequester = remember { FocusRequester() }

    // Library VOD watch progress restoration & persistence
    val initialResumePositionMs = remember(playbackRequest) {
        if (playbackRequest.isLiveChannel) {
            C.TIME_UNSET
        } else {
            val mediaId = playbackRequest.mediaId.ifBlank {
                playbackRequest.channelId.ifBlank { playbackRequest.title }
            }
            val existing = if (playbackRequest.isSeries) {
                WatchProgressManager.getEpisodeProgress(
                    accountId = playbackRequest.accountId,
                    seriesId = playbackRequest.seriesId.ifBlank { mediaId },
                    seasonNumber = playbackRequest.seasonNumber,
                    episodeNumber = playbackRequest.episodeNumber,
                    episodeId = playbackRequest.episodeId.ifBlank { mediaId }
                )
            } else {
                WatchProgressManager.getMovieProgress(
                    accountId = playbackRequest.accountId,
                    movieId = mediaId
                )
            }
            if (WatchProgressManager.shouldOfferResume(existing) && existing != null) {
                existing.positionMs
            } else {
                C.TIME_UNSET
            }
        }
    }
    var lastKnownValidPositionMs by remember(playbackRequest) {
        mutableLongStateOf(if (initialResumePositionMs > 0L) initialResumePositionMs else 0L)
    }

    val persistVodProgressIfNeeded: () -> Unit = {
        if (!playbackRequest.isLiveChannel && !playerManager.isReleased()) {
            val pos = exoPlayer.currentPosition.takeIf { it > 0L } ?: lastKnownValidPositionMs
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (pos >= WatchProgressManager.MIN_SAVE_POSITION_MS) {
                lastKnownValidPositionMs = pos
                val mediaId = playbackRequest.mediaId.ifBlank {
                    playbackRequest.channelId.ifBlank { playbackRequest.title }
                }
                WatchProgressManager.saveProgress(
                    accountId = playbackRequest.accountId,
                    mediaId = mediaId,
                    title = playbackRequest.title,
                    posterUrl = playbackRequest.posterUrl,
                    backdropUrl = playbackRequest.backdropUrl,
                    containerExtension = playbackRequest.containerExtension,
                    isSeries = playbackRequest.isSeries,
                    seriesId = playbackRequest.seriesId.ifBlank { mediaId },
                    seasonNumber = playbackRequest.seasonNumber,
                    episodeNumber = playbackRequest.episodeNumber,
                    episodeId = playbackRequest.episodeId,
                    episodeTitle = playbackRequest.episodeTitle.ifBlank { playbackRequest.subtitle },
                    positionMs = pos,
                    durationMs = dur
                )
            }
        }
    }

    // Series episode neighbours for Library series navigation
    val episodeNeighbours = remember(
        playbackRequest.isLiveChannel,
        playbackRequest.isSeries,
        playbackRequest.seasons,
        playbackRequest.episodesBySeason,
        playbackRequest.seasonNumber,
        playbackRequest.episodeId,
        playbackRequest.episodeNumber
    ) {
        if (!playbackRequest.isLiveChannel && playbackRequest.isSeries) {
            SeriesEpisodeNavigator.resolveNeighbours(
                seasons = playbackRequest.seasons,
                episodesBySeason = playbackRequest.episodesBySeason,
                currentSeasonNumber = playbackRequest.seasonNumber,
                currentEpisodeId = playbackRequest.episodeId,
                currentEpisodeNumber = playbackRequest.episodeNumber
            )
        } else {
            null
        }
    }

    BackHandler {
        persistVodProgressIfNeeded()
        onBack()
    }

    val resolvedActiveStreamUrl = remember(candidateUrls, currentUrlIndex) {
        if (currentUrlIndex !in candidateUrls.indices) {
            ""
        } else {
            candidateUrls[currentUrlIndex].trim()
        }
    }
    val activeServerTypeHint = remember(displayServers, currentUrlIndex) {
        displayServers.getOrNull(currentUrlIndex)?.streamType.orEmpty()
    }
    val activeServerNameHint = remember(displayServers, currentUrlIndex) {
        displayServers.getOrNull(currentUrlIndex)?.name.orEmpty()
    }
    val activeSourceType = remember(
        resolvedActiveStreamUrl,
        activeServerTypeHint,
        activeServerNameHint,
        playbackRequest.isLiveChannel
    ) {
        StreamSourceResolver.detectStreamSourceType(
            url = resolvedActiveStreamUrl,
            serverStreamTypeHint = activeServerTypeHint,
            serverNameHint = activeServerNameHint,
            isLiveChannel = playbackRequest.isLiveChannel
        )
    }

    LaunchedEffect(playbackRequest) {
        scheduledRecoveryJob?.cancel()
        recoveryManager.onChannelOrManualServerSelected(
            initialServerIndex = initialServerIndex,
            totalServers = candidateUrls.size.coerceAtLeast(1)
        )
    }

    val selectServerManually: (Int) -> Unit = { targetIndex ->
        if (targetIndex in candidateUrls.indices) {
            if (!playbackRequest.isLiveChannel && !playerManager.isReleased()) {
                val pos = exoPlayer.currentPosition
                if (pos > 0L) {
                    lastKnownValidPositionMs = pos
                }
            }
            scheduledRecoveryJob?.cancel()
            allExtensionsFailed = false
            downgradeBitrateOnReload = false
            recoveryManager.onChannelOrManualServerSelected(
                initialServerIndex = targetIndex,
                totalServers = candidateUrls.size.coerceAtLeast(1)
            )
            if (currentUrlIndex == targetIndex) {
                reloadTrigger += 1
            } else {
                currentUrlIndex = targetIndex
            }
            playerViewRef?.showController()
        }
    }

    LaunchedEffect(resolvedActiveStreamUrl, reloadTrigger, allExtensionsFailed, playbackRequest) {
        if (playerManager.isReleased()) return@LaunchedEffect
        if (!allExtensionsFailed && resolvedActiveStreamUrl.isNotBlank()) {
            playerManager.applyQualityMode(
                isAutoQuality = true,
                isDataSaver = false
            )
            val normalizedUrl = if (resolvedActiveStreamUrl.startsWith("/")) {
                Uri.fromFile(File(resolvedActiveStreamUrl)).toString()
            } else {
                resolvedActiveStreamUrl
            }
            val startPos = if (playbackRequest.isLiveChannel) {
                C.TIME_UNSET
            } else if (lastKnownValidPositionMs > 0L) {
                lastKnownValidPositionMs
            } else {
                initialResumePositionMs
            }

            playerManager.loadStream(
                url = normalizedUrl,
                serverIndex = currentUrlIndex,
                serverStreamTypeHint = activeServerTypeHint,
                serverNameHint = activeServerNameHint,
                isLiveChannel = playbackRequest.isLiveChannel,
                startPositionMs = startPos,
                playImmediately = true,
                downgradeBitrateCap = downgradeBitrateOnReload,
                channelId = playbackRequest.mediaId.ifBlank { playbackRequest.channelId }
            )
        } else if (candidateUrls.isEmpty()) {
            allExtensionsFailed = true
        }
    }

    // Periodically save VOD watch progress every 5 seconds during active playback
    LaunchedEffect(playerManager, playbackRequest) {
        if (playbackRequest.isLiveChannel) return@LaunchedEffect
        while (!playerManager.isReleased()) {
            delay(5_000L)
            if (playerManager.isReleased()) break
            if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                if (pos > 0L) {
                    lastKnownValidPositionMs = pos
                    persistVodProgressIfNeeded()
                }
            }
        }
    }

    val executeRecoveryDecision: (RecoveryDecision) -> Unit = { decision ->
        when (decision) {
            is RecoveryDecision.None -> Unit

            is RecoveryDecision.SeekToDefaultLiveEdge -> {
                scheduledRecoveryJob?.cancel()
                if (playbackRequest.isLiveChannel) {
                    playerManager.seekToLiveEdgeNonDestructive()
                } else {
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                }
            }

            is RecoveryDecision.ReloadCurrentServer -> {
                scheduledRecoveryJob?.cancel()
                if (!playbackRequest.isLiveChannel && !playerManager.isReleased()) {
                    val pos = exoPlayer.currentPosition
                    if (pos > 0L) lastKnownValidPositionMs = pos
                }
                scheduledRecoveryJob = coroutineScope.launch {
                    if (decision.delayMs > 0L) delay(decision.delayMs)
                    if (!playerManager.isReleased()) {
                        recoveryManager.onRecoveryDispatched()
                        downgradeBitrateOnReload = decision.downgradeBitrateHint
                        reloadTrigger += 1
                    }
                }
            }

            is RecoveryDecision.SwitchToNextServer -> {
                scheduledRecoveryJob?.cancel()
                if (!playbackRequest.isLiveChannel && !playerManager.isReleased()) {
                    val pos = exoPlayer.currentPosition
                    if (pos > 0L) lastKnownValidPositionMs = pos
                }
                scheduledRecoveryJob = coroutineScope.launch {
                    if (decision.delayMs > 0L) delay(decision.delayMs)
                    if (!playerManager.isReleased()) {
                        recoveryManager.onAutoServerSwitched(
                            newServerIndex = decision.nextServerIndex,
                            totalServers = candidateUrls.size.coerceAtLeast(1)
                        )
                        downgradeBitrateOnReload = false
                        currentUrlIndex = decision.nextServerIndex
                    }
                }
            }

            is RecoveryDecision.AllServersExhausted -> {
                scheduledRecoveryJob?.cancel()
                errorDescriptionAr = decision.userMessageAr
                allExtensionsFailed = true
            }
        }
    }

    val latestPlaybackRequest by rememberUpdatedState(playbackRequest)
    val latestCandidateUrls by rememberUpdatedState(candidateUrls)
    val latestCurrentUrlIndex by rememberUpdatedState(currentUrlIndex)
    val latestResolvedUrl by rememberUpdatedState(resolvedActiveStreamUrl)
    val latestSourceType by rememberUpdatedState(activeSourceType)
    val latestExecuteRecovery by rememberUpdatedState(executeRecoveryDecision)
    val latestGuideChannels by rememberUpdatedState(guideChannels)
    val latestOnSelectGuideChannel by rememberUpdatedState(onSelectGuideChannel)
    val latestEpisodeNeighbours by rememberUpdatedState(episodeNeighbours)
    val latestOnSelectVodEpisode by rememberUpdatedState(onSelectVodEpisode)
    val latestSeekStepMs by rememberUpdatedState(seekStepMs)
    val latestPersistVodProgress by rememberUpdatedState(persistVodProgressIfNeeded)

    // Lifecycle handling (keep playing in Picture-in-Picture, pause & save progress when backgrounded)
    DisposableEffect(lifecycleOwner, playerManager) {
        var wasPlayingBeforePause = true
        var handledBackground = false
        val observer = LifecycleEventObserver { _, event ->
            if (playerManager.isReleased()) return@LifecycleEventObserver
            val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    if (!inPip && !handledBackground) {
                        wasPlayingBeforePause = exoPlayer.playWhenReady
                        handledBackground = true
                        latestPersistVodProgress()
                        exoPlayer.playWhenReady = false
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (handledBackground) {
                        handledBackground = false
                        if (wasPlayingBeforePause && !allExtensionsFailed) {
                            exoPlayer.playWhenReady = true
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(playerManager) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playerManager.isReleased()) return
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        PlayerDiagnostics.record(
                            event = PlayerDiagnosticEvent.BUFFERING_STARTED,
                            rawUrl = latestResolvedUrl,
                            serverIndex = latestCurrentUrlIndex,
                            sourceType = latestSourceType.label
                        )
                    }
                    Player.STATE_READY -> {
                        scheduledRecoveryJob?.cancel()
                        recoveryManager.onPlaybackReady(
                            currentUrl = latestResolvedUrl,
                            sourceType = latestSourceType
                        )
                        allExtensionsFailed = false
                        if (!latestPlaybackRequest.isLiveChannel && exoPlayer.currentPosition > 0L) {
                            lastKnownValidPositionMs = exoPlayer.currentPosition
                        }
                        // Ensure controller auto-hide timer is armed once playback is ready (since showController during STATE_IDLE sets timeout=0)
                        playerViewRef?.let { pv ->
                            pv.controllerShowTimeoutMs = 3500
                            if (pv.isControllerFullyVisible) {
                                pv.showController()
                            }
                        }
                        PlayerDiagnostics.record(
                            event = PlayerDiagnosticEvent.BUFFERING_ENDED,
                            rawUrl = latestResolvedUrl,
                            serverIndex = latestCurrentUrlIndex,
                            sourceType = latestSourceType.label
                        )
                        PlayerDiagnostics.record(
                            event = PlayerDiagnosticEvent.PLAYBACK_READY,
                            rawUrl = latestResolvedUrl,
                            serverIndex = latestCurrentUrlIndex,
                            sourceType = latestSourceType.label,
                            details = "video=${exoPlayer.videoSize.width}x${exoPlayer.videoSize.height}"
                        )
                    }
                    Player.STATE_ENDED -> {
                        if (latestPlaybackRequest.isLiveChannel) {
                            val snapshot = WatchdogPlayerSnapshot(
                                playbackState = playbackState,
                                playWhenReady = exoPlayer.playWhenReady,
                                isPlaying = exoPlayer.isPlaying,
                                isLoading = exoPlayer.isLoading,
                                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                                bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L),
                                isLiveChannel = true,
                                currentServerIndex = latestCurrentUrlIndex,
                                totalServersCount = latestCandidateUrls.size.coerceAtLeast(1),
                                currentUrl = latestResolvedUrl,
                                sourceType = latestSourceType,
                                nowRealtimeMs = System.currentTimeMillis()
                            )
                            latestExecuteRecovery(recoveryManager.evaluateWatchdogTick(snapshot))
                        } else {
                            latestPersistVodProgress()
                            val nextEp = latestEpisodeNeighbours?.nextEpisode
                            val nextSeason = latestEpisodeNeighbours?.nextSeasonNumber
                            val epCallback = latestOnSelectVodEpisode
                            if (nextEp != null && nextSeason != null && epCallback != null) {
                                epCallback(nextEp, nextSeason)
                            }
                        }
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (playerManager.isReleased()) return
                val decision = recoveryManager.onPlayerError(
                    error = error,
                    currentUrl = latestResolvedUrl,
                    sourceType = latestSourceType,
                    isLiveChannel = latestPlaybackRequest.isLiveChannel,
                    totalServers = latestCandidateUrls.size.coerceAtLeast(1)
                )
                latestExecuteRecovery(decision)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            latestPersistVodProgress()
            scheduledRecoveryJob?.cancel()
            exoPlayer.removeListener(listener)
            playerManager.release()
        }
    }

    // Periodic Non-Destructive Watchdog
    LaunchedEffect(playerManager, playbackRequest, currentUrlIndex, allExtensionsFailed) {
        if (allExtensionsFailed || candidateUrls.isEmpty()) return@LaunchedEffect
        while (!playerManager.isReleased() && !allExtensionsFailed) {
            delay(1_000L)
            if (playerManager.isReleased() || allExtensionsFailed) break
            if (scheduledRecoveryJob?.isActive == true) continue

            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (!playbackRequest.isLiveChannel && pos > 0L) {
                lastKnownValidPositionMs = pos
            }

            val snapshot = WatchdogPlayerSnapshot(
                playbackState = exoPlayer.playbackState,
                playWhenReady = exoPlayer.playWhenReady,
                isPlaying = exoPlayer.isPlaying,
                isLoading = exoPlayer.isLoading,
                currentPositionMs = pos,
                bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L),
                isLiveChannel = playbackRequest.isLiveChannel,
                currentServerIndex = currentUrlIndex,
                totalServersCount = candidateUrls.size.coerceAtLeast(1),
                currentUrl = resolvedActiveStreamUrl,
                sourceType = activeSourceType,
                nowRealtimeMs = System.currentTimeMillis()
            )
            val decision = recoveryManager.evaluateWatchdogTick(snapshot)
            if (decision !is RecoveryDecision.None) {
                executeRecoveryDecision(decision)
            }
        }
    }

    val handlePreviousAction: () -> Unit = {
        if (latestPlaybackRequest.isLiveChannel) {
            val list = latestGuideChannels
            if (list.size > 1) {
                val currentIdx = list.indexOfFirst { it.id == latestPlaybackRequest.channelId }
                    .takeIf { it >= 0 } ?: 0
                val prevIdx = (currentIdx - 1 + list.size) % list.size
                latestOnSelectGuideChannel(list[prevIdx])
            } else {
                exoPlayer.seekToDefaultPosition()
            }
        } else {
            val prevEp = latestEpisodeNeighbours?.previousEpisode
            val prevSeason = latestEpisodeNeighbours?.previousSeasonNumber
            val epCallback = latestOnSelectVodEpisode
            if (prevEp != null && prevSeason != null && epCallback != null && exoPlayer.currentPosition < 5_000L) {
                latestPersistVodProgress()
                epCallback(prevEp, prevSeason)
            } else {
                exoPlayer.seekTo(0L)
                lastKnownValidPositionMs = 0L
            }
        }
    }

    val handleNextAction: () -> Unit = {
        if (latestPlaybackRequest.isLiveChannel) {
            val list = latestGuideChannels
            if (list.size > 1) {
                val currentIdx = list.indexOfFirst { it.id == latestPlaybackRequest.channelId }
                    .takeIf { it >= 0 } ?: 0
                val nextIdx = (currentIdx + 1) % list.size
                latestOnSelectGuideChannel(list[nextIdx])
            } else {
                exoPlayer.seekToDefaultPosition()
            }
        } else {
            val nextEp = latestEpisodeNeighbours?.nextEpisode
            val nextSeason = latestEpisodeNeighbours?.nextSeasonNumber
            val epCallback = latestOnSelectVodEpisode
            if (nextEp != null && nextSeason != null && epCallback != null) {
                latestPersistVodProgress()
                epCallback(nextEp, nextSeason)
            } else {
                val dur = exoPlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                val target = (exoPlayer.currentPosition + latestSeekStepMs).coerceAtMost(dur)
                exoPlayer.seekTo(target)
            }
        }
    }

    val requestFocusOnTopBar: () -> Unit = {
        playerViewRef?.showController()
        runCatching {
            if (displayServers.isNotEmpty()) {
                activeServerFocusRequester.requestFocus()
            } else {
                topBackFocusRequester.requestFocus()
            }
        }
    }

    val requestFocusOnCenterPlayPause: () -> Unit = {
        playerViewRef?.showController()
        playerViewRef?.findViewById<View>(androidx.media3.ui.R.id.exo_play_pause)?.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                val nativeEvent = keyEvent.nativeKeyEvent
                if (nativeEvent.action == KeyEvent.ACTION_DOWN) {
                    when (nativeEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!isControllerVisible || controllerAlpha < 0.1f) {
                                playerViewRef?.showController()
                                isControllerVisible = true
                                controllerAlpha = 1f
                                requestFocusOnCenterPlayPause()
                                return@onPreviewKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_CHANNEL_UP -> {
                            handleNextAction()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            handlePreviousAction()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            playerViewRef?.showController()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            }
            .testTag("media3_styled_player_root")
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("media3_styled_player_view"),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    playerViewRef = this
                    player = exoPlayer
                    useController = true
                    controllerAutoShow = true
                    controllerShowTimeoutMs = 3500
                    controllerHideOnTouch = true
                    this.resizeMode = resizeMode
                    setKeepContentOnPlayerReset(true)
                    setShutterBackgroundColor(AndroidColor.BLACK)
                    setBackgroundColor(AndroidColor.BLACK)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)

                    // Sync Top Server Bar & Back Arrow visibility with Media3 PlayerView controller
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            val vis = (visibility == View.VISIBLE)
                            isControllerVisible = vis
                            controllerAlpha = if (vis) 1f else 0f
                        }
                    )

                    // Enable standard Media3 StyledPlayerView controls shown in screenshot
                    setShowShuffleButton(true)
                    setRepeatToggleModes(
                        RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE or RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
                    )
                    setShowSubtitleButton(true)
                    setShowPreviousButton(true)
                    setShowNextButton(true)
                    setShowRewindButton(true)
                    setShowFastForwardButton(true)

                    setFullscreenButtonClickListener {
                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                            AspectRatioFrameLayout.RESIZE_MODE_FILL
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        this.resizeMode = resizeMode
                        activity?.window?.let { win ->
                            WindowCompat.getInsetsController(win, win.decorView)
                                .hide(WindowInsetsCompat.Type.systemBars())
                        }
                    }

                    configureStyledPlayerViewControls(
                        playerView = this,
                        isMutedProvider = { isMuted },
                        onControllerVisualStateChanged = { visible, alpha ->
                            if (isControllerVisible != visible) {
                                isControllerVisible = visible
                            }
                            if (abs(controllerAlpha - alpha) > 0.02f) {
                                controllerAlpha = alpha
                            }
                        },
                        onDpadUpFromCenterControls = {
                            requestFocusOnTopBar()
                        },
                        onToggleMute = { btn ->
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                            btn.setImageResource(
                                if (isMuted) R.drawable.ic_exo_volume_off else R.drawable.ic_exo_volume_on
                            )
                        },
                        onEnterPip = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                runCatching {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity.enterPictureInPictureMode(params)
                                }
                            }
                        },
                        onCycleAspectRatio = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            this.resizeMode = resizeMode
                            val modeLabel = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> "ملء الشاشة"
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "تكبير"
                                else -> "الوضع الأصلي"
                            }
                            Toast.makeText(ctx, modeLabel, Toast.LENGTH_SHORT).show()
                        },
                        onMultiViewOrServerClick = {
                            if (candidateUrls.size > 1) {
                                val nextServerIdx = (latestCurrentUrlIndex + 1) % candidateUrls.size
                                selectServerManually(nextServerIdx)
                            } else {
                                reloadTrigger += 1
                            }
                        },
                        onRewindClicked = {
                            if (!latestPlaybackRequest.isLiveChannel || exoPlayer.isCurrentMediaItemSeekable) {
                                val target = (exoPlayer.currentPosition - latestSeekStepMs).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                if (!latestPlaybackRequest.isLiveChannel) {
                                    lastKnownValidPositionMs = target
                                }
                            }
                        },
                        onFastForwardClicked = {
                            if (!latestPlaybackRequest.isLiveChannel || exoPlayer.isCurrentMediaItemSeekable) {
                                val dur = exoPlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                val target = (exoPlayer.currentPosition + latestSeekStepMs).coerceAtMost(dur)
                                exoPlayer.seekTo(target)
                                if (!latestPlaybackRequest.isLiveChannel) {
                                    lastKnownValidPositionMs = target
                                }
                            }
                        },
                        onPreviousClicked = {
                            handlePreviousAction()
                        },
                        onNextClicked = {
                            handleNextAction()
                        }
                    )
                }
            },
            update = { playerView ->
                playerViewRef = playerView
                if (playerView.player !== exoPlayer) {
                    playerView.player = exoPlayer
                }
                if (playerView.resizeMode != resizeMode) {
                    playerView.resizeMode = resizeMode
                }
                val volumeBtn = playerView.findViewWithTag<ImageButton>(TAG_CUSTOM_VOLUME_BTN)
                volumeBtn?.setImageResource(
                    if (isMuted) R.drawable.ic_exo_volume_off else R.drawable.ic_exo_volume_on
                )
            }
        )

        // Integrated Top Server Bar & Back Arrow — synchronized frame-by-frame with Media3 PlayerView controls
        if (isControllerVisible && controllerAlpha > 0.02f) {
            var isBackFocused by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val clampedAlpha = controllerAlpha.coerceIn(0f, 1f)
                        alpha = clampedAlpha
                        translationY = -(1f - clampedAlpha) * 28f
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.84f),
                                Color.Black.copy(alpha = 0.44f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            playerViewRef?.showController()
                            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                requestFocusOnCenterPlayPause()
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    }
                    .testTag("player_top_servers_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isBackFocused) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.14f)
                    },
                    border = BorderStroke(
                        width = if (isBackFocused) 2.dp else 1.dp,
                        color = if (isBackFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .scale(if (isBackFocused) 1.12f else 1f)
                        .focusRequester(topBackFocusRequester)
                        .onFocusChanged { state ->
                            isBackFocused = state.isFocused
                            if (state.isFocused) {
                                playerViewRef?.showController()
                            }
                        }
                        .focusable()
                        .clickable {
                            persistVodProgressIfNeeded()
                            onBack()
                        }
                        .testTag("player_top_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = if (isBackFocused) Color.Black else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                if (playbackRequest.title.isNotBlank()) {
                    Text(
                        text = playbackRequest.title,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.35f, fill = false)
                    )
                }

                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_top_servers_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(displayServers) { index, server ->
                        val isSelected = index == currentUrlIndex
                        var isChipFocused by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when {
                                isSelected -> Color.White
                                isChipFocused -> Color(0xFF2B2B2B)
                                else -> Color(0xFF1A1A1A).copy(alpha = 0.85f)
                            },
                            border = BorderStroke(
                                width = if (isChipFocused) 2.dp else 1.dp,
                                color = when {
                                    isChipFocused -> Color(0xFF00E5FF)
                                    isSelected -> Color.White
                                    else -> Color.White.copy(alpha = 0.24f)
                                }
                            ),
                            modifier = Modifier
                                .scale(if (isChipFocused) 1.08f else 1f)
                                .then(
                                    if (isSelected) {
                                        Modifier.focusRequester(activeServerFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .onFocusChanged { state ->
                                    isChipFocused = state.isFocused
                                    if (state.isFocused) {
                                        playerViewRef?.showController()
                                    }
                                }
                                .focusable()
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    selectServerManually(index)
                                }
                                .testTag("top_bar_server_chip_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00C853))
                                    )
                                }
                                Text(
                                    text = cleanServerDisplayName(server.name, index),
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || isChipFocused) {
                                        FontWeight.ExtraBold
                                    } else {
                                        FontWeight.SemiBold
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (allExtensionsFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = errorDescriptionAr,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                allExtensionsFailed = false
                                recoveryManager.onChannelOrManualServerSelected(
                                    initialServerIndex = currentUrlIndex,
                                    totalServers = candidateUrls.size.coerceAtLeast(1)
                                )
                                reloadTrigger += 1
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("live_player_retry_button")
                        ) {
                            Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                        }
                        if (displayServers.size > 1) {
                            Button(
                                onClick = {
                                    val nextIdx = (currentUrlIndex + 1) % displayServers.size
                                    selectServerManually(nextIdx)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF222222),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("السيرفر التالي", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(
                            onClick = {
                                persistVodProgressIfNeeded()
                                onBack()
                            }
                        ) {
                            Text("رجوع", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Configures the internal AndroidX Media3 [PlayerView] (`StyledPlayerView`) controls
 * with full Android TV D-Pad remote navigation and frame-accurate visibility/alpha tracking
 * so the Top Server Bar and Back Arrow fade in and out in complete sync with the player controls.
 */
@OptIn(UnstableApi::class)
private fun configureStyledPlayerViewControls(
    playerView: PlayerView,
    isMutedProvider: () -> Boolean,
    onControllerVisualStateChanged: (visible: Boolean, alpha: Float) -> Unit,
    onDpadUpFromCenterControls: () -> Unit,
    onToggleMute: (ImageButton) -> Unit,
    onEnterPip: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onMultiViewOrServerClick: () -> Unit,
    onRewindClicked: () -> Unit,
    onFastForwardClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onNextClicked: () -> Unit
) {
    val context = playerView.context

    // 1. Style the full-width thin DefaultTimeBar
    val timeBar = playerView.findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress)
    timeBar?.apply {
        setPlayedColor(AndroidColor.WHITE)
        setScrubberColor(AndroidColor.WHITE)
        setBufferedColor(0x66FFFFFF)
        setUnplayedColor(0x33FFFFFF)
        isFocusable = true
    }

    val controllerRoot = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_controller)
    val centerControls = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_center_controls)
    val controlsBg = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_controls_background)
    val bottomBar = playerView.findViewById<FrameLayout>(androidx.media3.ui.R.id.exo_bottom_bar)

    // Track real-time visibility & fade alpha of Media3's controller and center buttons so Top Server Bar + Back Arrow
    // appear and disappear at the exact same millisecond as the player's buttons.
    val syncVisualState = {
        val isRootShown = controllerRoot != null &&
            controllerRoot.visibility == View.VISIBLE &&
            controllerRoot.isShown
        val isCenterShown = centerControls == null ||
            (centerControls.visibility == View.VISIBLE && centerControls.isShown)

        val currentAlpha = if (!isRootShown || !isCenterShown) {
            0f
        } else {
            val cAlpha = centerControls?.alpha ?: 1f
            val bgAlpha = if (controlsBg != null && controlsBg.visibility == View.VISIBLE) {
                controlsBg.alpha
            } else {
                cAlpha
            }
            val rootAlpha = controllerRoot?.alpha ?: 1f
            (minOf(cAlpha, bgAlpha) * rootAlpha).coerceIn(0f, 1f)
        }
        val controllerShown = isRootShown && isCenterShown && currentAlpha > 0.02f
        onControllerVisualStateChanged(controllerShown, currentAlpha)
    }

    playerView.viewTreeObserver.addOnPreDrawListener(
        object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!playerView.isAttachedToWindow) return true
                syncVisualState()
                return true
            }
        }
    )

    // Helper for TV D-Pad Up key on center controls -> jumps directly to Top Server Bar / Back Arrow
    val centerControlKeyListener = View.OnKeyListener { _, keyCode, event ->
        if (event.action == KeyEvent.ACTION_DOWN) {
            playerView.showController()
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                onDpadUpFromCenterControls()
                return@OnKeyListener true
            }
        }
        false
    }

    // 2. Style the Center Transport Controls (Prev, Rewind, White Circle Play/Pause, FastForward, Next)
    val playPauseBtn = playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_play_pause)
    playPauseBtn?.apply {
        background = createPlayPauseCircleDrawable(focused = false)
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(AndroidColor.BLACK))
        scaleType = ImageView.ScaleType.FIT_CENTER
        isFocusable = true
        isFocusableInTouchMode = false
        val sizePx = dpToPx(context, 54)
        val padPx = dpToPx(context, 13)
        setPadding(padPx, padPx, padPx, padPx)
        layoutParams = (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            width = sizePx
            height = sizePx
            marginStart = dpToPx(context, 18)
            marginEnd = dpToPx(context, 18)
        } ?: LinearLayout.LayoutParams(sizePx, sizePx).apply {
            gravity = Gravity.CENTER
            marginStart = dpToPx(context, 18)
            marginEnd = dpToPx(context, 18)
        }
        setOnFocusChangeListener { v, hasFocus ->
            v.scaleX = if (hasFocus) 1.16f else 1f
            v.scaleY = if (hasFocus) 1.16f else 1f
            v.background = createPlayPauseCircleDrawable(focused = hasFocus)
            if (hasFocus) playerView.showController()
        }
        setOnKeyListener(centerControlKeyListener)
    }

    // Style Rewind & FastForward as clean circular arrows without numbers inside
    val rewBtn = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_rew_with_amount)
        ?: playerView.findViewById(androidx.media3.ui.R.id.exo_rew)
    rewBtn?.apply {
        if (this is TextView) {
            text = ""
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 0f)
            setTextColor(AndroidColor.TRANSPARENT)
            setCompoundDrawables(null, null, null, null)
        }
        background = ContextCompat.getDrawable(context, R.drawable.ic_exo_rewind_clean)
        isFocusable = true
        val sizePx = dpToPx(context, 34)
        layoutParams = (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            width = sizePx
            height = sizePx
            marginStart = dpToPx(context, 14)
            marginEnd = dpToPx(context, 14)
        } ?: LinearLayout.LayoutParams(sizePx, sizePx).apply {
            gravity = Gravity.CENTER
        }
        attachTvFocusEffect(this, playerView)
        setOnKeyListener(centerControlKeyListener)
        setOnClickListener {
            playerView.showController()
            onRewindClicked()
        }
    }

    val ffwdBtn = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_ffwd_with_amount)
        ?: playerView.findViewById(androidx.media3.ui.R.id.exo_ffwd)
    ffwdBtn?.apply {
        if (this is TextView) {
            text = ""
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 0f)
            setTextColor(AndroidColor.TRANSPARENT)
            setCompoundDrawables(null, null, null, null)
        }
        background = ContextCompat.getDrawable(context, R.drawable.ic_exo_fastforward_clean)
        isFocusable = true
        val sizePx = dpToPx(context, 34)
        layoutParams = (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            width = sizePx
            height = sizePx
            marginStart = dpToPx(context, 14)
            marginEnd = dpToPx(context, 14)
        } ?: LinearLayout.LayoutParams(sizePx, sizePx).apply {
            gravity = Gravity.CENTER
        }
        attachTvFocusEffect(this, playerView)
        setOnKeyListener(centerControlKeyListener)
        setOnClickListener {
            playerView.showController()
            onFastForwardClicked()
        }
    }

    val prevBtn = playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_prev)
    prevBtn?.apply {
        visibility = View.VISIBLE
        isEnabled = true
        isFocusable = true
        alpha = 1.0f
        attachTvFocusEffect(this, playerView)
        setOnKeyListener(centerControlKeyListener)
        setOnClickListener {
            playerView.showController()
            onPreviousClicked()
        }
    }

    val nextBtn = playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_next)
    nextBtn?.apply {
        visibility = View.VISIBLE
        isFocusable = true
        attachTvFocusEffect(this, playerView)
        setOnKeyListener(centerControlKeyListener)
        setOnClickListener {
            playerView.showController()
            onNextClicked()
        }
    }

    // 3. Configure Bottom Bar Right Icons in exact screenshot order:
    // [Shuffle] [Repeat] [Volume] [Settings] [PiP] [CC] [Aspect Ratio <->] [Multi-view] [Fullscreen]
    val basicControls = playerView.findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_basic_controls)
        ?: return

    val shuffleBtn = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_shuffle)
    val repeatBtn = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_repeat_toggle)
    val settingsBtn = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_settings)
    val subtitleBtn = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_subtitle)
    val fullscreenBtn = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_fullscreen)
    val vrBtn = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_vr)
    val overflowShowBtn = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_overflow_show)

    vrBtn?.visibility = View.GONE
    overflowShowBtn?.visibility = View.GONE

    val volumeBtn = createBottomBarActionButton(
        context = context,
        tagName = TAG_CUSTOM_VOLUME_BTN,
        drawableRes = if (isMutedProvider()) R.drawable.ic_exo_volume_off else R.drawable.ic_exo_volume_on,
        contentDescription = "الصوت"
    ).apply {
        setOnClickListener {
            playerView.showController()
            onToggleMute(this)
        }
    }

    val pipBtn = createBottomBarActionButton(
        context = context,
        tagName = TAG_CUSTOM_PIP_BTN,
        drawableRes = R.drawable.ic_exo_pip,
        contentDescription = "نافذة عائمة"
    ).apply {
        setOnClickListener {
            playerView.showController()
            onEnterPip()
        }
    }

    val aspectBtn = createBottomBarActionButton(
        context = context,
        tagName = TAG_CUSTOM_ASPECT_BTN,
        drawableRes = R.drawable.ic_exo_aspect_ratio,
        contentDescription = "أبعاد الشاشة"
    ).apply {
        setOnClickListener {
            playerView.showController()
            onCycleAspectRatio()
        }
    }

    val multiviewBtn = createBottomBarActionButton(
        context = context,
        tagName = TAG_CUSTOM_MULTIVIEW_BTN,
        drawableRes = R.drawable.ic_exo_multiview,
        contentDescription = "تبديل السيرفر"
    ).apply {
        setOnClickListener {
            playerView.showController()
            onMultiViewOrServerClick()
        }
    }

    val orderedButtons = listOfNotNull(
        shuffleBtn,
        repeatBtn,
        volumeBtn,
        settingsBtn,
        pipBtn,
        subtitleBtn,
        aspectBtn,
        multiviewBtn,
        fullscreenBtn
    )

    orderedButtons.forEach { btn ->
        btn.isFocusable = true
        attachTvFocusEffect(btn, playerView)
    }

    val btnWidthPx = dpToPx(context, 42)
    val btnHeightPx = dpToPx(context, 40)
    val btnPadPx = dpToPx(context, 9)

    val enforceBottomBarOrder = {
        if (overflowShowBtn?.visibility == View.VISIBLE) {
            overflowShowBtn.visibility = View.GONE
        }
        if (vrBtn?.visibility == View.VISIBLE) {
            vrBtn.visibility = View.GONE
        }

        val needsReorder = orderedButtons.withIndex().any { (targetIndex, view) ->
            view.parent !== basicControls ||
                basicControls.indexOfChild(view) != targetIndex ||
                view.visibility != View.VISIBLE ||
                view.layoutParams?.width != btnWidthPx
        }

        if (needsReorder) {
            orderedButtons.forEachIndexed { targetIndex, view ->
                val parentGroup = view.parent as? ViewGroup
                if (parentGroup != null && parentGroup !== basicControls) {
                    parentGroup.removeView(view)
                }
                if (view.parent == null) {
                    basicControls.addView(
                        view,
                        targetIndex.coerceAtMost(basicControls.childCount),
                        LinearLayout.LayoutParams(btnWidthPx, btnHeightPx).apply {
                            gravity = Gravity.CENTER_VERTICAL
                        }
                    )
                } else {
                    val currentIdx = basicControls.indexOfChild(view)
                    if (currentIdx != targetIndex && currentIdx >= 0) {
                        basicControls.removeView(view)
                        basicControls.addView(
                            view,
                            targetIndex.coerceAtMost(basicControls.childCount),
                            LinearLayout.LayoutParams(btnWidthPx, btnHeightPx).apply {
                                gravity = Gravity.CENTER_VERTICAL
                            }
                        )
                    }
                }

                if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                }
                view.setPadding(btnPadPx, btnPadPx, btnPadPx, btnPadPx)
                val lp = view.layoutParams as? LinearLayout.LayoutParams
                if (lp == null || lp.width != btnWidthPx || lp.height != btnHeightPx) {
                    view.layoutParams = LinearLayout.LayoutParams(btnWidthPx, btnHeightPx).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                }
            }
        }

        prevBtn?.apply {
            if (visibility != View.VISIBLE) visibility = View.VISIBLE
            if (!isEnabled) isEnabled = true
            if (alpha < 0.99f) alpha = 1.0f
        }
        rewBtn?.apply {
            if (this is TextView && text.isNotEmpty()) {
                text = ""
            }
        }
        ffwdBtn?.apply {
            if (this is TextView && text.isNotEmpty()) {
                text = ""
            }
        }
    }

    enforceBottomBarOrder()

    bottomBar?.setBackgroundColor(0x99000000.toInt())
    bottomBar?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        val needsFix = overflowShowBtn?.visibility == View.VISIBLE ||
            orderedButtons.any { it.parent !== basicControls || it.visibility != View.VISIBLE }
        if (needsFix) {
            bottomBar.post { enforceBottomBarOrder() }
        }
    }
}

@OptIn(UnstableApi::class)
private fun attachTvFocusEffect(view: View, playerView: PlayerView) {
    view.setOnFocusChangeListener { v, hasFocus ->
        v.scaleX = if (hasFocus) 1.2f else 1f
        v.scaleY = if (hasFocus) 1.2f else 1f
        if (hasFocus) {
            playerView.showController()
        }
    }
}

private fun createPlayPauseCircleDrawable(focused: Boolean): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(AndroidColor.WHITE)
        if (focused) {
            setStroke(6, 0xFF00E5FF.toInt())
        }
    }
}

private fun createBottomBarActionButton(
    context: Context,
    tagName: String,
    drawableRes: Int,
    contentDescription: String
): ImageButton {
    val outValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
    return ImageButton(context).apply {
        tag = tagName
        setImageResource(drawableRes)
        this.contentDescription = contentDescription
        scaleType = ImageView.ScaleType.FIT_CENTER
        setBackgroundResource(outValue.resourceId)
        alpha = 1.0f
    }
}

private fun dpToPx(context: Context, dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}

private fun cleanServerDisplayName(rawName: String, fallbackIndex: Int): String {
    val cleaned = rawName
        .replace(Regex("\\b(TS|M3U8|HLS|MPEGTS)\\b", RegexOption.IGNORE_CASE), "")
        .replace(Regex("[•|()\\-]+\\s*$"), "")
        .replace(Regex("^\\s*[•|()\\-]+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    return cleaned.ifBlank { "سيرفر ${fallbackIndex + 1}" }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
