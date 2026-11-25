package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.ScoutCatalog
import com.example.data.remote.UserProgressPayload
import com.example.model.GudepMatchStatus
import com.example.model.GudepProfile
import com.example.model.ScoutMaterial
import com.example.model.ScoutTask
import com.example.model.SyncStateStatus
import com.example.model.UserSession
import com.example.model.WeatherSnapshot
import com.example.ui.components.BentoCard
import com.example.ui.components.GudepLogoView
import com.example.ui.components.JenjangBadge
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BerandaScreen
import com.example.ui.screens.DashboardPembinaScreen
import com.example.ui.screens.JenjangOnboardingScreen
import com.example.ui.screens.KegiatanScreen
import com.example.ui.screens.MateriScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScoutLabScreen
import com.example.ui.screens.SkuKuisScreen
import com.example.ui.theme.AmberScoutSoft
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatSeragamSoft
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.HijauAlam
import com.example.ui.theme.HijauAlamSoft
import com.example.ui.theme.KremHangat
import com.example.ui.theme.OutlineWarm

enum class MainNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    BERANDA("beranda", "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    MATERI("materi", "Materi", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    LAB("lab", "Lab", Icons.Filled.Science, Icons.Outlined.Science),
    SKU("sku", "SKU", Icons.Filled.AssignmentTurnedIn, Icons.Outlined.AssignmentTurnedIn),
    KEGIATAN("kegiatan", "Kegiatan", Icons.Filled.Groups, Icons.Outlined.Groups)
}

enum class ActiveOverlayScreen {
    NONE,
    PROFILE,
    DASHBOARD
}

