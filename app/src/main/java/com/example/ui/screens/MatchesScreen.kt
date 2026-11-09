package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.LeagueMatchesGroup
import com.example.data.model.LineupPlayer
import com.example.data.model.LiveChannel
import com.example.data.model.LiveEventMatch
import com.example.data.model.MatchCompetitor
import com.example.data.model.MatchDetailData
import com.example.data.model.MatchItem
import com.example.data.model.MatchPublishedChannel
import com.example.data.model.MatchesSearchResult
import com.example.data.model.PlayerFullProfile
import com.example.data.model.TeamFullProfile
import com.example.ui.components.CompactTopBarIconButton
import com.example.ui.components.FoulSectionSubBar
import com.example.ui.components.FoulSmartTopBar
import com.example.ui.components.OrderedColorfulDivider
import com.example.ui.components.tvFocusableClickable
import com.example.ui.theme.FoulBlackBg
import com.example.ui.theme.FoulBottomBarBlack
import com.example.ui.theme.FoulCardDark
import com.example.ui.theme.FoulCardElevated
import com.example.ui.theme.FoulDivider
import com.example.ui.theme.FoulLiveGreen
import com.example.ui.theme.FoulLiveRed
import com.example.ui.theme.FoulPurpleAccent
import com.example.ui.theme.FoulPurpleBright
import com.example.ui.theme.FoulPurpleDeep
import com.example.ui.theme.FoulPurpleGlow
import com.example.ui.theme.FoulPurplePrimary
import com.example.ui.theme.FoulSurfaceDark
import com.example.ui.theme.FoulTextMuted
import com.example.ui.theme.FoulTextSecondary
import com.example.ui.viewmodel.DateOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun MatchesScreen(
    dateOptions: List<DateOption>,
    selectedDate: String,
    isLoading: Boolean,
    error: String?,
    leagueGroups: List<LeagueMatchesGroup>,
    selectedMatch: MatchItem?,
    isLoadingMatchDetail: Boolean,
    matchDetailError: String?,
    matchDetail: MatchDetailData?,
    isMatchesSearchOpen: Boolean = false,
    matchesSearchQuery: String = "",
    isSearchingMatches: Boolean = false,
    matchesSearchResult: MatchesSearchResult? = null,
    selectedPlayerProfile: PlayerFullProfile? = null,
    isLoadingPlayerProfile: Boolean = false,
    playerProfileError: String? = null,
    selectedTeamProfile: TeamFullProfile? = null,
    isLoadingTeamProfile: Boolean = false,
    teamProfileError: String? = null,
    allChannels: List<LiveChannel> = emptyList(),
    liveEvents: List<LiveEventMatch> = emptyList(),
    matchPublishedChannels: List<MatchPublishedChannel> = emptyList(),
    telegramUrl: String = "https://t.me/jdj_q",
    onSelectDate: (String) -> Unit,
    onMatchClick: (MatchItem) -> Unit,
    onWatchChannel: (LiveChannel) -> Unit = {},
    onCloseMatchDetail: () -> Unit,
    onOpenMatchesSearch: () -> Unit = {},
    onCloseMatchesSearch: () -> Unit = {},
    onMatchesSearchQueryChange: (String) -> Unit = {},
    onPlayerClickFromLineup: (LineupPlayer, Int, String, String) -> Unit = { _, _, _, _ -> },
    onPlayerSelectFromSearch: (PlayerFullProfile) -> Unit = {},
    onClosePlayerProfile: () -> Unit = {},
    onTeamSelectFromSearch: (MatchCompetitor) -> Unit = {},
    onCloseTeamProfile: () -> Unit = {},
    onRefresh: () -> Unit
) {
    // 1. Player Full Profile Screen
    if (selectedPlayerProfile != null || isLoadingPlayerProfile) {
        PlayerFullProfileScreen(
            playerProfile = selectedPlayerProfile,
            isLoading = isLoadingPlayerProfile,
            errorMessage = playerProfileError,
            onBack = onClosePlayerProfile
        )
        return
    }

    // 2. Team / Club Full Profile Screen
    if (selectedTeamProfile != null || isLoadingTeamProfile) {
        TeamFullProfileScreen(
            teamProfile = selectedTeamProfile,
            isLoading = isLoadingTeamProfile,
            errorMessage = teamProfileError,
            onMatchClick = { match ->
                onCloseTeamProfile()
                onMatchClick(match)
            },
            onPlayerClick = onPlayerSelectFromSearch,
            onBack = onCloseTeamProfile
        )
        return
    }

    // 3. Match Detail Screen
    if (selectedMatch != null) {
        val selectedMatchChannels = remember(selectedMatch, matchPublishedChannels, allChannels, liveEvents) {
            resolveChannelsForMatch(
                match = selectedMatch,
                matchPublishedChannels = matchPublishedChannels,
                allChannels = allChannels,
                liveEvents = liveEvents
            )
        }
        MatchDetailScreen(
            match = selectedMatch,
            isLoading = isLoadingMatchDetail,
            error = matchDetailError,
            detail = matchDetail,
            publishedChannels = selectedMatchChannels,
            allChannels = allChannels,
            telegramUrl = telegramUrl,
            onWatchChannel = onWatchChannel,
            onBack = onCloseMatchDetail,
            onRetry = { onMatchClick(selectedMatch) },
            onPlayerClick = onPlayerClickFromLineup
        )
        return
    }

    // 4. Dedicated Matches Search Screen
    if (isMatchesSearchOpen) {
        MatchesSearchScreen(
            searchQuery = matchesSearchQuery,
            onSearchQueryChange = onMatchesSearchQueryChange,
            isSearching = isSearchingMatches,
            searchResult = matchesSearchResult,
            telegramUrl = telegramUrl,
            onPlayerSelect = onPlayerSelectFromSearch,
            onTeamSelect = onTeamSelectFromSearch,
            onBack = onCloseMatchesSearch
        )
        return
    }

    val baghdadTz = remember { TimeZone.getTimeZone("Asia/Baghdad") }
    val apiFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = baghdadTz } }

    val yesterdayDate = remember {
        dateOptions.firstOrNull { it.labelAr == "أمس" }?.apiDateString ?: run {
            val c = Calendar.getInstance(baghdadTz)
            c.add(Calendar.DAY_OF_YEAR, -1)
            apiFmt.format(c.time)
        }
    }
    val todayDate = remember {
        dateOptions.firstOrNull { it.labelAr == "اليوم" }?.apiDateString ?: run {
            val c = Calendar.getInstance(baghdadTz)
            apiFmt.format(c.time)
        }
    }
    val tomorrowDate = remember {
        dateOptions.firstOrNull { it.labelAr == "غداً" }?.apiDateString ?: run {
            val c = Calendar.getInstance(baghdadTz)
            c.add(Calendar.DAY_OF_YEAR, 1)
            apiFmt.format(c.time)
        }
    }

    var isCalendarDialogOpen by remember { mutableStateOf(false) }
    var pickerTitle by remember { mutableStateOf("") }
    var pickerSubtitle by remember { mutableStateOf("") }
    var pickerChannels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }

    if (pickerChannels.isNotEmpty()) {
        MatchAndEventChannelPickerDialog(
            title = pickerTitle,
            subtitle = pickerSubtitle,
            channels = pickerChannels,
            onSelectChannel = { ch ->
                pickerChannels = emptyList()
                onWatchChannel(ch)
            },
            onDismiss = { pickerChannels = emptyList() }
        )
    }

    val centerCalendarLabel = when (selectedDate) {
        todayDate -> "اليوم • $selectedDate"
        yesterdayDate -> "أمس • $selectedDate"
        tomorrowDate -> "غداً • $selectedDate"
        else -> selectedDate
    }

    if (isCalendarDialogOpen) {
        CalendarTableDialog(
            selectedDateStr = selectedDate,
            todayDateStr = todayDate,
            onSelectDate = { pickedDate ->
                isCalendarDialogOpen = false
                onSelectDate(pickedDate)
            },
            onDismiss = { isCalendarDialogOpen = false }
        )
    }

    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0 = All, 1 = Live Now, 2 = Broadcasted

    val totalMatchesCount = remember(leagueGroups) {
        leagueGroups.sumOf { it.matches.size }
    }
    val liveMatchesCount = remember(leagueGroups) {
        leagueGroups.sumOf { group -> group.matches.count { it.isLive } }
    }
    val broadcastMatchesCount = remember(leagueGroups, matchPublishedChannels, allChannels, liveEvents) {
        leagueGroups.sumOf { group ->
            group.matches.count { m ->
                m.tvBroadcasters.isNotEmpty() || m.hasTvNetworks ||
                    resolveChannelsForMatch(m, matchPublishedChannels, allChannels, liveEvents).isNotEmpty()
            }
        }
    }
    val filteredLeagueGroups = remember(leagueGroups, selectedFilterIndex, matchPublishedChannels, allChannels, liveEvents) {
        when (selectedFilterIndex) {
            1 -> leagueGroups.mapNotNull { g ->
                val filtered = g.matches.filter { it.isLive }
                if (filtered.isEmpty()) null else g.copy(matches = filtered)
            }
            2 -> leagueGroups.mapNotNull { g ->
                val filtered = g.matches.filter { m ->
                    m.tvBroadcasters.isNotEmpty() || m.hasTvNetworks ||
                        resolveChannelsForMatch(m, matchPublishedChannels, allChannels, liveEvents).isNotEmpty()
                }
                if (filtered.isEmpty()) null else g.copy(matches = filtered)
            }
            else -> leagueGroups
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FoulBlackBg)
            .testTag("matches_screen")
    ) {
        // 1. Smart Top Bar: Strictly Telegram Icon + FOUL TV Name + App Logo Image
        FoulSmartTopBar(
            telegramUrl = telegramUrl,
            telegramTestTag = "matches_telegram_button"
        )

        // 2. Contextual Sub-Header Bar Directly Underneath Top Bar (Section Title + Search Button + Refresh)
        FoulSectionSubBar(
            title = "جدول المباريات",
            subtitle = when {
                liveMatchesCount > 0 -> "مباشر الآن ($liveMatchesCount) • إجمالي المباريات ($totalMatchesCount)"
                totalMatchesCount > 0 -> "إجمالي مباريات هذا اليوم ($totalMatchesCount مباراة)"
                else -> "النتائج المباشرة والقنوات الناقلة"
            },
            onSearchClick = onOpenMatchesSearch,
            searchButtonLabel = "بحث لاعب أو فريق",
            searchButtonTestTag = "matches_search_button",
            actions = {
                CompactTopBarIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "تحديث المباريات",
                    onClick = onRefresh,
                    modifier = Modifier.testTag("matches_refresh_button")
                )
            }
        )

        // 3. Sleek Pure-Black & Dark-Purple 3-Part Date Bar: أمس | اليوم (جدول تقويم) | غداً
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FoulBottomBarBlack)
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. أمس (Yesterday)
                val isYesterdaySelected = selectedDate == yesterdayDate
                DateTabPill(
                    label = "أمس",
                    isSelected = isYesterdaySelected,
                    onClick = { onSelectDate(yesterdayDate) },
                    testTag = "date_yesterday_button"
                )

                // 2. اليوم / Calendar Picker Button
                val isCenterHighlighted = selectedDate != yesterdayDate && selectedDate != tomorrowDate
                Surface(
                    color = if (isCenterHighlighted) FoulPurplePrimary else FoulCardDark,
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .tvFocusableClickable(
                            shape = RoundedCornerShape(11.dp),
                            scaleOnFocus = 1.03f,
                            onClick = { isCalendarDialogOpen = true }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isCenterHighlighted) FoulPurpleBright else FoulDivider,
                            shape = RoundedCornerShape(11.dp)
                        )
                        .testTag("date_calendar_picker_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "فتح التقويم",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = centerCalendarLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = FoulTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 3. غداً (Tomorrow)
                val isTomorrowSelected = selectedDate == tomorrowDate
                DateTabPill(
                    label = "غداً",
                    isSelected = isTomorrowSelected,
                    onClick = { onSelectDate(tomorrowDate) },
                    testTag = "date_tomorrow_button"
                )
            }

            // Quick Filter Chips Bar (الكل | مباشر الآن | القنوات الناقلة)
            if (totalMatchesCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MatchesFilterChip(
                        label = "الكل ($totalMatchesCount)",
                        selected = selectedFilterIndex == 0,
                        accentColor = FoulPurpleBright,
                        onClick = { selectedFilterIndex = 0 },
                        modifier = Modifier.weight(1f),
                        testTag = "matches_filter_all"
                    )
                    MatchesFilterChip(
                        label = "مباشر الآن ($liveMatchesCount)",
                        selected = selectedFilterIndex == 1,
                        accentColor = FoulLiveGreen,
                        showLiveDot = liveMatchesCount > 0,
                        onClick = { selectedFilterIndex = 1 },
                        modifier = Modifier.weight(1f),
                        testTag = "matches_filter_live"
                    )
                    MatchesFilterChip(
                        label = "القنوات الناقلة ($broadcastMatchesCount)",
                        selected = selectedFilterIndex == 2,
                        accentColor = Color(0xFF38BDF8),
                        onClick = { selectedFilterIndex = 2 },
                        modifier = Modifier.weight(1.2f),
                        testTag = "matches_filter_broadcast"
                    )
                }
            }
        }
        OrderedColorfulDivider(thickness = 1.dp)

        // Matches Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = FoulPurpleBright, strokeWidth = 2.8.dp)
                        Text(
                            text = "جاري جلب جدول المباريات المباشر...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FoulTextSecondary
                        )
                    }
                }
            }

            error != null && leagueGroups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FoulCardDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, FoulDivider)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = FoulPurplePrimary)
                            ) {
                                Text("إعادة المحاولة", color = Color.White)
                            }
                        }
                    }
                }
            }

            filteredLeagueGroups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = FoulPurpleGlow,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = when (selectedFilterIndex) {
                                1 -> "لا توجد مباريات جارية الآن في هذا التاريخ"
                                2 -> "لا توجد مباريات بقنوات ناقلة مسجلة حالياً في هذا الفلتر"
                                else -> "لا توجد مباريات مسجلة في هذا التاريخ"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = FoulTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        if (selectedFilterIndex != 0 && leagueGroups.isNotEmpty()) {
                            Button(
                                onClick = { selectedFilterIndex = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = FoulPurplePrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("عرض جميع المباريات ($totalMatchesCount)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("matches_lazy_column")
                ) {
                    items(filteredLeagueGroups, key = { it.competitionId }) { group ->
                        LeagueGroupCard(
                            group = group,
                            matchPublishedChannels = matchPublishedChannels,
                            allChannels = allChannels,
                            liveEvents = liveEvents,
                            onMatchClick = onMatchClick,
                            onWatchSingleChannel = onWatchChannel,
                            onWatchMatchChannels = { match, channels ->
                                if (channels.size == 1) {
                                    onWatchChannel(channels.first())
                                } else if (channels.size > 1) {
                                    pickerTitle = "${match.homeTeam.name} ضد ${match.awayTeam.name}"
                                    pickerSubtitle = "اختر القناة الناقلة للمباراة (${match.competitionName})"
                                    pickerChannels = channels
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchesFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    showLiveDot: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        color = if (selected) accentColor.copy(alpha = 0.22f) else FoulCardDark,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .tvFocusableClickable(
                shape = RoundedCornerShape(9.dp),
                scaleOnFocus = 1.04f,
                onClick = onClick
            )
            .border(
                width = 1.dp,
                color = if (selected) accentColor else FoulDivider,
                shape = RoundedCornerShape(9.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showLiveDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(FoulLiveGreen)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = if (selected) Color.White else FoulTextSecondary,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DateTabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (isSelected) FoulPurplePrimary else FoulCardDark,
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .tvFocusableClickable(
                shape = RoundedCornerShape(11.dp),
                scaleOnFocus = 1.05f,
                onClick = onClick
            )
            .border(
                width = 1.dp,
                color = if (isSelected) FoulPurpleBright else FoulDivider,
                shape = RoundedCornerShape(11.dp)
            )
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.White else FoulTextSecondary,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}

/**
 * Interactive Calendar Table Modal ("بشكل جدول تقويم للفتح والعرض")
 */
@Composable
private fun CalendarTableDialog(
    selectedDateStr: String,
    todayDateStr: String,
    onSelectDate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val baghdadTz = remember { TimeZone.getTimeZone("Asia/Baghdad") }
    val apiFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = baghdadTz } }

    val initialCal = remember(selectedDateStr) {
        val c = Calendar.getInstance(baghdadTz)
        try {
            val parsed = apiFmt.parse(selectedDateStr)
            if (parsed != null) c.time = parsed
        } catch (_: Exception) {
        }
        c
    }

    var currentYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }

    val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    val weekDaysAr = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

    val daysInMonth = remember(currentYear, currentMonth) {
        val c = Calendar.getInstance(baghdadTz)
        c.set(currentYear, currentMonth, 1)
        val maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1
        val cells = mutableListOf<Int?>()
        repeat(firstDayOfWeek) { cells.add(null) }
        for (d in 1..maxDay) cells.add(d)
        cells
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FoulCardDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FoulPurpleBright.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .testTag("calendar_table_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "اختر تاريخ المباريات",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = FoulTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Month & Year Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FoulBlackBg)
                        .border(1.dp, FoulDivider, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear -= 1
                            } else {
                                currentMonth -= 1
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "الشهر السابق",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "${arabicMonths[currentMonth]} $currentYear",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear += 1
                            } else {
                                currentMonth += 1
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "الشهر التالي",
                            tint = Color.White
                        )
                    }
                }

                // Weekday Headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDaysAr.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = FoulTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 7-Column Calendar Table Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    items(daysInMonth.size) { index ->
                        val dayNum = daysInMonth[index]
                        if (dayNum == null) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        } else {
                            val dateStr = String.format(
                                Locale.US,
                                "%02d/%02d/%04d",
                                dayNum,
                                currentMonth + 1,
                                currentYear
                            )
                            val isSelected = dateStr == selectedDateStr
                            val isToday = dateStr == todayDateStr

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> FoulPurplePrimary
                                            isToday -> FoulBlackBg
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            isSelected -> FoulPurpleBright
                                            isToday -> FoulDivider
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .tvFocusableClickable(
                                        shape = RoundedCornerShape(8.dp),
                                        scaleOnFocus = 1.08f,
                                        onClick = { onSelectDate(dateStr) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSelectDate(todayDateStr) },
                        colors = ButtonDefaults.buttonColors(containerColor = FoulPurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مباريات اليوم", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onSelectDate("21/10/2026") },
                        colors = ButtonDefaults.buttonColors(containerColor = FoulBlackBg),
                        border = BorderStroke(1.dp, FoulDivider),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("21/10/2026", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Luxury Pure-Black & Deep-Dark-Purple Competition Group Card
 */
@Composable
private fun LeagueGroupCard(
    group: LeagueMatchesGroup,
    matchPublishedChannels: List<MatchPublishedChannel>,
    allChannels: List<LiveChannel>,
    liveEvents: List<LiveEventMatch>,
    onMatchClick: (MatchItem) -> Unit,
    onWatchSingleChannel: (LiveChannel) -> Unit = {},
    onWatchMatchChannels: (MatchItem, List<LiveChannel>) -> Unit
) {
    val liveInLeague = remember(group.matches) { group.matches.count { it.isLive } }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FoulCardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (liveInLeague > 0) {
                        listOf(FoulLiveGreen.copy(alpha = 0.7f), FoulPurpleBright.copy(alpha = 0.55f), FoulDivider)
                    } else {
                        listOf(FoulPurpleBright.copy(alpha = 0.45f), FoulDivider, FoulDivider)
                    }
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sleek Dark Royal Purple & Obsidian Competition Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                FoulPurplePrimary.copy(alpha = 0.82f),
                                FoulSurfaceDark,
                                FoulCardDark
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Vertical accent bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (liveInLeague > 0) FoulLiveGreen else FoulPurpleGlow)
                    )

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.96f))
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = group.logoUrl,
                            contentDescription = group.competitionName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = group.competitionName,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (liveInLeague > 0) {
                        Surface(
                            color = FoulLiveGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(0.8.dp, FoulLiveGreen.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(FoulLiveGreen)
                                )
                                Text(
                                    text = "$liveInLeague مباشر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FoulLiveGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        color = FoulBlackBg,
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(0.8.dp, FoulDivider)
                    ) {
                        Text(
                            text = "${group.matches.size} مباراة",
                            style = MaterialTheme.typography.labelSmall,
                            color = FoulTextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FoulDivider)
            )

            // Matches List inside League on Pure Black Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FoulSurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.matches.forEach { match ->
                    val publishedChannels = remember(match, matchPublishedChannels, allChannels, liveEvents) {
                        resolveChannelsForMatch(
                            match = match,
                            matchPublishedChannels = matchPublishedChannels,
                            allChannels = allChannels,
                            liveEvents = liveEvents
                        )
                    }
                    MatchRowItem(
                        match = match,
                        publishedChannels = publishedChannels,
                        allChannels = allChannels,
                        onClick = { onMatchClick(match) },
                        onWatchSingleChannel = onWatchSingleChannel,
                        onWatchNowClick = { onWatchMatchChannels(match, publishedChannels) }
                    )
                }
            }
        }
    }
}

/**
 * Pure OLED Black & Deep Royal Purple Broadcast Match Row Card with Real Arabic TV Broadcasters
 */
@Composable
private fun MatchRowItem(
    match: MatchItem,
    publishedChannels: List<LiveChannel> = emptyList(),
    allChannels: List<LiveChannel> = emptyList(),
    onClick: () -> Unit,
    onWatchSingleChannel: (LiveChannel) -> Unit = {},
    onWatchNowClick: () -> Unit = {}
) {
    val realBroadcasters = remember(match.tvBroadcasters) {
        match.tvBroadcasters.filter { it.name.isNotBlank() }.distinctBy { it.name.trim().lowercase() }
    }
    val hasAnyBroadcasters = realBroadcasters.isNotEmpty() || publishedChannels.isNotEmpty()

    Surface(
        color = FoulBlackBg,
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .tvFocusableClickable(
                shape = RoundedCornerShape(15.dp),
                scaleOnFocus = 1.015f,
                onClick = onClick
            )
            .border(
                width = 1.dp,
                color = when {
                    match.isLive -> FoulLiveGreen.copy(alpha = 0.7f)
                    hasAnyBroadcasters -> FoulPurpleBright.copy(alpha = 0.55f)
                    else -> FoulDivider
                },
                shape = RoundedCornerShape(15.dp)
            )
            .testTag("match_card_${match.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            // Optional Stage / Round & Live Broadcast Header Strip
            if (match.roundName.isNotBlank() || match.isLive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (match.roundName.isNotBlank()) {
                        Text(
                            text = match.roundName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = FoulTextSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (match.isLive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(FoulLiveGreen)
                            )
                            Text(
                                text = "يبث الآن مباشرة",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = FoulLiveGreen,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home Team (Right in RTL)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FoulCardDark)
                            .border(1.dp, FoulDivider, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = match.homeTeam.logoUrl,
                            contentDescription = match.homeTeam.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = match.homeTeam.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (match.homeTeam.redCards > 0) {
                            Text(
                                text = "بطاقة حمراء (${match.homeTeam.redCards})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = FoulLiveRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Center Scoreboard / Kickoff Time Pod
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    if (match.homeTeam.hasScore && match.awayTeam.hasScore) {
                        Surface(
                            color = FoulCardDark,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = if (match.isLive) FoulLiveGreen else FoulPurpleBright.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        ) {
                            Text(
                                text = "${match.homeTeam.score}   :   ${match.awayTeam.score}",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (match.isLive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(FoulLiveGreen)
                                )
                            }
                            Text(
                                text = if (match.isLive && match.gameTimeDisplay.isNotEmpty()) {
                                    "مباشر • ${match.gameTimeDisplay}"
                                } else {
                                    match.statusText
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = if (match.isLive) FoulLiveGreen else FoulTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Surface(
                            color = FoulCardDark,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = FoulPurplePrimary,
                                shape = RoundedCornerShape(10.dp)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = match.displayTime.ifBlank { match.statusText },
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (match.displayTime.isNotBlank() && match.statusText.isNotBlank()) {
                                    Text(
                                        text = match.statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = FoulTextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Away Team (Left in RTL)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = match.awayTeam.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        if (match.awayTeam.redCards > 0) {
                            Text(
                                text = "بطاقة حمراء (${match.awayTeam.redCards})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = FoulLiveRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(9.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FoulCardDark)
                            .border(1.dp, FoulDivider, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = match.awayTeam.logoUrl,
                            contentDescription = match.awayTeam.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Real Arabic TV Broadcasters & Published Channels Footer Strip (Strictly shown ONLY when real data exists)
            if (hasAnyBroadcasters) {
                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    FoulPurpleDeep.copy(alpha = 0.85f),
                                    FoulCardDark
                                )
                            )
                        )
                        .border(0.8.dp, FoulPurpleBright.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = FoulLiveGreen,
                            modifier = Modifier.size(14.dp)
                        )

                        // Combine real Arabic broadcasters (e.g. beIN SPORTS HD 1, الكأس) + published channels
                        val broadcasterLabels = remember(realBroadcasters, publishedChannels) {
                            val list = mutableListOf<String>()
                            realBroadcasters.forEach { b ->
                                if (list.none { it.equals(b.name.trim(), ignoreCase = true) }) {
                                    list.add(b.name.trim())
                                }
                            }
                            publishedChannels.forEach { ch ->
                                if (list.none { it.equals(ch.name.trim(), ignoreCase = true) }) {
                                    list.add(ch.name.trim())
                                }
                            }
                            list
                        }

                        if (realBroadcasters.firstOrNull()?.id ?: 0 > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .padding(1.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = realBroadcasters.first().logoUrl,
                                    contentDescription = realBroadcasters.first().name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Text(
                            text = broadcasterLabels.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Show "شاهد الآن" button if developer published channels OR a broadcaster matches an app channel
                    val matchedPlayableFromBroadcaster = remember(realBroadcasters, publishedChannels, allChannels) {
                        realBroadcasters.firstNotNullOfOrNull { b ->
                            findPlayableChannelForBroadcaster(b.name, publishedChannels, allChannels)
                        }
                    }

                    if (publishedChannels.isNotEmpty() || matchedPlayableFromBroadcaster != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FoulPurplePrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .tvFocusableClickable(
                                    shape = RoundedCornerShape(8.dp),
                                    scaleOnFocus = 1.06f,
                                    onClick = {
                                        if (publishedChannels.isNotEmpty()) {
                                            onWatchNowClick()
                                        } else if (matchedPlayableFromBroadcaster != null) {
                                            onWatchSingleChannel(matchedPlayableFromBroadcaster)
                                        }
                                    }
                                )
                                .border(1.dp, FoulPurpleBright, RoundedCornerShape(8.dp))
                                .testTag("match_watch_now_${match.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "شاهد الآن",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "شاهد الآن",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
