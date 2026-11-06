package dev.spider.spider.ui

import dev.spider.spider.ui.platform.BackHandler
import dev.spider.spider.ui.home.HomeFabCluster
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spider.spider.data.config.ConfigRepository
import dev.spider.spider.domain.config.effectiveLocalProxyPort
import dev.spider.spider.domain.config.effectiveLanSocksPort
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.auto_group_all
import dev.spider.spider.shared.resources.auto_title_for
import dev.spider.spider.domain.model.AppLanguage
import dev.spider.spider.domain.model.AppSettings
import dev.spider.spider.ui.servers.ServersScreen
import dev.spider.spider.ui.theme.toPersonalization
import dev.spider.spider.ui.connection.ConnectionViewModel
import dev.spider.spider.ui.easteregg.SpaceScreen
import dev.spider.spider.ui.home.HomeScreen
import dev.spider.spider.ui.motion.transitionDecoration
import dev.spider.spider.ui.navigation.TopDestination
import dev.spider.spider.ui.settings.SettingsScreen
import dev.spider.spider.ui.spider.spiderPointerPulse
import dev.spider.spider.ui.platform.hideNativePointer
import dev.spider.spider.ui.update.NudgeHost
import dev.spider.spider.ui.vault.VaultImportHost
import dev.spider.spider.ui.vault.VaultScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScaffold(
    settings: AppSettings,
    configRepository: ConfigRepository,
    onToggleConnection: () -> Unit,

    onActiveServerChanged: () -> Unit = {},
    onImportZsx: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onLanguage: (AppLanguage) -> Unit,

    aiBridge: dev.spider.spider.domain.ai.AiAppBridge? = null,
    appVersion: String = "",
) {
    val platform = dev.spider.spider.ui.platform.LocalPlatform.current
    val lanSharingScope = androidx.compose.runtime.rememberCoroutineScope()
    fun setLanSharing(enabled: Boolean) {
        onUpdateSettings { current ->
            if (enabled && (current.socksUsername.isBlank() || current.socksPassword.isBlank())) current.copy(
                proxySharing = true,
                socksUsername = current.socksUsername.ifBlank { "zed" },
                socksPassword = current.socksPassword.ifBlank { dev.spider.spider.platform.secureRandomToken(16) },
            ) else current.copy(proxySharing = enabled)
        }
        if (dev.spider.spider.core.VpnManager.status.value.state == dev.spider.spider.domain.model.ConnectionState.Connected) {
            lanSharingScope.launch {
                kotlinx.coroutines.delay(120)
                onToggleConnection()
                kotlinx.coroutines.withTimeoutOrNull(12_000) {
                    dev.spider.spider.core.VpnManager.status.first {
                        it.state == dev.spider.spider.domain.model.ConnectionState.Idle
                    }
                }
                kotlinx.coroutines.delay(200)
                if (dev.spider.spider.core.VpnManager.status.value.state == dev.spider.spider.domain.model.ConnectionState.Idle) {
                    onToggleConnection()
                }
            }
        }
    }
    var current by rememberSaveable { mutableStateOf(TopDestination.Home) }

    var spaceUnlocked by rememberSaveable { mutableStateOf(false) }

    var deepLinkOrigin by rememberSaveable { mutableStateOf<TopDestination?>(null) }

    var pendingSettingsPage by rememberSaveable {
        mutableStateOf(dev.spider.spider.ui.settings.SettingsPage.Root)
    }
    val spider = true
    val tabs = androidx.compose.runtime.remember(settings.navTabs, platform.supportsPerAppProxy) {
        val available = dev.spider.spider.domain.model.NavTab.entries
            .filter { it != dev.spider.spider.domain.model.NavTab.Apps || platform.supportsPerAppProxy }
            .toSet()
        dev.spider.spider.domain.model.NavTabs.parse(settings.navTabs, available).map { TopDestination.of(it) }
    }
    fun orderOf(destination: TopDestination): Int = tabs.indexOf(destination).takeIf { it >= 0 } ?: (tabs.size + destination.ordinal)
    val barStyle = if (spider) dev.spider.spider.domain.model.NavBarStyle.Dock else settings.navBarStyle
    val dock = spider
    var dockShown by rememberSaveable { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(current) { dockShown = true }
    val autoHide = settings.spider.dockAutoHide
    val dockScroll = androidx.compose.runtime.remember(autoHide) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (autoHide) {
                    if (consumed.y < -4f) dockShown = false else if (consumed.y > 4f || available.y > 24f) dockShown = true
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    val connectionVm: ConnectionViewModel = viewModel { ConnectionViewModel() }
    val homeVm: dev.spider.spider.ui.home.HomeViewModel = viewModel { dev.spider.spider.ui.home.HomeViewModel() }
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    val profiles by configRepository.profiles.collectAsStateWithLifecycle()
    val subscriptions by configRepository.subscriptions.collectAsStateWithLifecycle()
    val activeId by configRepository.activeId.collectAsStateWithLifecycle()

    val activeProfile = androidx.compose.runtime.remember(profiles, subscriptions, activeId) {
        activeId?.let { configRepository.profile(it) }
    }
    val autoSession by dev.spider.spider.core.AutoSelect.session.collectAsStateWithLifecycle()

    val lockedNote = activeProfile?.takeIf { it.isLocked }?.note

    val mtuHint = androidx.compose.runtime.remember(activeProfile) {
        val parsed = activeProfile?.linkServer
        val host = parsed?.address?.takeIf { it.isNotBlank() } ?: activeProfile?.address.orEmpty()
        dev.spider.spider.data.net.MtuServerHint(
            host = host,
            overhead = parsed
                ?.let { dev.spider.spider.data.net.MtuOverheads.ofServer(it) }
                ?: dev.spider.spider.data.net.MtuOverheads.worstCase(),
        )
    }

    VaultImportHost(
        repository = configRepository,
        onImported = { current = TopDestination.Vault },
    )

    dev.spider.spider.ui.servers.DeepLinkImportHost(
        repository = configRepository,
        onImported = { current = TopDestination.Servers },
    )

    NudgeHost(settings = settings, onUpdateSettings = onUpdateSettings)

    BackHandler(enabled = current != TopDestination.Home) { current = TopDestination.Home }

    if (spaceUnlocked) {
        SpaceScreen(onExit = { spaceUnlocked = false })
        return
    }

    val serverTargets = androidx.compose.runtime.remember(profiles) {
        profiles.filter { it.standardLink != null }.map { it.id to it.name }
    }
    val groupTargets = listOf(
        dev.spider.spider.domain.config.AutoSelectIds.ALL to
            stringResource(Res.string.auto_title_for, stringResource(Res.string.auto_group_all)),
    ) + subscriptions.map { sub ->
        dev.spider.spider.domain.config.AutoSelectIds.of(sub.id) to stringResource(Res.string.auto_title_for, sub.name)
    }

    Box(
        Modifier
            .fillMaxSize()
            .hideNativePointer(platform.isComputer)
            .spiderPointerPulse(platform.isComputer, dev.spider.spider.ui.spider.Spider.colors.bad),
    ) {
    Scaffold(
        bottomBar = {
            if (!dock) {
                dev.spider.spider.ui.navigation.SpiderNavBar(
                    style = barStyle,
                    tabs = tabs,
                    current = current,
                    onSelect = { current = it },
                )
            }
        },
    ) { scaffoldPadding ->
      val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
      val innerPadding = if (dock) {
          PaddingValues(
              start = scaffoldPadding.calculateStartPadding(layoutDirection),
              top = scaffoldPadding.calculateTopPadding(),
              end = scaffoldPadding.calculateEndPadding(layoutDirection),
              bottom = scaffoldPadding.calculateBottomPadding() + dev.spider.spider.ui.spider.SpiderDockHeight +
                  dev.spider.spider.ui.spider.SpiderDockMargin + 8.dp,
          )
      } else {
          scaffoldPadding
      }
      androidx.compose.foundation.layout.Box(
          Modifier
              .fillMaxSize()
              .then(if (dock) Modifier.nestedScroll(dockScroll) else Modifier),
      ) {
        val sign = dev.spider.spider.ui.motion.layoutSign()
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                dev.spider.spider.ui.motion.screenTransition(
                    style = settings.screenTransition,
                    forward = orderOf(targetState) >= orderOf(initialState),
                    direction = sign,
                    reduceMotion = settings.reduceMotion,
                ).using(SizeTransform(clip = false))
            },
            label = "top-destination",
            modifier = Modifier.fillMaxSize(),
        ) { dest ->

            val decoration = transitionDecoration(
                style = settings.screenTransition,
                forward = orderOf(dest) >= orderOf(current),
                direction = sign,
                reduceMotion = settings.reduceMotion,
            )
            Box(decoration) {
            when (dest) {
                TopDestination.Home -> {
                  val homeName = activeProfile?.let { p ->
                      if (p.isAutoSelect) {
                          stringResource(
                              Res.string.auto_title_for,
                              dev.spider.spider.ui.servers.autoSelectGroupLabel(p),
                          )
                      } else p.name
                  }
                  val homeDetail = activeProfile?.takeIf { it.isAutoSelect }?.let { p ->
                      autoSession?.takeIf { it.profileId == p.id }?.let { session ->
                          dev.spider.spider.ui.servers.autoSelectLiveLine(session) { id ->
                              profiles.firstOrNull { it.id == id }?.name
                          }
                      }
                  }
                  HomeScreen(
                    connectionVm = connectionVm,
                    homeVm = homeVm,
                    activeConfigName = homeName,
                    exitGeneration = autoSession?.takeIf { it.profileId == activeProfile?.id }?.exitGeneration ?: 0,
                    activeConfigDetail = homeDetail,
                    lockedNote = lockedNote,
                    localProxyAddress = if (settings.runMode == dev.spider.spider.domain.model.RunMode.ProxyOnly) {
                        "127.0.0.1:${dev.spider.spider.core.VpnManager.activeSocksPort ?: settings.effectiveLocalProxyPort}"
                    } else null,
                    activeLocked = activeProfile?.isLocked == true,
                    reduceMotion = settings.reduceMotion,

                    showConnectionInfo = activeProfile?.isDnsTunnel != true,
                    ipApiUrl = settings.ipApiUrl,
                    delayTestUrl = settings.delayTestUrl,

                    personalization = settings.toPersonalization(),
                    connectStyle = settings.connectButtonStyle,
                    design = dev.spider.spider.domain.model.AppDesign.Spider,
                    contentPadding = innerPadding,
                    runMode = settings.runMode,
                    onRunModeChange = { mode -> onUpdateSettings { it.copy(runMode = mode) } },
                    routingMode = settings.routingMode,
                    onRoutingModeChange = { mode -> onUpdateSettings { it.copy(routingMode = mode) } },
                    shareLanActive = settings.proxySharing,
                    shareLanPort = settings.effectiveLanSocksPort,
                    onToggleShareLan = ::setLanSharing,
                    onToggleConnection = onToggleConnection,

                    onBrowseConfigs = {
                        current = if (activeProfile?.isLocked == true) TopDestination.Vault
                        else TopDestination.Servers
                    },
                    onSecretUnlocked = { spaceUnlocked = true },
                    spiderActive = activeProfile?.let { p ->
                        dev.spider.spider.ui.spider.SpiderActiveServer(
                            name = homeName ?: p.name,
                            detail = homeDetail ?: p.transportLabel.removeSuffix(" · sing-box"),
                            country = if (settings.detectServerLocation) dev.spider.spider.domain.config.ProfileCountry.of(p) else null,
                            engine = autoSession
                                ?.takeIf { p.isAutoSelect && it.profileId == p.id }
                                ?.selectedProfileId
                                ?.let { id -> profiles.firstOrNull { it.id == id } }
                                ?.engineLabel
                                ?: p.engineLabel,
                            locked = p.isLocked,
                            subscription = p.subscriptionId.ifBlank { p.autoSelectSettings()?.subscriptionId.orEmpty() }
                                .takeIf { it.isNotBlank() }
                                ?.let { id -> subscriptions.firstOrNull { it.id == id } },
                        )
                    },
                    spiderPicks = androidx.compose.runtime.remember(profiles, activeId, settings.detectServerLocation) {
                        profiles
                            .filter { !it.isLocked && it.id != activeId && (it.lastPingMs ?: 0) > 0 }
                            .sortedBy { it.lastPingMs }
                            .take(3)
                            .map {
                                dev.spider.spider.ui.spider.SpiderServerPick(
                                    id = it.id,
                                    name = it.name,
                                    detail = it.transportLabel.removeSuffix(" · sing-box"),
                                    country = if (settings.detectServerLocation) dev.spider.spider.domain.config.ProfileCountry.of(it) else null,
                                    pingMs = it.lastPingMs,
                                )
                            }
                    },
                    onPickServer = { id ->
                        configRepository.setActive(id)
                        onActiveServerChanged()
                    },
                    onOpenSpeedTest = {
                        if (TopDestination.SpeedTest in tabs) {
                            current = TopDestination.SpeedTest
                        } else {
                            deepLinkOrigin = current
                            pendingSettingsPage = dev.spider.spider.ui.settings.SettingsPage.SpeedTest
                            current = TopDestination.Settings
                        }
                    },
                    onOpenMap = {
                        if (TopDestination.Map in tabs) {
                            current = TopDestination.Map
                        } else {
                            deepLinkOrigin = current
                            pendingSettingsPage = dev.spider.spider.ui.settings.SettingsPage.Map
                            current = TopDestination.Settings
                        }
                    },
                  )
                }
                TopDestination.Servers -> ServersScreen(
                    repository = configRepository,
                    contentPadding = innerPadding,
                    twoColumns = settings.doubleColumnDisplay,
                    showAllGroup = settings.groupAllDisplay,
                    confirmRemove = settings.confirmRemove,
                    realPingConcurrency = settings.realPingConcurrency,
                    delayTestUrl = settings.delayTestUrl,
                    autoTestAfterUpdate = settings.autoTestAfterUpdate,
                    autoRemoveInvalidAfterTest = settings.autoRemoveInvalidAfterTest,
                    autoSortAfterTest = settings.autoSortAfterTest,
                    personalization = settings.toPersonalization(),
                    reduceMotion = settings.reduceMotion,
                    onServerActivated = {
                        if (settings.homeAfterSelect) current = TopDestination.Home

                        onActiveServerChanged()
                    },
                    detectServerLocation = settings.detectServerLocation,
                )
                TopDestination.Chains -> dev.spider.spider.ui.servers.ChainsScreen(
                    repository = configRepository,
                    contentPadding = innerPadding,
                    onChainActivated = {
                        onActiveServerChanged()
                        if (settings.homeAfterSelect) current = TopDestination.Home
                    },
                )
                TopDestination.Vault -> VaultScreen(
                    repository = configRepository,
                    contentPadding = innerPadding,
                    onImportRequested = onImportZsx,
                    onActiveChanged = onActiveServerChanged,
                )
                TopDestination.Subscriptions -> dev.spider.spider.ui.navigation.SubscriptionsTabPage(configRepository, innerPadding)
                TopDestination.Logs -> dev.spider.spider.ui.navigation.LogsTabPage(innerPadding)
                TopDestination.SpeedTest -> dev.spider.spider.ui.components.ProvidePageBack(null) {
                    dev.spider.spider.ui.speedtest.SpeedTestScreen(
                        contentPadding = innerPadding,
                        unsupported = activeProfile?.isDnsBasedTunnel == true,
                    )
                }
                TopDestination.Map -> dev.spider.spider.ui.components.ProvidePageBack(null) {
                    dev.spider.spider.ui.map.MapScreen(
                        contentPadding = innerPadding,
                        reduceMotion = settings.reduceMotion,
                        ipApiUrl = settings.ipApiUrl,
                        personalization = settings.toPersonalization(),
                        settings = settings,
                        onToggleShareLan = ::setLanSharing,
                        savedOriginCode = settings.verifiedOriginCode,
                        savedOriginLabel = settings.verifiedOriginLabel,
                        onRealLocation = { code, label ->
                            if (code != settings.verifiedOriginCode || label != settings.verifiedOriginLabel) {
                                onUpdateSettings { it.copy(verifiedOriginCode = code, verifiedOriginLabel = label) }
                            }
                        },
                    )
                }
                TopDestination.Routing -> dev.spider.spider.ui.components.ProvidePageBack(null) {
                    dev.spider.spider.ui.settings.RoutingScreen(
                        settings,
                        innerPadding,
                        onUpdateSettings,
                        Modifier,
                        serverTargets,
                        groupTargets,
                    )
                }
                TopDestination.Apps -> dev.spider.spider.ui.components.ProvidePageBack(null) {
                    dev.spider.spider.ui.settings.PerAppProxyScreen(settings, innerPadding, onUpdateSettings, Modifier)
                }
                TopDestination.Settings -> SettingsScreen(
                    settings = settings,
                    configRepository = configRepository,
                    contentPadding = innerPadding,
                    onUpdate = onUpdateSettings,
                    onLanguage = onLanguage,
                    initialPage = pendingSettingsPage,
                    onDeepLinkBack = deepLinkOrigin?.let { origin ->
                        {
                            current = origin
                            deepLinkOrigin = null
                        }
                    },
                    onReconnect = onToggleConnection,
                    onInitialPageConsumed = {
                        pendingSettingsPage = dev.spider.spider.ui.settings.SettingsPage.Root
                    },

                    serverTargets = serverTargets,
                    autoCountries = androidx.compose.runtime.remember(profiles) {
                        profiles.mapNotNull { dev.spider.spider.domain.config.ProfileCountry.of(it) }.distinct()
                    },
                    groupTargets = groupTargets,
                    dnsTunnelActive = activeProfile?.isDnsBasedTunnel == true,

                    onOpenVault = if (TopDestination.Vault in tabs) null else ({ current = TopDestination.Vault }),

                    mtuHint = mtuHint,
                    ai = aiBridge?.let { bridge ->
                        dev.spider.spider.ui.settings.AiSection(
                            settings = settings.ai,
                            onUpdate = { next -> onUpdateSettings { it.copy(ai = next) } },
                            bridge = bridge,
                            onOpenUrl = { url -> platform.openUri(url) },

                            languageName = languageEnglishName(settings.language),
                            languageNative = languageNativeName(settings.language),
                            platform = "Android",
                            appVersion = appVersion,
                        )
                    },
                )
            }
            }
        }

        val vpnStatus by dev.spider.spider.core.VpnManager.status.collectAsStateWithLifecycle()
        val detailsOpen by homeVm.expanded.collectAsStateWithLifecycle()
        HomeFabCluster(
            visible = current == TopDestination.Home && !spider &&
                !detailsOpen,
            showSpeedTest = vpnStatus.state == dev.spider.spider.domain.model.ConnectionState.Connected &&
                activeProfile?.isDnsBasedTunnel != true,
            reduceMotion = settings.reduceMotion,
            atEnd = settings.speedFabAtEnd,
            yFraction = settings.speedFabY,
            onMove = { end, y -> onUpdateSettings { it.copy(speedFabAtEnd = end, speedFabY = y) } },
            onOpenMap = {
                deepLinkOrigin = current
                pendingSettingsPage = dev.spider.spider.ui.settings.SettingsPage.Map
                current = TopDestination.Settings
            },
            onOpenSpeedTest = {
                deepLinkOrigin = current
                pendingSettingsPage = dev.spider.spider.ui.settings.SettingsPage.SpeedTest
                current = TopDestination.Settings
            },
            modifier = Modifier.padding(innerPadding),
        )
      }
    }

    if (dock) {
        dev.spider.spider.ui.spider.SpiderDock(
            tabs = tabs,
            current = current,
            onSelect = { current = it },
            visible = dockShown || !autoHide,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    }
}

private fun languageEnglishName(language: AppLanguage): String = when (language) {
    AppLanguage.Persian -> "Persian"
    AppLanguage.Chinese -> "Chinese"
    AppLanguage.Russian -> "Russian"
    AppLanguage.Spanish -> "Spanish"
    AppLanguage.English -> "English"

    AppLanguage.System -> "the language the user writes in"
}

private fun languageNativeName(language: AppLanguage): String = when (language) {
    AppLanguage.Persian -> "فارسی"
    AppLanguage.Chinese -> "中文"
    AppLanguage.Russian -> "Русский"
    AppLanguage.Spanish -> "Español"
    AppLanguage.English -> "English"
    AppLanguage.System -> ""
}