@Composable
fun MyScoutApp() {
    val context = LocalContext.current
    val viewModel: MainScoutViewModel = viewModel(factory = MainScoutViewModel.factory(context))

    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    if (isAuthLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KremHangat),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CokelatTua)
        }
        return
    }

    val activeSession = session
    if (activeSession == null) {
        AuthScreen(
            authError = authError,
            onGoogleSuccess = { uid, email, name ->
                viewModel.onGoogleAuthenticated(uid, email, name)
            },
            onAuthError = { viewModel.setAuthErrorMessage(it) },
            onManualRegister = { fullName, username, email, password ->
                viewModel.registerManual(fullName, username, email, password)
            },
            onManualLogin = { identifier, password ->
                viewModel.loginManual(identifier, password)
            }
        )
    } else if (!activeSession.hasCompletedJenjangSelection) {
        JenjangOnboardingScreen(
            session = activeSession,
            onConfirmJenjang = { jenjang ->
                viewModel.confirmInitialJenjang(jenjang)
            },
            onSwitchAccount = {
                viewModel.logout(context)
            }
        )
    } else {
        key(activeSession.sessionId) {
            MainScoutShell(
                session = activeSession,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun MainScoutShell(
    session: UserSession,
    viewModel: MainScoutViewModel
) {
    val context = LocalContext.current
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val gudepMatchStatus by viewModel.gudepMatchStatus.collectAsStateWithLifecycle()
    val ownedGudep by viewModel.ownedGudep.collectAsStateWithLifecycle()
    val gudepMaterials by viewModel.gudepMaterials.collectAsStateWithLifecycle()
    val gudepTasks by viewModel.gudepTasks.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val cachedMaterialIds by viewModel.cachedOpenedMaterialIds.collectAsStateWithLifecycle()
    val cachedTaskIds by viewModel.cachedOpenedTaskIds.collectAsStateWithLifecycle()
    val totalCachedCount by viewModel.totalCachedOpenedCount.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainNavTab.BERANDA) }
    var overlayScreen by remember { mutableStateOf(ActiveOverlayScreen.NONE) }
    var labInitialTab by remember { mutableIntStateOf(0) }
    var skuInitialTab by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val msg = feedback
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Strict guard if user changes jenjang to non-Pembina while on Dashboard
    LaunchedEffect(session.canAccessDashboard) {
        if (!session.canAccessDashboard && overlayScreen == ActiveOverlayScreen.DASHBOARD) {
            overlayScreen = ActiveOverlayScreen.PROFILE
        }
    }

    if (overlayScreen == ActiveOverlayScreen.PROFILE) {
        Scaffold(
            containerColor = KremHangat,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ProfileScreen(
                    session = session,
                    gudepMatchStatus = gudepMatchStatus,
                    syncStatus = syncStatus,
                    isOnline = isOnline,
                    pendingSyncCount = pendingSyncCount,
                    totalCachedCount = totalCachedCount,
                    onBack = { overlayScreen = ActiveOverlayScreen.NONE },
                    onSaveProfile = { name, user, jenjang, inst, gudep, unit ->
                        viewModel.saveProfileForm(name, user, jenjang, inst, gudep, unit)
                    },
                    onOpenDashboard = {
                        if (session.canAccessDashboard) {
                            overlayScreen = ActiveOverlayScreen.DASHBOARD
                        }
                    },
                    onExportJson = { viewModel.exportBackupJson() },
                    onImportJson = { raw -> viewModel.importBackupJson(raw) },
                    onLogout = { viewModel.logout(context) }
                )
            }
        }
        return
    }

    if (overlayScreen == ActiveOverlayScreen.DASHBOARD) {
        Scaffold(
            containerColor = KremHangat,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                DashboardPembinaScreen(
                    session = session,
                    ownedGudep = ownedGudep,
                    gudepMaterials = gudepMaterials,
                    onBack = { overlayScreen = ActiveOverlayScreen.PROFILE },
                    onSaveGudep = { inst, num, addr, logo ->
                        viewModel.saveGudepIdentity(inst, num, addr, logo)
                    },
                    onSaveMaterial = { id, title, cat, target, sum, content, pub ->
                        viewModel.saveGudepMaterial(id, title, cat, target, sum, content, pub)
                    },
                    onSendTask = { title, desc, target, due, xp ->
                        viewModel.sendAdditionalTask(title, desc, target, due, xp)
                    }
                )
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat)
    ) {
        val isTabletOrDesktop = maxWidth >= 680.dp
        val showRightSummaryPanel = maxWidth >= 1000.dp

        if (!isTabletOrDesktop) {
            // Mobile Layout: Compact Header + Bottom NavigationBar (5 tabs)
            Scaffold(
                containerColor = KremHangat,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    MobileTopBar(
                        session = session,
                        onOpenProfile = { overlayScreen = ActiveOverlayScreen.PROFILE }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        MainNavTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CokelatTua,
                                    selectedTextColor = CokelatTua,
                                    indicatorColor = CokelatSeragamSoft,
                                    unselectedIconColor = CokelatSeragam,
                                    unselectedTextColor = CokelatSeragam
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    OfflineQueueBanner(
                        isOnline = isOnline,
                        pendingSyncCount = pendingSyncCount,
                        totalCachedCount = totalCachedCount
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        MainTabContent(
                            currentTab = currentTab,
                            session = session,
                            progress = progress,
                            gudepMatchStatus = gudepMatchStatus,
                            gudepMaterials = gudepMaterials,
                            gudepTasks = gudepTasks,
                            cachedMaterialIds = cachedMaterialIds,
                            cachedTaskIds = cachedTaskIds,
                            weather = weather,
                            labInitialTab = labInitialTab,
                            skuInitialTab = skuInitialTab,
                            viewModel = viewModel,
                            onSetLabTab = { labInitialTab = it; currentTab = MainNavTab.LAB },
                            onSetSkuTab = { skuInitialTab = it; currentTab = MainNavTab.SKU },
                            onOpenProfile = { overlayScreen = ActiveOverlayScreen.PROFILE }
                        )
                    }
                }
            }
        } else {
            // Tablet / Desktop Layout: Left NavigationRail + Center Content + Right Summary Panel
            Scaffold(
                containerColor = KremHangat,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = Color.White,
                        header = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_scout_emblem_1795584337483),
                                    contentDescription = "Logo My Scout",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("My Scout", style = MaterialTheme.typography.labelSmall, color = CokelatTua, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        MainNavTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CokelatTua,
                                    selectedTextColor = CokelatTua,
                                    indicatorColor = CokelatSeragamSoft
                                ),
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        NavigationRailItem(
                            selected = false,
                            onClick = { overlayScreen = ActiveOverlayScreen.PROFILE },
                            icon = { Icon(Icons.Filled.Person, contentDescription = "Profil") },
                            label = { Text("Profil") },
                            modifier = Modifier.padding(bottom = 12.dp).testTag("rail_profile")
                        )
                    }

                    VerticalDivider(color = OutlineWarm)

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        OfflineQueueBanner(
                            isOnline = isOnline,
                            pendingSyncCount = pendingSyncCount,
                            totalCachedCount = totalCachedCount
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            MainTabContent(
                                currentTab = currentTab,
                                session = session,
                                progress = progress,
                                gudepMatchStatus = gudepMatchStatus,
                                gudepMaterials = gudepMaterials,
                                gudepTasks = gudepTasks,
                                cachedMaterialIds = cachedMaterialIds,
                                cachedTaskIds = cachedTaskIds,
                                weather = weather,
                                labInitialTab = labInitialTab,
                                skuInitialTab = skuInitialTab,
                                viewModel = viewModel,
                                onSetLabTab = { labInitialTab = it; currentTab = MainNavTab.LAB },
                                onSetSkuTab = { skuInitialTab = it; currentTab = MainNavTab.SKU },
                                onOpenProfile = { overlayScreen = ActiveOverlayScreen.PROFILE }
                            )
                        }
                    }

                    if (showRightSummaryPanel) {
                        VerticalDivider(color = OutlineWarm)
                        DesktopRightSummaryPanel(
                            session = session,
                            gudepMatchStatus = gudepMatchStatus,
                            syncStatus = syncStatus,
                            isOnline = isOnline,
                            pendingSyncCount = pendingSyncCount,
                            totalCachedCount = totalCachedCount,
                            onOpenProfile = { overlayScreen = ActiveOverlayScreen.PROFILE },
                            modifier = Modifier.width(300.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineQueueBanner(
    isOnline: Boolean,
    pendingSyncCount: Int,
    totalCachedCount: Int
) {
    AnimatedVisibility(visible = !isOnline || pendingSyncCount > 0) {
        Surface(
            color = if (!isOnline) AmberScoutSoft else HijauAlamSoft,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offline_sync_banner")
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (!isOnline) Icons.Filled.CloudOff else Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = if (!isOnline) CokelatTua else HijauAlam,
                        modifier = Modifier.size(16.dp)
                    )
                    val message = when {
                        !isOnline && pendingSyncCount > 0 ->
                            "Mode Offline • $totalCachedCount item di cache • $pendingSyncCount antrean Drive otomatis saat online"
                        !isOnline ->
                            "Mode Offline Aktif • $totalCachedCount materi & tugas siap diakses offline"
                        else ->
                            "Menyinkronkan $pendingSyncCount antrean data ke Google Drive..."
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!isOnline) CokelatTua else HijauAlam,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider(color = OutlineWarm)
            }
        }
    }
}

@Composable
private fun MobileTopBar(
    session: UserSession,
    onOpenProfile: () -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_scout_emblem_1795584337483),
                        contentDescription = "Logo My Scout",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "My Scout",
                        style = MaterialTheme.typography.titleLarge,
                        color = CokelatTua
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    JenjangBadge(jenjang = session.jenjang)
                    Surface(
                        onClick = onOpenProfile,
                        shape = RoundedCornerShape(50),
                        color = CokelatTua,
                        modifier = Modifier.testTag("header_profile_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profil",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Profil",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = OutlineWarm)
        }
    }
}

@Composable
private fun DesktopRightSummaryPanel(
    session: UserSession,
    gudepMatchStatus: GudepMatchStatus,
    syncStatus: SyncStateStatus,
    isOnline: Boolean,
    pendingSyncCount: Int,
    totalCachedCount: Int,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dasa = remember { ScoutCatalog.dasaDarmaForToday() }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(KremHangat)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BentoCard(onClick = onOpenProfile) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val logo = (gudepMatchStatus as? GudepMatchStatus.Matched)?.gudep?.logoBase64
                GudepLogoView(logoBase64 = logo, institutionName = session.institutionName.ifBlank { "My Scout" }, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.fullName.ifBlank { session.username }, style = MaterialTheme.typography.titleSmall, color = CokelatTua)
                    JenjangBadge(jenjang = session.jenjang)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (gudepMatchStatus) {
                    is GudepMatchStatus.Matched -> "${gudepMatchStatus.gudep.institutionName} • Gudep ${gudepMatchStatus.gudep.gudepNumber}"
                    is GudepMatchStatus.Unmatched -> "Gudep ${gudepMatchStatus.enteredGudepNumber} belum terdaftar"
                    GudepMatchStatus.Unfilled -> "Lengkapi data Gudep di Profil"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BentoCard(
            containerColor = if (!isOnline || pendingSyncCount > 0) AmberScoutSoft else HijauAlamSoft,
            borderColor = if (!isOnline || pendingSyncCount > 0) CokelatSeragam.copy(alpha = 0.4f) else HijauAlam.copy(alpha = 0.35f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (!isOnline) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = if (!isOnline || pendingSyncCount > 0) CokelatTua else HijauAlam,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            !isOnline -> "Mode Offline ($totalCachedCount cache)"
                            syncStatus == SyncStateStatus.SYNCING -> "Menyinkronkan..."
                            pendingSyncCount > 0 -> "$pendingSyncCount antrean Drive"
                            else -> "Sinkronisasi Otomatis Aktif"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (!isOnline || pendingSyncCount > 0) CokelatTua else HijauAlam
                    )
                    Text(
                        text = "$totalCachedCount materi & tugas tersimpan offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = CokelatTua
                    )
                }
            }
        }

        BentoCard(containerColor = CokelatTua, borderColor = CokelatTua) {
            Text("DASA DARMA KE-${dasa.number}", style = MaterialTheme.typography.labelSmall, color = CokelatSeragam)
            Spacer(Modifier.height(4.dp))
            Text(dasa.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(dasa.practiceTip, style = MaterialTheme.typography.bodySmall, color = KremHangat)
        }
    }
}

