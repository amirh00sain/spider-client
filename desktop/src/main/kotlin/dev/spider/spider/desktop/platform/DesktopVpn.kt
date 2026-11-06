package dev.spider.spider.desktop.platform

import dev.spider.spider.core.LogBus
import dev.spider.spider.core.SshController
import dev.spider.spider.core.VpnManager
import dev.spider.spider.data.config.ConfigRepository
import dev.spider.spider.desktop.core.DesktopDnsTunnel
import dev.spider.spider.desktop.core.DesktopIkev2
import dev.spider.spider.desktop.core.DesktopPsiphon
import dev.spider.spider.desktop.core.DesktopTor
import dev.spider.spider.desktop.core.DesktopTun
import dev.spider.spider.desktop.core.EngineCgroup
import dev.spider.spider.desktop.core.PhysicalInterface
import dev.spider.spider.core.LocalPortPicker
import dev.spider.spider.desktop.core.SystemProxy
import dev.spider.spider.desktop.core.Os
import dev.spider.spider.desktop.core.TunMode
import dev.spider.spider.desktop.core.XrayCore
import dev.spider.spider.domain.config.DnsTunnelProfile
import dev.spider.spider.domain.config.Ikev2Profile
import dev.spider.spider.domain.config.LocalPorts
import dev.spider.spider.domain.config.LocalProxy
import dev.spider.spider.domain.config.PsiphonProfile
import dev.spider.spider.domain.config.SshProfile
import dev.spider.spider.domain.config.autoSelectTuning
import dev.spider.spider.domain.config.effectiveLanSocksPort
import dev.spider.spider.domain.config.effectiveLocalProxyPort
import dev.spider.spider.domain.config.engineRemoteDns
import dev.spider.spider.domain.config.toBuildOptions
import dev.spider.spider.domain.model.AppSettings
import dev.spider.spider.domain.model.RunMode
import java.io.File
import java.net.InetAddress
import kotlin.concurrent.thread
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object DesktopVpn {
    private const val METRICS_PORT = LocalPorts.DESKTOP_METRICS
    private const val SSH_PORT = LocalPorts.SSH
    private const val PORT_WAIT_MS = 4_000L
    private const val PORT_RETRY_MS = 200L
    private const val DNS_PORT = LocalPorts.DNS_TUNNEL
    private const val CARRIER_PORT = LocalPorts.CHAIN_CARRIER
    private const val CARRIER_CONFIG = "xray-carrier.json"
    private const val CHAIN = "The chain"

    val work = File(System.getProperty("java.io.tmpdir"), "spider").apply { mkdirs() }
    @Volatile private var xray: XrayCore? = null
    private var tun: DesktopTun? = null
    private var stats: DesktopStats? = null
    private var singBoxStats: DesktopSingBoxStats? = null
    private var ssh: SshController? = null
    private var dnsTunnel: DesktopDnsTunnel? = null
    @Volatile private var tor: DesktopTor? = null
    @Volatile private var psiphon: DesktopPsiphon? = null
    @Volatile private var masterDns: dev.spider.spider.desktop.core.DesktopMasterDns? = null
    @Volatile private var ikev2: DesktopIkev2? = null
    @Volatile private var carrier: XrayCore? = null
    @Volatile private var openConnect: dev.spider.spider.desktop.core.DesktopOpenConnect? = null
    @Volatile private var aether: dev.spider.spider.desktop.core.DesktopAether? = null
    @Volatile private var mhr: dev.spider.spider.desktop.core.DesktopMhr? = null
    @Volatile private var soar: dev.spider.spider.desktop.core.DesktopSoar? = null
    @Volatile private var vpeen: dev.spider.spider.core.VpeenEngine? = null
    @Volatile private var firefoxVpnActive = false
    @Volatile private var outerPsiphon: DesktopPsiphon? = null
    @Volatile private var outerTor: DesktopTor? = null
    @Volatile private var outerSsh: SshController? = null
    @Volatile private var outerDns: DesktopDnsTunnel? = null
    @Volatile private var carrierBypass: List<String>? = null
    @Volatile private var engineCgroup: String? = null
    @Volatile private var lanOpen = false
    private var meter: DesktopMeter? = null
    private var usedSystemProxy = false

    @Volatile var runMode: RunMode = RunMode.SystemProxy

    @Volatile var settingsProvider: () -> AppSettings = { AppSettings() }

    @Volatile

    var chainConfigProvider: (dev.spider.spider.domain.config.VpnProfile, dev.spider.spider.domain.config.XrayJsonBuilder.BuildOptions) -> String? =
        { _, _ -> null }

    var ruleOutboundsProvider: (List<dev.spider.spider.domain.model.RulesetItem>) -> Map<String, dev.spider.spider.domain.config.ServerConfig> =
        { emptyMap() }

    var ruleGroupsProvider: (List<dev.spider.spider.domain.model.RulesetItem>) -> Map<String, dev.spider.spider.domain.config.RuleGroup> =
        { emptyMap() }

    private fun geoAssetsAvailable(): Boolean = dev.spider.spider.desktop.core.DesktopGeoAssets.allPresent()

    private fun geoCodes(settings: AppSettings): dev.spider.spider.domain.config.GeoCodes? =
        if (!geoAssetsAvailable()) null
        else dev.spider.spider.data.assets.GeoCodeReader.readFor(
            dev.spider.spider.desktop.core.DesktopGeoAssets.dir,
            dev.spider.spider.domain.config.RoutingMigration.effectiveRulesets(settings),
        )

    fun toggle(config: ConfigRepository) {
        val state = VpnManager.status.value.state
        if (state.isActive || state.isTransitioning) { stop(); return }
        val profile = config.activeProfile()?.unsealed() ?: run { VpnManager.onError("Select a config first"); return }

        val requestedMode = settingsProvider().runMode
        val providerProfile = profile.isVpeen || profile.isFirefoxVpn
        runMode = if (providerProfile && requestedMode == RunMode.ProxyOnly) RunMode.SystemProxy else requestedMode
        if (runMode != requestedMode) {
            LogBus.append("I/Desktop ${profile.engineLabel} uses the system proxy while proxy mode is selected")
        }

        when {
            profile.sniSpoofSettings() != null ->
                VpnManager.onError("SNI spoofing needs a rooted Android device; it is not available on desktop yet")
            profile.isFirefoxVpn -> startFirefoxVpn(profile.name)
            profile.isMhr -> startMhr(profile.name, profile.mhrSettings()!!)
            profile.isSoar -> startSoar(profile.name, profile.soarSettings()!!)
            profile.isVpeen -> startVpeen(profile.name, (profile.source as dev.spider.spider.domain.config.ProfileSource.Vpeen).region)
            profile.ikev2Settings() != null -> startIkev2(profile.name, profile.ikev2Settings()!!)
            profile.openConnectSettings() != null -> startOpenConnect(profile.name, profile.openConnectSettings()!!)
            profile.psiphonSettings() != null -> startPsiphon(profile.name, profile.psiphonSettings()!!)
            profile.isTor -> startTor(profile.name)
            profile.dnsTunnelSettings() != null -> startDns(profile.name, profile.dnsTunnelSettings()!!)
            profile.masterDnsSettings() != null -> startMasterDns(profile.name, profile.masterDnsSettings()!!)
            profile.sshSettings() != null -> startSsh(profile.name, profile.sshSettings()!!)
            profile.isSingBoxConfig -> startSingBoxConfig(profile.name, profile)
            profile.isAether -> startAether(profile.name, profile)
            profile.isCrossChain -> startCrossChain(profile.name, profile, config)
            else -> startXray(profile.name, profile, config)
        }
    }

    private fun startFirefoxVpn(name: String) {
        VpnManager.onStarting(name)
        thread(name = "desktop-firefox-vpn") {
            try {
                kotlinx.coroutines.runBlocking {
                    dev.spider.spider.platform.startFirefoxVpnCore()
                    dev.spider.spider.platform.firefoxVpnRequest("POST", "power", "{\"on\":true}")
                }
                firefoxVpnActive = true
                awaitFirefoxVpnProxy()
                if (!connectThrough(10810, "Firefox VPN")) {
                    kotlinx.coroutines.runBlocking {
                        dev.spider.spider.platform.firefoxVpnRequest("POST", "power", "{\"on\":false}")
                    }
                    firefoxVpnActive = false
                    return@thread
                }
                VpnManager.onConnected(name)
            } catch (e: Exception) {
                if (firefoxVpnActive) runCatching {
                    kotlinx.coroutines.runBlocking {
                        dev.spider.spider.platform.firefoxVpnRequest("POST", "power", "{\"on\":false}")
                    }
                }
                firefoxVpnActive = false
                LogBus.append("E/Firefox VPN ${e.message ?: "core could not start"}")
                VpnManager.onError("Firefox VPN failed: ${e.message ?: "core could not start"}")
            }
        }
    }

    private fun awaitFirefoxVpnProxy() {
        repeat(30) {
            val response = kotlinx.coroutines.runBlocking {
                dev.spider.spider.platform.firefoxVpnRequest("GET", "status")
            }
            val state = kotlinx.serialization.json.Json.parseToJsonElement(response)
                .jsonObject["state"]?.jsonObject
            val phase = state?.get("phase")?.jsonPrimitive?.contentOrNull.orEmpty()
            if (phase == "running") return
            val code = state?.get("code")?.jsonPrimitive?.contentOrNull.orEmpty()
            if (code == "no_accounts" || code == "no_usable_account" || phase == "error") {
                throw IllegalStateException(state?.get("message")?.jsonPrimitive?.contentOrNull ?: "No Firefox Account can connect")
            }
            Thread.sleep(500)
        }
        throw IllegalStateException("Firefox VPN proxy did not become ready")
    }

    private fun startAether(name: String, profile: dev.spider.spider.domain.config.VpnProfile) {
        val settings = profile.aetherSettings() ?: run { VpnManager.onError("Invalid Aether profile"); return }
        VpnManager.onStarting(name)
        thread(name = "desktop-aether") {
            val engine = dev.spider.spider.desktop.core.DesktopAether(settings, work)
            aether = engine
            val port = engine.start().getOrElse { error ->
                if (aether === engine) aether = null
                VpnManager.onError(error.message ?: "Aether failed to start")
                return@thread
            }
            if (aether !== engine) { engine.stop(); return@thread }
            if (!connectThrough(port, "Aether")) { engine.stop(); aether = null; return@thread }
            VpnManager.onConnected(name)
            while (aether === engine) {
                Thread.sleep(2_000)
                if (aether === engine && !engine.isRunning) {
                    aether = null
                    VpnManager.onError("Aether disconnected")
                    break
                }
            }
        }
    }

    private fun startMhr(name: String, settings: dev.spider.spider.domain.config.MhrProfile) {
        VpnManager.onStarting(name)
        thread(name = "desktop-mhr") {
            val engine = dev.spider.spider.desktop.core.DesktopMhr(settings, work)
            mhr = engine
            val port = engine.start()
            if (port < 0) { if (mhr === engine) mhr = null; VpnManager.onError("MHR failed to start; check Python 3.10+ and the logs"); return@thread }
            if (mhr !== engine) { engine.stop(); return@thread }
            if (!connectThrough(port, "MHR")) { engine.stop(); mhr = null; return@thread }
            VpnManager.onConnected(name)
            while (mhr === engine) {
                Thread.sleep(2_000)
                if (mhr === engine && !engine.isRunning) {
                    mhr = null
                    engine.stop()
                    VpnManager.onError("MHR disconnected")
                    break
                }
            }
        }
    }

    private fun startSoar(name: String, settings: dev.spider.spider.domain.config.SoarProfile) {
        VpnManager.onStarting(name)
        thread(name = "desktop-soar") {
            val engine = dev.spider.spider.desktop.core.DesktopSoar(settings, work)
            soar = engine
            val port = engine.start()
            if (port < 0) { if (soar === engine) soar = null; VpnManager.onError("Soar failed to start; check the logs"); return@thread }
            if (soar !== engine) { engine.stop(); return@thread }
            if (!connectThrough(port, "Soar")) { engine.stop(); soar = null; return@thread }
            VpnManager.onConnected(name)
            while (soar === engine) {
                Thread.sleep(2_000)
                if (soar === engine && !engine.isRunning) {
                    soar = null
                    engine.stop()
                    VpnManager.onError("Soar disconnected")
                    break
                }
            }
        }
    }

    private fun startVpeen(name: String, region: String) {
        VpnManager.onStarting(name)
        thread(name = "desktop-vpeen") {
            val engine = dev.spider.spider.core.VpeenEngine(File(work, "vpeen-state"), log = { LogBus.append("I/VPeeN $it") })
            vpeen = engine
            val port = runCatching { engine.start(region, localPort(18530)) }.getOrElse { error ->
                if (vpeen === engine) vpeen = null
                engine.stop()
                VpnManager.onError("VPeeN failed: ${error.message ?: "connection error"}")
                return@thread
            }
            if (vpeen !== engine) { engine.stop(); return@thread }
            if (!connectThrough(port, "VPeeN")) { engine.stop(); vpeen = null; return@thread }
            VpnManager.onConnected(name)
            while (vpeen === engine) {
                Thread.sleep(2_000)
                if (vpeen === engine && !engine.isRunning) {
                    vpeen = null
                    engine.stop()
                    VpnManager.onError("VPeeN disconnected")
                    break
                }
            }
        }
    }

    private fun startDns(name: String, dnsProfile: DnsTunnelProfile) {
        VpnManager.onStarting(name)
        thread(name = "desktop-dns") {
            val dns = DesktopDnsTunnel(dnsProfile, work, listenPort = DNS_PORT)
            dnsTunnel = dns
            val port = dns.start()
            if (port < 0) { VpnManager.onError("DNS tunnel failed to start"); return@thread }

            val upstreamPort = if (dnsProfile.sshEnabled && dnsProfile.sshHost.isNotBlank()) {
                val sshProfile = SshProfile(
                    host = dnsProfile.sshHost, port = dnsProfile.sshPort, username = dnsProfile.sshUsername,
                    authType = dnsProfile.sshAuthType, password = dnsProfile.sshPassword,
                    privateKey = dnsProfile.sshPrivateKey, keyPassphrase = dnsProfile.sshKeyPassphrase,
                )
                val controller = SshController(
                    profile = sshProfile, cipher = "auto", compression = false, listenPort = SSH_PORT,
                    proxySocksHost = "127.0.0.1", proxySocksPort = port,
                    proxySocksUser = dnsProfile.socksUser, proxySocksPass = dnsProfile.socksPass,
                )
                ssh = controller
                val sshPort = controller.start()
                if (sshPort < 0) { dns.stop(); ssh = null; VpnManager.onError("SSH-over-DNS failed to connect"); return@thread }
                sshPort
            } else port

            val bypass = resolveHosts(dnsResolverHosts(dnsProfile) + listOfNotNull(dnsProfile.sshHost.takeIf { it.isNotBlank() }))
            if (!connectThrough(upstreamPort, "The DNS tunnel", bypass)) { teardown(); return@thread }
            VpnManager.onConnected(name)
        }
    }

    private fun startMasterDns(name: String, profile: dev.spider.spider.domain.config.MasterDnsProfile) {
        VpnManager.onStarting(name)
        thread(name = "desktop-masterdns") {
            val settings = settingsProvider()
            val effective = if (settings.dnsGlobalResolverEnabled && settings.dnsGlobalResolvers.isNotBlank()) {
                profile.copy(resolvers = settings.dnsGlobalResolvers)
            } else profile
            val engine = dev.spider.spider.desktop.core.DesktopMasterDns(effective, work)
            masterDns = engine
            val port = runCatching { engine.start() }.getOrElse {
                LogBus.append("E/MasterDns ${it.message ?: it}")
                -1
            }
            if (masterDns !== engine) return@thread
            if (port < 0) {
                masterDns = null
                VpnManager.onError("MasterDNS could not connect: ${engine.lastError ?: "unknown error"}")
                return@thread
            }
            if (!connectThrough(port, "MasterDNS", resolveHosts(engine.resolverHosts()))) { teardown(); return@thread }
            VpnManager.onConnected(name)
        }
    }

    private fun startTor(name: String) {
        VpnManager.onStarting(name)
        thread(name = "desktop-tor") {
            val engine = DesktopTor(settingsProvider(), work, engineCgroup = engineCgroupForTun(), socksPort = localPort(LocalPorts.TOR_SOCKS))
            tor = engine
            var reported = -1
            val started = engine.start(onProgress = { pct ->
                if (pct > reported) {
                    reported = pct
                    LogBus.append("I/Tor bootstrapped $pct%")
                }
            })
            if (tor !== engine) return@thread
            started.exceptionOrNull()?.let { e ->
                tor = null
                VpnManager.onError(e.message ?: "Tor failed to start")
                return@thread
            }
            if (!connectThrough(engine.socksPort, "Tor", onion = true)) {
                engine.stop()
                tor = null
                return@thread
            }
            VpnManager.onConnected(name)
        }
    }

    private fun startPsiphon(name: String, profile: PsiphonProfile) {
        VpnManager.onStarting(name)
        thread(name = "desktop-psiphon") {
            val socksPort = localPort(LocalPorts.PSIPHON_SOCKS)
            val httpPort = localPort(LocalPorts.PSIPHON_HTTP, setOf(socksPort))
            val engine = DesktopPsiphon(profile, work, socksPort, httpPort, engineCgroup = engineCgroupForTun())
            psiphon = engine
            val started = engine.start(onNotice = { notice ->
                if (notice.type == "ConnectedServerRegion") {
                    LogBus.append("I/Psiphon exit region ${notice.string("region").orEmpty()}")
                }
            })
            if (psiphon !== engine) return@thread
            started.exceptionOrNull()?.let { e ->
                psiphon = null
                VpnManager.onError(e.message ?: "Psiphon failed to start")
                return@thread
            }
            if (!connectThrough(engine.socksPort, "Psiphon")) {
                engine.stop()
                psiphon = null
                return@thread
            }
            VpnManager.onConnected(name)
        }
    }

    private fun startIkev2(name: String, profile: Ikev2Profile) {
        VpnManager.onStarting(name)
        thread(name = "desktop-ikev2") {
            val engine = DesktopIkev2(profile, work)
            ikev2 = engine
            val connected = engine.connect()
            if (ikev2 !== engine) {
                if (connected.isSuccess) engine.disconnect()
                return@thread
            }
            connected.exceptionOrNull()?.let { e ->
                ikev2 = null
                VpnManager.onError(e.message ?: "IKEv2 failed to connect")
                return@thread
            }
            LogBus.append("I/IKEv2 the system VPN '${DesktopIkev2.NAME}' carries all traffic")
            VpnManager.onConnected(name)
            meter = DesktopMeter(totals = { null }).also { it.start() }
            while (ikev2 === engine) {
                Thread.sleep(15_000)
                if (ikev2 === engine && !engine.isUp()) {
                    ikev2 = null
                    VpnManager.onError("IKEv2 disconnected")
                    break
                }
            }
        }
    }

    private fun startOpenConnect(name: String, profile: dev.spider.spider.domain.config.OpenConnectProfile) {
        VpnManager.onStarting(name)
        thread(name = "desktop-openconnect") {
            val engine = dev.spider.spider.desktop.core.DesktopOpenConnect(profile, work, AdminPassword::ask)
            openConnect = engine
            val connected = engine.connect()
            if (openConnect !== engine) {
                if (connected.isSuccess) engine.disconnect()
                return@thread
            }
            connected.exceptionOrNull()?.let { e ->
                openConnect = null
                VpnManager.onError(e.message ?: "OpenConnect failed to connect")
                return@thread
            }
            LogBus.append("I/OpenConnect the system VPN carries all traffic")
            VpnManager.onConnected(name)
            meter = DesktopMeter(totals = { null }).also { it.start() }
            while (openConnect === engine) {
                Thread.sleep(5_000)
                if (openConnect === engine && !engine.isUp) {
                    openConnect = null
                    engine.disconnect()
                    VpnManager.onError("OpenConnect disconnected")
                    break
                }
            }
        }
    }

    private fun connectThrough(upstreamPort: Int, engine: String, bypass: List<String>? = null, onion: Boolean = false): Boolean {
        val tunReady = runMode == RunMode.Vpn && TunMode.supported()
        val cgroup = engineCgroup.takeIf { bypass == null && tunReady }
        val viaTun = tunReady && (cgroup != null || bypass != null)
        val socksPort = proxyPort(setOf(upstreamPort))
        val metricsPort = localPort(METRICS_PORT, setOf(socksPort, socksPort + 1, upstreamPort))
        val json = DesktopXray.augmentWithMetrics(frontJson(upstreamPort, socksPort, onion), metricsPort)
            .let { if (viaTun && cgroup == null) keepCoreOutside(it) else it }
        val core = adopt(XrayCore(work, engineCgroup = cgroup))
        if (!core.start(json)) {
            VpnManager.onError("Core failed to start")
            return false
        }
        if (!route(socksPort, bypass.orEmpty(), udpOverTcp = false, tunCarries = engine.takeIf { !viaTun }, excludeCgroup = cgroup)) {
            core.stop()
            xray = null
            VpnManager.onError("Could not route traffic (TUN + system proxy failed)")
            return false
        }
        VpnManager.activeSocksPort = socksPort
        DesktopStats(metricsPort).also { stats = it; it.start() }
        shareOnLan()
        return true
    }

    private fun shareOnLan() {
        val settings = settingsProvider()
        if (!settings.proxySharing) return
        val port = settings.effectiveLanSocksPort
        thread(name = "desktop-lan") {
            if (Os.current == Os.WINDOWS) {
                if (!dev.spider.spider.desktop.core.WindowsElevation.ensure(work)) {
                    LogBus.append("W/Desktop LAN sharing needs administrator rights to open port $port in Windows Firewall")
                    return@thread
                }
                dev.spider.spider.desktop.core.WindowsElevation.allowLan(listOf(port, port + 1))
                    .onSuccess {
                        lanOpen = true
                        LogBus.append("I/Desktop Windows Firewall lets other devices reach port $port")
                    }
                    .onFailure { LogBus.append("W/Desktop ${it.message}") }
            } else {
                LogBus.append("I/Desktop LAN proxy is listening on 0.0.0.0:$port")
            }
        }
    }

    private fun frontJson(upstreamPort: Int, socksPort: Int, onion: Boolean): String {
        val settings = settingsProvider()
        val rulesets = dev.spider.spider.domain.config.RoutingMigration.effectiveRulesets(settings)
        val options = settings.toBuildOptions(
            geoAssetsAvailable = geoAssetsAvailable(),
            geoCodes = geoCodes(settings),
            ruleOutbounds = ruleOutboundsProvider(rulesets),
            ruleGroups = ruleGroupsProvider(rulesets),
        ).let(::vpnModeOptions)
        return dev.spider.spider.domain.config.XrayJsonBuilder.buildEngineFront(
            dev.spider.spider.domain.config.XrayJsonBuilder.EngineFront(upstreamPort, onion),
            socksPort,
            options,
            settings.engineRemoteDns(),
        )
    }

    private fun startCrossChain(
        name: String,
        profile: dev.spider.spider.domain.config.VpnProfile,
        config: ConfigRepository,
    ) {
        val (inner, outer) = runCatching { config.crossChainMembers(profile) }.getOrElse {
            VpnManager.onError(it.message ?: "This chain cannot be built"); return
        }
        if (inner.isTor && !dev.spider.spider.core.tor.TorConfigBuilder.supportsUpstreamProxy(settingsProvider())) {
            val refused = dev.spider.spider.data.config.CrossChainUnsupportedException(
                dev.spider.spider.data.config.CrossChainUnsupportedException.DIALER, inner.name,
            )
            VpnManager.onError(refused.message ?: "${inner.name} cannot run through ${outer.name}")
            return
        }
        VpnManager.onStarting(name)
        thread(name = "desktop-chain") {
            LogBus.append("I/Desktop chain: ${inner.name} runs through ${outer.name}")
            val carrierPort = runCatching { startCarrier(outer) }.getOrElse { e ->
                LogBus.append("E/Desktop chain: ${outer.name} did not start: ${e.message}")
                -1
            }
            if (carrierPort <= 0) {
                teardown()
                VpnManager.onError("${outer.name} did not start, so the chain has nothing to run through")
                return@thread
            }
            LogBus.append("I/Desktop chain: ${outer.name} carries on 127.0.0.1:$carrierPort")
            val error = runCatching { startInner(inner, carrierPort) }.getOrElse { it.message ?: "failed" }
            if (error != null) {
                teardown()
                VpnManager.onError("${inner.name} could not run through ${outer.name}: $error")
                return@thread
            }
            VpnManager.onConnected(name)
        }
    }

    private fun startCarrier(outer: dev.spider.spider.domain.config.VpnProfile): Int {
        carrierBypass = null
        outer.psiphonSettings()?.let { psiphonProfile ->
            val socks = localPort(LocalPorts.PSIPHON_SOCKS)
            val http = localPort(LocalPorts.PSIPHON_HTTP, setOf(socks))
            val engine = DesktopPsiphon(psiphonProfile, work, socks, http, engineCgroup = engineCgroupForTun())
            outerPsiphon = engine
            return if (engine.start().isSuccess) socks else -1
        }
        if (outer.isTor) {
            val engine = DesktopTor(settingsProvider(), work, engineCgroup = engineCgroupForTun(), socksPort = localPort(LocalPorts.TOR_SOCKS))
            outerTor = engine
            return if (engine.start(onProgress = { LogBus.append("I/Tor bootstrapped $it%") }).isSuccess) engine.socksPort else -1
        }
        outer.sshSettings()?.let { sshProfile ->
            val controller = SshController(profile = sshProfile, cipher = "auto", compression = false, listenPort = SSH_PORT)
            outerSsh = controller
            carrierBypass = resolveHosts(listOf(sshProfile.host))
            return controller.start()
        }
        outer.dnsTunnelSettings()?.let { dnsProfile ->
            val engine = DesktopDnsTunnel(dnsProfile, work, listenPort = DNS_PORT)
            outerDns = engine
            carrierBypass = resolveHosts(dnsResolverHosts(dnsProfile))
            return engine.start()
        }
        val settings = settingsProvider()
        val options = settings.toBuildOptions(geoAssetsAvailable = geoAssetsAvailable(), geoCodes = geoCodes(settings))
            .copy(carrier = true, appendHttpProxy = false, lan = null)
        val port = localPort(CARRIER_PORT)
        val json = keepCoreOutside(DesktopXray.withSocksPort(xrayJsonOf(outer, options), LocalProxy.SOCKS_PORT, port))
        carrierBypass = resolveHosts(DesktopXray.extractServerHosts(json))
        val core = XrayCore(work)
        carrier = core
        return if (core.start(json, CARRIER_CONFIG)) port else -1
    }

    private fun startInner(inner: dev.spider.spider.domain.config.VpnProfile, carrierPort: Int): String? {
        inner.psiphonSettings()?.let { psiphonProfile ->
            val socks = localPort(LocalPorts.PSIPHON_SOCKS, setOf(carrierPort))
            val http = localPort(LocalPorts.PSIPHON_HTTP, setOf(socks, carrierPort))
            val engine = DesktopPsiphon(psiphonProfile, work, socks, http, upstreamSocksPort = carrierPort)
            psiphon = engine
            engine.start().exceptionOrNull()?.let { return it.message ?: "Psiphon failed to start" }
            return if (connectThrough(engine.socksPort, CHAIN, carrierBypass)) null else "the core did not start"
        }
        if (inner.isTor) {
            val engine = DesktopTor(settingsProvider(), work, upstreamSocksPort = carrierPort, socksPort = localPort(LocalPorts.TOR_SOCKS, setOf(carrierPort)))
            tor = engine
            engine.start(onProgress = { LogBus.append("I/Tor bootstrapped $it%") })
                .exceptionOrNull()?.let { return it.message ?: "Tor failed to start" }
            return if (connectThrough(engine.socksPort, CHAIN, carrierBypass, onion = true)) null else "the core did not start"
        }
        inner.sshSettings()?.let { sshProfile ->
            val controller = SshController(
                profile = sshProfile, cipher = "auto", compression = false,
                listenPort = localPort(SSH_PORT + 5, setOf(carrierPort)),
                proxySocksHost = "127.0.0.1", proxySocksPort = carrierPort,
            )
            ssh = controller
            val port = controller.start()
            if (port < 0) return "SSH failed to connect"
            return if (connectThrough(port, CHAIN, carrierBypass)) null else "the core did not start"
        }
        val settings = settingsProvider()
        val options = settings.toBuildOptions(
            geoAssetsAvailable = geoAssetsAvailable(),
            geoCodes = geoCodes(settings),
            ruleOutbounds = ruleOutboundsProvider(
                dev.spider.spider.domain.config.RoutingMigration.effectiveRulesets(settings),
            ),
            ruleGroups = ruleGroupsProvider(
                dev.spider.spider.domain.config.RoutingMigration.effectiveRulesets(settings),
            ),
        ).let(::vpnModeOptions)
        val chained = dev.spider.spider.domain.config.XrayJsonBuilder.withCarrierProxy(xrayJsonOf(inner, options), carrierPort)
        val socksPort = proxyPort(setOf(carrierPort))
        val metricsPort = localPort(METRICS_PORT, setOf(socksPort, socksPort + 1, carrierPort))
        val bypass = carrierBypass
        val excluded = engineCgroup.takeIf { bypass == null }
        val json = DesktopXray.augmentWithMetrics(
            dev.spider.spider.domain.config.XrayJsonBuilder.withLocalPorts(chained, LocalProxy.SOCKS_PORT, socksPort),
            metricsPort,
        )
            .let { if (bypass != null) keepCoreOutside(it) else it }
        val core = adopt(XrayCore(work, engineCgroup = excluded))
        if (!core.start(json)) return "the core did not start"
        if (!route(socksPort, bypass.orEmpty(), udpOverTcp = false, tunCarries = CHAIN.takeIf { bypass == null && excluded == null }, excludeCgroup = excluded)) {
            return "could not route traffic"
        }
        VpnManager.activeSocksPort = socksPort
        DesktopStats(metricsPort).also { stats = it; it.start() }
        shareOnLan()
        return null
    }

    private fun xrayJsonOf(
        profile: dev.spider.spider.domain.config.VpnProfile,
        options: dev.spider.spider.domain.config.XrayJsonBuilder.BuildOptions,
    ): String = if (profile.isProxyChain) {
        chainConfigProvider(profile, options) ?: error("Proxy chain members are unavailable")
    } else {
        profile.toXrayConfigJson(options)
    }

    private fun dnsResolverHosts(p: DnsTunnelProfile): List<String> =
        if (p.dnsTransport == DnsTunnelProfile.TRANSPORT_DOH) {
            runCatching { java.net.URI(p.dohUrl.trim()).host }.getOrNull()?.let { listOf(it) } ?: emptyList()
        } else {
            p.resolvers.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.map { hp ->
                if (hp.startsWith("[")) hp.substringAfter('[').substringBefore(']')
                else hp.substringBefore(':')
            }
        }

    private fun startXray(
        name: String,
        profile: dev.spider.spider.domain.config.VpnProfile,
        config: ConfigRepository,
    ) {
        val settings = settingsProvider()
        val options = settings.toBuildOptions(
            geoAssetsAvailable = geoAssetsAvailable(),
            geoCodes = geoCodes(settings),
            ruleOutbounds = ruleOutboundsProvider(
                dev.spider.spider.domain.config.RoutingMigration.effectiveRulesets(settings),
            ),
            ruleGroups = ruleGroupsProvider(
                dev.spider.spider.domain.config.RoutingMigration.effectiveRulesets(settings),
            ),
        ).let(::vpnModeOptions)

        dev.spider.spider.core.AutoSelect.clearPrepared()
        val baseJson = runCatching {
            if (profile.isAutoSelect) {
                val build = config.buildAutoSelectConfig(
                    profile,
                    options,
                    settings.autoSelectTuning(),
                )
                dev.spider.spider.core.AutoSelect.prepare(profile.id, build.memberProfiles)
                build.json
            } else {
                xrayJsonOf(profile, options)
            }
        }.getOrElse {
            VpnManager.onError(it.message ?: "This config type isn't supported on desktop yet"); return
        }
        VpnManager.onStarting(name)
        thread(name = "desktop-xray") {
            val socksPort = proxyPort()
            val metricsPort = localPort(METRICS_PORT, setOf(socksPort, socksPort + 1))
            val ported = DesktopXray.augmentWithMetrics(
                dev.spider.spider.domain.config.XrayJsonBuilder.withLocalPorts(baseJson, LocalProxy.SOCKS_PORT, socksPort),
                metricsPort,
            )
            val json = keepCoreOutside(ported)
            val core = adopt(XrayCore(work))
            if (!core.start(json)) { VpnManager.onError("Core failed to start"); return@thread }
            dev.spider.spider.core.AutoSelect.activate()
            val bypass = resolveHosts(DesktopXray.extractServerHosts(json))
            if (!route(socksPort, bypass, udpOverTcp = false)) {
                core.stop(); xray = null
                VpnManager.onError("Could not route traffic (TUN + system proxy failed)"); return@thread
            }
            VpnManager.activeSocksPort = socksPort
            DesktopStats(metricsPort).also { stats = it; it.start() }
            shareOnLan()
            VpnManager.onConnected(name)
        }
    }

    private fun startSingBoxConfig(name: String, profile: dev.spider.spider.domain.config.VpnProfile) {
        val source = profile.source as? dev.spider.spider.domain.config.ProfileSource.SingBoxConfig ?: return
        VpnManager.onStarting(name)
        thread(name = "desktop-singbox") {
            val socksPort = proxyPort()
            val prepared = runCatching {
                dev.spider.spider.domain.config.SingBoxConfigs.prepareForDesktop(
                    source.json,
                    socksPort = socksPort,
                    clashApiPort = localPort(METRICS_PORT, setOf(socksPort)),
                )
            }.getOrElse { VpnManager.onError(it.message ?: "Not a usable sing-box config"); return@thread }
            val core = adopt(XrayCore(work))
            if (!core.startSingBox(prepared.json)) { VpnManager.onError("sing-box failed to start"); return@thread }

            val hosts = dev.spider.spider.domain.config.SingBoxJson.servers(source.json).mapNotNull { it.address }
            if (!route(prepared.socksPort, resolveHosts(hosts), udpOverTcp = false)) {
                core.stop(); xray = null
                VpnManager.onError("Could not route traffic (TUN + system proxy failed)"); return@thread
            }
            VpnManager.activeSocksPort = prepared.socksPort
            DesktopSingBoxStats(prepared.clashApi, prepared.clashSecret).also { singBoxStats = it; it.start() }
            VpnManager.onConnected(name)
        }
    }

    private fun startSsh(name: String, sshProfile: dev.spider.spider.domain.config.SshProfile) {
        VpnManager.onStarting(name)
        thread(name = "desktop-ssh") {
            val controller = SshController(
                profile = sshProfile,
                cipher = "auto",
                compression = false,
                listenPort = SSH_PORT,
            )
            ssh = controller
            val sshPort = controller.start()
            if (sshPort < 0) { VpnManager.onError("SSH failed to connect"); return@thread }

            if (!connectThrough(sshPort, "SSH", resolveHosts(listOf(sshProfile.host)))) {
                controller.stop(); ssh = null
                return@thread
            }
            VpnManager.onConnected(name)
        }
    }

    private fun proxyPort(taken: Set<Int> = emptySet()): Int {
        val settings = settingsProvider()
        val preferred = settings.effectiveLocalProxyPort
        if (settings.dynamicSocksPort) {
            return LocalPortPicker.pickRandomPair(taken).also { port ->
                LogBus.append("I/Desktop random local proxy port for this session: $port")
            }
        }
        if (preferred !in taken && preferred + 1 !in taken) {
            val deadline = System.currentTimeMillis() + PORT_WAIT_MS
            while (true) {
                if (LocalPortPicker.isFree(preferred) && LocalPortPicker.isFree(preferred + 1)) return preferred
                if (System.currentTimeMillis() >= deadline) break
                Thread.sleep(PORT_RETRY_MS)
            }
        }
        return LocalPortPicker.pickPair(preferred + 2, taken).also { port ->
            LogBus.append("W/Desktop port $preferred is in use by another program; using $port instead")
        }
    }

    private fun localPort(preferred: Int, taken: Set<Int> = emptySet()): Int =
        LocalPortPicker.pick(preferred, taken).also { port ->
            if (port != preferred) {
                LogBus.append("W/Desktop port $preferred is in use by another program; using $port instead")
            }
        }

    private fun engineCgroupForTun(): String? {
        if (runMode != RunMode.Vpn || Os.current != Os.LINUX) return null
        engineCgroup?.let { return it }
        return (EngineCgroup.create() ?: rootCgroup()).also { created ->
            engineCgroup = created
            if (created == null) LogBus.append("W/Desktop could not give the engine its own cgroup here; VPN mode will use the system proxy for it")
        }
    }

    private fun rootCgroup(): String? {
        if (!dev.spider.spider.desktop.core.LinuxBroker.ensure(work, AdminPassword::ask)) return null
        return dev.spider.spider.desktop.core.LinuxBroker.engineCgroup()
            .onFailure { LogBus.append("W/Desktop ${it.message}") }
            .getOrNull()
            ?.also { LogBus.append("I/Desktop the root helper keeps the engine in its own cgroup") }
    }

    private fun route(
        socksPort: Int,
        bypassIps: List<String>,
        udpOverTcp: Boolean,
        tunCarries: String? = null,
        excludeCgroup: String? = null,
    ): Boolean {
        if (runMode == RunMode.ProxyOnly) {
            LogBus.append("I/Desktop SOCKS and HTTP proxy on 127.0.0.1:$socksPort; the system proxy is left alone")
            return true
        }
        if (runMode == RunMode.Vpn && tunCarries != null) {
            LogBus.append("W/Desktop $tunCarries reaches too many servers to route around a tunnel; using the system proxy")
            if (tunCarries == CHAIN) LogBus.append("I/Desktop VPN mode needs a chain whose first hop is Xray, SSH or a DNS tunnel")
        }
        val useTun = runMode == RunMode.Vpn && tunCarries == null
        if (useTun && !TunMode.supported()) {
            LogBus.append("W/Desktop VPN mode is not available on ${Os.current} yet; using the system proxy")
        }
        if (useTun && TunMode.supported()) {
            LogBus.append("I/Desktop TUN bypass: ${bypassIps.joinToString().ifEmpty { "(none)" }}")
            excludeCgroup?.let { LogBus.append("I/Desktop the engine runs outside the tunnel from its own cgroup") }
            val settings = settingsProvider()
            val engine = TunMode.Factory.create(
                work, socksPort, bypassIps, udpOverTcp, settings.vpnDns, AdminPassword::ask,
                allowApps = listOf("xray.exe", "zeddns.exe").map { File(work, it) }.filter { it.isFile },
                zeptunOnWindows = settings.useZepTun,
                hevOnMac = settings.useHevOnMac,
                excludeCgroup = excludeCgroup,
            )
            if (engine != null && engine.start()) {
                tun = engine
                if (settings.tunSystemProxy && SystemProxy.set("127.0.0.1", socksPort)) {
                    usedSystemProxy = true
                    LogBus.append("I/Desktop the system proxy also points at 127.0.0.1:$socksPort while the tunnel is up")
                }
                return true
            }
            LogBus.append("W/Desktop TUN unavailable — falling back to system proxy")
        }
        return if (SystemProxy.set("127.0.0.1", socksPort)) { usedSystemProxy = true; true } else false
    }

    private fun vpnModeOptions(options: dev.spider.spider.domain.config.XrayJsonBuilder.BuildOptions) = when {
        runMode != RunMode.Vpn -> options
        options.directDns.trim().equals("localhost", ignoreCase = true) -> {
            LogBus.append("I/Desktop VPN mode would send the system resolver back into the tunnel; direct DNS uses ${AppSettings().directDns}")
            options.copy(directDns = AppSettings().directDns, tunTraffic = true)
        }
        else -> options.copy(tunTraffic = true)
    }

    private fun keepCoreOutside(json: String): String {
        if (runMode != RunMode.Vpn || !TunMode.supported()) return json
        val iface = PhysicalInterface.detect()
        if (iface == null || !PhysicalInterface.canBind()) {
            LogBus.append("W/Desktop could not find the network adapter; only the servers are kept outside the tunnel")
            return json
        }
        LogBus.append("I/Desktop the core's own connections leave through $iface, outside the tunnel")
        return DesktopXray.bindOutbounds(json, iface, pinServerAddresses(DesktopXray.extractServerHosts(json)))
    }

    private fun pinServerAddresses(hosts: List<String>): Map<String, List<String>> =
        hosts.filter { host -> host.any { it.isLetter() } && ':' !in host }.associateWith { host ->
            val all = runCatching { InetAddress.getAllByName(host).toList() }.getOrDefault(emptyList())
            val picked = all.filterIsInstance<java.net.Inet4Address>().ifEmpty { all }.mapNotNull { it.hostAddress }.distinct()
            val clean = dev.spider.spider.domain.config.DnsGuard.clean(picked)
            if (clean.isNotEmpty() || picked.isEmpty()) {
                clean
            } else {
                LogBus.append("W/DNS: $host resolved to ${picked.joinToString()}, a filtering block page; asking DNS over HTTPS instead")
                listOfNotNull(kotlinx.coroutines.runBlocking { dev.spider.spider.domain.config.DnsGuard.lookup(host) })
            }
        }.filterValues { it.isNotEmpty() }

    private fun resolveHosts(hosts: List<String>): List<String> = hosts.flatMap { host ->
        runCatching { InetAddress.getAllByName(host).map { it.hostAddress } }.getOrDefault(emptyList())
    }.distinct()

    fun reconnect(config: ConfigRepository) {
        val state = VpnManager.status.value.state
        if (state != dev.spider.spider.domain.model.ConnectionState.Connected) return
        thread(name = "desktop-reconnect") {
            stop()
            val deadline = System.currentTimeMillis() + 15_000
            while (VpnManager.status.value.state != dev.spider.spider.domain.model.ConnectionState.Idle && System.currentTimeMillis() < deadline) {
                Thread.sleep(100)
            }
            if (VpnManager.status.value.state == dev.spider.spider.domain.model.ConnectionState.Idle) toggle(config)
        }
    }

    fun stop() {
        if (!VpnManager.onStopping()) return
        thread(name = "desktop-vpn-stop") {
            teardown()
            VpnManager.onDisconnected()
        }
    }

    fun shutdown() {
        teardown()
        dev.spider.spider.desktop.core.LinuxBroker.close()
        dev.spider.spider.desktop.core.WindowsElevation.close()
        dev.spider.spider.desktop.core.MacTunHelper.close()
    }

    fun repairLeftovers() {
        if (Os.current != Os.MACOS || !dev.spider.spider.desktop.core.MacTunHelper.dnsLeftBehind) return
        thread(name = "desktop-repair") {
            val repaired = dev.spider.spider.desktop.core.MacTunHelper.repair(
                work,
                dev.spider.spider.desktop.core.ZeptunBinary.extract(work),
            )
            LogBus.append(
                if (repaired) "I/Desktop the DNS settings an earlier tunnel left behind are back to normal"
                else "W/Desktop the DNS settings an earlier tunnel left behind could not be put back yet",
            )
        }
    }

    private fun tunnelLost() {
        if (tun != null && VpnManager.status.value.state.isActive) {
            LogBus.append("E/Desktop the tunnel engine stopped; disconnecting so traffic does not leave outside the VPN")
            thread(name = "desktop-tun-lost") {
                teardown()
                VpnManager.onError("The tunnel stopped unexpectedly")
            }
        }
    }

    init {
        dev.spider.spider.desktop.core.LinuxBroker.onTunLost = { tunnelLost() }
        dev.spider.spider.desktop.core.WindowsElevation.onLost = { what -> if (what == "tun") tunnelLost() }
    }

    @Synchronized
    private fun adopt(core: XrayCore): XrayCore {
        xray?.takeIf { it !== core }?.stop()
        xray = core
        return core
    }

    @Synchronized
    private fun teardown() {
        val wasFirefoxVpn = firefoxVpnActive
        firefoxVpnActive = false
        meter?.stop(); meter = null
        stats?.stop(); stats = null
        singBoxStats?.stop(); singBoxStats = null
        aether?.let { aether = null; it.stop() }
        mhr?.let { mhr = null; it.stop() }
        soar?.let { soar = null; it.stop() }
        vpeen?.let { vpeen = null; it.stop() }
        if (wasFirefoxVpn) runCatching {
            kotlinx.coroutines.runBlocking {
                dev.spider.spider.platform.firefoxVpnRequest("POST", "power", "{\"on\":false}")
            }
        }
        tun?.stop(); tun = null
        tor?.let { tor = null; it.stop() }
        ikev2?.let { ikev2 = null; it.disconnect() }
        openConnect?.let { openConnect = null; it.disconnect() }
        psiphon?.let { psiphon = null; it.stop() }
        masterDns?.let { masterDns = null; it.stop() }
        ssh?.stop(); ssh = null
        dnsTunnel?.stop(); dnsTunnel = null
        if (usedSystemProxy) { runCatching { SystemProxy.clear() }; usedSystemProxy = false }
        xray?.stop(); xray = null
        carrier?.let { carrier = null; it.stop() }
        outerPsiphon?.let { outerPsiphon = null; it.stop() }
        outerTor?.let { outerTor = null; it.stop() }
        outerSsh?.let { outerSsh = null; it.stop() }
        outerDns?.let { outerDns = null; it.stop() }
        engineCgroup?.let { engineCgroup = null; EngineCgroup.remove(it) }
        if (lanOpen) {
            lanOpen = false
            dev.spider.spider.desktop.core.WindowsElevation.closeLan()
        }
        dev.spider.spider.core.AutoSelect.end()
    }
}
