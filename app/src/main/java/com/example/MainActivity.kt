package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutUser
import com.example.data.IntegrationSyncService
import com.example.data.TpqRepository
import com.example.data.UserViewMode
import com.example.notifications.NotificationHelper
import com.example.ui.TpqViewModel
import com.example.ui.screens.AbsensiScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GamifikasiLaporanScreen
import com.example.ui.screens.HafalanTajwidScreen
import com.example.ui.screens.IntegrasiSettingsScreen
import com.example.ui.theme.RaporTpqTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Beranda", Icons.Rounded.Home, Icons.Outlined.Home),
    HAFALAN("hafalan_114", "114 Surat", Icons.Rounded.MenuBook, Icons.Outlined.MenuBook),
    ABSENSI("absensi", "Absensi", Icons.Rounded.EventAvailable, Icons.Outlined.EventAvailable),
    PRESTASI("prestasi_laporan", "Prestasi", Icons.Rounded.WorkspacePremium, Icons.Outlined.WorkspacePremium),
    INTEGRASI("integrasi", "Integrasi", Icons.Rounded.Settings, Icons.Outlined.Settings)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        setContent {
            RaporTpqTheme {
                RaporTpqRoot()
            }
        }
    }
}

@Composable
private fun RaporTpqRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedRaporShell(
                currentUserId = uid,
                displayName = user.displayName ?: "Ustadz / Wali TPQ",
                email = user.email ?: "",
                onSignOut = {
                    signOutUser(credentialManager, scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedRaporShell(
    currentUserId: String,
    displayName: String,
    email: String,
    onSignOut: () -> Unit
) {
    val viewModel: TpqViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                TpqViewModel(
                    repository = TpqRepository(db),
                    integrationService = IntegrationSyncService(app),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val santriListState by viewModel.santriListState.collectAsStateWithLifecycle()
    val selectedSantri by viewModel.selectedSantri.collectAsStateWithLifecycle()
    val nilaiListState by viewModel.nilaiListState.collectAsStateWithLifecycle()
    val absensiListState by viewModel.absensiListState.collectAsStateWithLifecycle()
    val laporanListState by viewModel.laporanListState.collectAsStateWithLifecycle()
    val localCredentials by viewModel.localCredentials.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var isBiometricUnlocked by remember(currentUserId) { mutableStateOf(false) }
    val requiresBiometric = userProfile?.biometricEnabled == true && !isBiometricUnlocked

    if (requiresBiometric) {
        BiometricLockScreen(
            userName = userProfile?.displayName?.ifBlank { displayName } ?: displayName,
            onUnlocked = { isBiometricUnlocked = true },
            onSignOutClick = onSignOut
        )
        return
    }

    var activeTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (activeTab != AppTab.DASHBOARD) {
        BackHandler { activeTab = AppTab.DASHBOARD }
    }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val viewMode = UserViewMode.fromRaw(userProfile?.preferredMode)
    val santriList = (santriListState as? com.example.ui.UiState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 780.dp).fillMaxSize()) {
                        when (activeTab) {
                            AppTab.DASHBOARD -> DashboardScreen(
                                userProfile = userProfile,
                                santriListState = santriListState,
                                selectedSantri = selectedSantri,
                                nilaiListState = nilaiListState,
                                currentUserId = currentUserId,
                                onSelectSantri = viewModel::selectSantri,
                                onCreateSantri = viewModel::createSantri,
                                onLinkParentCode = viewModel::linkParentByShareCode,
                                onNavigateToHafalan = { activeTab = AppTab.HAFALAN },
                                onNavigateToAbsensi = { activeTab = AppTab.ABSENSI },
                                onCopyFeedback = viewModel::showFeedback
                            )

                            AppTab.HAFALAN -> HafalanTajwidScreen(
                                selectedSantri = selectedSantri,
                                nilaiListState = nilaiListState,
                                viewMode = viewMode,
                                currentUserId = currentUserId,
                                onRecordNilai = viewModel::recordNilai
                            )

                            AppTab.ABSENSI -> AbsensiScreen(
                                santriList = santriList,
                                selectedSantri = selectedSantri,
                                absensiListState = absensiListState,
                                viewMode = viewMode,
                                currentUserId = currentUserId,
                                onRecordAbsensi = viewModel::recordAbsensi,
                                onMarkAllHadir = viewModel::markAllHadir
                            )

                            AppTab.PRESTASI -> GamifikasiLaporanScreen(
                                selectedSantri = selectedSantri,
                                laporanListState = laporanListState,
                                viewMode = viewMode,
                                currentUserId = currentUserId,
                                onGenerateMonthlyReport = viewModel::generateMonthlyReport,
                                onAcknowledgeReport = viewModel::acknowledgeReport
                            )

                            AppTab.INTEGRASI -> IntegrasiSettingsScreen(
                                userProfile = userProfile,
                                localCredentials = localCredentials,
                                isBusy = isBusy,
                                onSaveSettings = viewModel::updateSettings,
                                onTestAppsScript = viewModel::testAppsScript,
                                onSyncToSpreadsheet = viewModel::syncToSpreadsheet,
                                onPushDirectToGitHub = viewModel::pushDirectToGitHub,
                                onFeedback = viewModel::showFeedback,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}