@Composable
private fun MainTabContent(
    currentTab: MainNavTab,
    session: UserSession,
    progress: UserProgressPayload,
    gudepMatchStatus: GudepMatchStatus,
    gudepMaterials: List<ScoutMaterial>,
    gudepTasks: List<ScoutTask>,
    cachedMaterialIds: Set<String>,
    cachedTaskIds: Set<String>,
    weather: WeatherSnapshot,
    labInitialTab: Int,
    skuInitialTab: Int,
    viewModel: MainScoutViewModel,
    onSetLabTab: (Int) -> Unit,
    onSetSkuTab: (Int) -> Unit,
    onOpenProfile: () -> Unit
) {
    when (currentTab) {
        MainNavTab.BERANDA -> BerandaScreen(
            session = session,
            progress = progress,
            gudepMatchStatus = gudepMatchStatus,
            tasks = gudepTasks,
            cachedTaskIds = cachedTaskIds,
            weather = weather,
            onRefreshWeather = { viewModel.refreshWeather() },
            onTaskOpened = { viewModel.onTaskOpened(it) },
            onToggleTaskComplete = { viewModel.toggleTaskComplete(it) },
            onNavigateToProfile = onOpenProfile,
            onQuickActionSandi = { onSetLabTab(0) },
            onQuickActionKuis = { onSetSkuTab(2) },
            onQuickActionSku = { onSetSkuTab(0) },
            onQuickActionKompas = { onSetLabTab(2) }
        )
        MainNavTab.MATERI -> MateriScreen(
            gudepMaterials = gudepMaterials,
            readMaterialIds = progress.readMaterialIds,
            cachedMaterialIds = cachedMaterialIds,
            onMaterialOpened = { viewModel.onMaterialOpened(it) },
            onToggleRead = { viewModel.toggleMaterialRead(it) }
        )
        MainNavTab.LAB -> ScoutLabScreen(
            initialTab = labInitialTab,
            packingList = progress.packingList,
            onTogglePackingItem = { viewModel.togglePackingItem(it) },
            onAddPackingItem = { name, cat -> viewModel.addPackingItem(name, cat) }
        )
        MainNavTab.SKU -> SkuKuisScreen(
            initialTab = skuInitialTab,
            userJenjang = session.jenjang,
            progress = progress,
            onToggleSku = { viewModel.toggleSkuCompleted(it) },
            onToggleTkk = { viewModel.toggleTkkEarned(it) },
            onRecordQuizResult = { viewModel.recordQuizResult(it) }
        )
        MainNavTab.KEGIATAN -> KegiatanScreen(
            session = session,
            progress = progress,
            onToggleMemberPresent = { viewModel.toggleMemberAttendance(it) },
            onAddMember = { name, role -> viewModel.addReguMember(name, role) },
            onToggleActivityAttended = { viewModel.toggleActivityAttendance(it) },
            onAddActivity = { title, date, loc, desc -> viewModel.addActivity(title, date, loc, desc) }
        )
    }
}
