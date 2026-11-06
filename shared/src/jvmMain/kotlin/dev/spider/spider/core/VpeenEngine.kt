@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package dev.spider.spider.core

import kotlinx.serialization.json.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Properties
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import dev.spider.spider.domain.config.VpeenLocation
import kotlin.io.encoding.Base64

/** Small JVM implementation of the free VeePN extension protocol used by VPeeN. */
class VpeenEngine(
    private val stateDir: File,
    private val socketFactory: (String, Int) -> Socket = { host, port ->
        Socket().apply { connect(InetSocketAddress(host, port), 12_000); soTimeout = 15_000 }
    },
    private val log: (String) -> Unit = {},
) {
    @Volatile private var running = false
    val isRunning: Boolean get() = running && listener?.isClosed == false
    @Volatile private var listener: ServerSocket? = null
    @Volatile private var region = ""
    @Volatile private var servers: List<JsonObject> = emptyList()
    private val workers = Executors.newCachedThreadPool { r -> Thread(r, "spider-vpeen-io").apply { isDaemon = true } }
    private val clients = CopyOnWriteArrayList<Socket>()
    private val api = VpeenApi(stateDir, socketFactory, log)

    fun start(requestedRegion: String, port: Int, localUser: String? = null, localPassword: String? = null): Int {
        check(!running) { "VPeeN is already running" }
        val locations = api.freeLocations()
        val available = locations.map { it.region }.toSet()
        region = requestedRegion.takeIf { it.isNotBlank() && it in available }
            ?: api.optimalRegion()?.takeIf { it in available }
            ?: locations.firstOrNull { it.region == "nl" }?.region
            ?: locations.firstOrNull()?.region
            ?: error("VeePN returned no free locations")
        servers = api.serverList(region)
        check(servers.isNotEmpty()) { "No upstream servers are available for $region" }
        val bound = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress("127.0.0.1", port))
        }
        listener = bound
        running = true
        log("VeePN acquired ${servers.size} upstream servers for $region")
        workers.execute {
            while (running) {
                try {
                    val socket = bound.accept().apply { soTimeout = 0 }
                    clients += socket
                    workers.execute { serveSocks(socket, localUser, localPassword) }
                } catch (e: Exception) {
                    if (running) log("VeePN local proxy stopped: ${e.message}")
                }
            }
        }
        return bound.localPort
    }

    fun stop() {
        running = false
        runCatching { listener?.close() }
        listener = null
        clients.forEach { runCatching { it.close() } }
        clients.clear()
        workers.shutdownNow()
        api.close()
    }

    private fun serveSocks(client: Socket, username: String?, password: String?) {
        var upstream: Socket? = null
        try {
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            val greeting = readExact(input, 2)
            if (greeting.size != 2 || greeting[0].toInt() != 5) return
            val methods = readExact(input, greeting[1].toInt() and 0xff).map { it.toInt() and 0xff }
            val authRequired = !username.isNullOrBlank() && methods.contains(2)
            val chosen = if (authRequired) 2 else if (methods.contains(0)) 0 else 0xff
            output.write(byteArrayOf(5, chosen.toByte())); output.flush()
            if (chosen == 0xff) return
            if (chosen == 2) {
                val ver = input.read(); val nUser = input.read()
                if (ver != 1 || nUser < 0) return
                val gotUser = String(readExact(input, nUser), Charsets.UTF_8)
                val nPass = input.read()
                if (nPass < 0) return
                val gotPass = String(readExact(input, nPass), Charsets.UTF_8)
                val ok = gotUser == username && gotPass == password
                output.write(byteArrayOf(1, if (ok) 0 else 1)); output.flush()
                if (!ok) return
            }
            val request = readExact(input, 4)
            if (request.size != 4 || request[0].toInt() != 5 || request[1].toInt() != 1) {
                socksReply(output, 7); return
            }
            val host = when (request[3].toInt() and 0xff) {
                1 -> readExact(input, 4).joinToString(".") { (it.toInt() and 0xff).toString() }
                3 -> String(readExact(input, input.read().also { if (it < 0) throw IllegalStateException("bad domain") }), Charsets.UTF_8)
                4 -> InetSocketAddress(java.net.InetAddress.getByAddress(readExact(input, 16)), 0).hostString
                else -> { socksReply(output, 8); return }
            }
            val portBytes = readExact(input, 2)
            if (host.isBlank() || portBytes.size != 2) { socksReply(output, 1); return }
            val port = ((portBytes[0].toInt() and 0xff) shl 8) or (portBytes[1].toInt() and 0xff)
            try {
                val target = connectThroughVpeen(host, port)
                upstream = target
                socksReply(output, 0)
                val pump = workers.submit { copy(client, target) }
                copy(target, client)
                pump.get()
            } catch (e: Exception) {
                log("VeePN connection failed: ${e.message}")
                runCatching { socksReply(output, 5) }
            }
        } catch (_: Exception) {
            // Peer disconnected or sent a malformed SOCKS request.
        } finally {
            runCatching { upstream?.close() }
            runCatching { client.close() }
            clients.remove(client)
        }
    }

    private fun connectThroughVpeen(host: String, port: Int): Socket {
        var last: Exception? = null
        for (server in servers) {
            try { return api.connectTunnel(server, host, port) }
            catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("No VeePN server available")
    }

    private fun socksReply(out: BufferedOutputStream, code: Int) {
        out.write(byteArrayOf(5, code.toByte(), 0, 1, 0, 0, 0, 0, 0, 0)); out.flush()
    }

    private fun copy(from: Socket, to: Socket) {
        runCatching {
            val buf = ByteArray(64 * 1024)
            val src = from.getInputStream(); val dst = to.getOutputStream()
            while (true) {
                val n = src.read(buf); if (n < 0) break
                dst.write(buf, 0, n); dst.flush()
            }
        }
        runCatching { to.shutdownOutput() }
    }

    companion object {
        fun fetchFreeLocations(log: (String) -> Unit = {}): List<VpeenLocation> =
            VpeenApi(File(System.getProperty("java.io.tmpdir"), "spider-vpeen-state"), { h, p -> Socket().apply { connect(InetSocketAddress(h, p), 10_000); soTimeout = 12_000 } }, log)
                .let { it.freeLocations() }
    }
}

private class VpeenApi(
    private val stateDir: File,
    private val socketFactory: (String, Int) -> Socket,
    private val log: (String) -> Unit,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val domains = listOf("antpeak.com", "hibchr.com", "hisball.com", "bitphox.com", "freloop.com", "tronlit.com", "tronyza.com")
    private val stateFile = File(stateDir, "vpeen.properties")
    private val state = Properties().apply { runCatching { stateFile.inputStream().use { load(it) } } }
    @Volatile private var token = state.getProperty("token").orEmpty()
    @Volatile private var tokenAt = state.getProperty("token_at")?.toLongOrNull() ?: 0L
    @Volatile private var udid = state.getProperty("udid") ?: UUID.randomUUID().toString()

    fun freeLocations(): List<VpeenLocation> {
        val payload = request("/v3/location/extension/")
        val values = (payload as? JsonObject)?.get("locations") as? JsonArray ?: return emptyList()
        return values.mapNotNull { raw ->
            val item = raw as? JsonObject ?: return@mapNotNull null
            if (item["proxyType"]?.jsonPrimitive?.intOrNull != 0) return@mapNotNull null
            val region = item["region"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (region.isBlank()) return@mapNotNull null
            VpeenLocation(region, item["name"]?.jsonPrimitive?.contentOrNull ?: region, item["countryCode"]?.jsonPrimitive?.contentOrNull.orEmpty())
        }.distinctBy { it.region }.sortedBy { it.name }
    }

    fun optimalRegion(): String? = runCatching {
        (request("/v3/location/optimal/") as? JsonObject)?.get("region")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    fun serverList(region: String): List<JsonObject> {
        val payload = request("/v3/server/list/", "POST", buildJsonObject {
            put("protocol", "https"); put("region", region); put("type", 0)
        }) as? JsonArray ?: error("No VeePN servers returned for $region")
        return payload.mapNotNull { (it as? JsonObject)?.takeIf { obj -> obj["addresses"] is JsonArray && obj["port"] != null } }
    }

    fun connectTunnel(server: JsonObject, target: String, port: Int): Socket {
        val addressList = (server["addresses"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        val serverPort = server["port"]?.jsonPrimitive?.intOrNull ?: error("Invalid VeePN server port")
        var last: Exception? = null
        for (address in addressList) {
            var raw: Socket? = null
            try {
                raw = socketFactory(address, serverPort)
                val tls = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(raw, address, serverPort, true) as SSLSocket
                // VeePN publishes proxy endpoints as IP addresses and its
                // reference client does not validate proxy certificate names.
                // Keep the platform's CA chain validation, while avoiding a
                // hostname check against the raw IP address.
                tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = null }
                tls.soTimeout = 15_000
                tls.startHandshake()
                val auth = Base64.encode("${server["username"]?.jsonPrimitive?.content.orEmpty()}:${server["password"]?.jsonPrimitive?.content.orEmpty()}".toByteArray())
                val out = BufferedOutputStream(tls.getOutputStream())
                out.write("CONNECT ${formatHost(target)}:$port HTTP/1.1\r\nHost: ${formatHost(target)}:$port\r\nProxy-Authorization: Basic $auth\r\nUser-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36\r\nProxy-Connection: Keep-Alive\r\n\r\n".toByteArray(Charsets.ISO_8859_1)); out.flush()
                val input = BufferedInputStream(tls.getInputStream())
                val status = readLine(input).orEmpty()
                while (readLine(input)?.isNotEmpty() == true) Unit
                if (!status.contains(" 200 ") && !status.endsWith(" 200")) { tls.close(); throw IllegalStateException("VeePN proxy rejected CONNECT: $status") }
                tls.soTimeout = 0
                return tls
            } catch (e: Exception) { runCatching { raw?.close() }; last = e }
        }
        throw last ?: IllegalStateException("VeePN server has no usable addresses")
    }

    private fun request(path: String, method: String = "GET", body: JsonObject? = null, forceToken: Boolean = false): JsonElement {
        var last: Exception? = null
        for (domain in domains) {
            try {
                val access = if (path == "/v3/launch/") null else ensureToken(forceToken)
                val data = body?.toString()?.toByteArray() ?: ByteArray(0)
                val response = http(domain, path, method, data, access)
                if (response.first in 200..299) return runCatching { json.parseToJsonElement(response.second) }.getOrElse { JsonPrimitive(response.second) }
                if (response.first == 401 || response.first == 403) { tokenAt = 0; continue }
                if (response.first == 429 || response.first >= 500) { last = IllegalStateException("HTTP ${response.first}"); continue }
                throw IllegalStateException("VeePN API HTTP ${response.first}: ${response.second.take(180)}")
            } catch (e: Exception) { last = e; log("VeePN API $domain failed: ${e.message}") }
        }
        throw last ?: IllegalStateException("VeePN API is unavailable")
    }

    private fun ensureToken(force: Boolean = false): String {
        if (!force && token.isNotBlank() && System.currentTimeMillis() - tokenAt < 7L * 24 * 60 * 60 * 1000) return token
        val launch = buildJsonObject {
            put("udid", udid); put("appVersion", "5.0.2"); put("platform", "chrome")
            put("platformVersion", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            put("timeZone", TimeZone.getDefault().id); put("deviceName", "Chrome 130")
        }.toString().toByteArray()
        var last: Exception? = null
        for (domain in domains) try {
            val (status, raw) = http(domain, "/v3/launch/", "POST", launch, null)
            val access = (json.parseToJsonElement(raw) as? JsonObject)?.get("access")?.jsonPrimitive?.contentOrNull
            if (status in 200..299 && !access.isNullOrBlank()) {
                token = access; tokenAt = System.currentTimeMillis(); persist(); return token
            }
            last = IllegalStateException("VeePN launch failed (HTTP $status)")
        } catch (e: Exception) { last = e }
        throw last ?: IllegalStateException("VeePN could not acquire an access token")
    }

    private fun http(domain: String, path: String, method: String, body: ByteArray, bearer: String?): Pair<Int, String> {
        val host = domain.substringBefore(':')
        val port = domain.substringAfter(':', "443").toIntOrNull() ?: 443
        val raw = socketFactory(host, port)
        val tls = try { (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(raw, host, port, true) as SSLSocket } catch (e: Exception) { raw.close(); throw e }
        tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
        tls.soTimeout = 15_000
        try {
            tls.startHandshake()
            val out = BufferedOutputStream(tls.getOutputStream())
            val headers = buildString {
                append("$method $path HTTP/1.1\r\nHost: $host\r\nConnection: close\r\nAccept: application/json\r\nUser-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36\r\n")
                append("Content-Type: application/json\r\n")
                if (bearer != null) append("Authorization: Bearer $bearer\r\n")
                if (body.isNotEmpty()) append("Content-Length: ${body.size}\r\n")
                append("\r\n")
            }
            out.write(headers.toByteArray(Charsets.ISO_8859_1)); out.write(body); out.flush()
            val input = BufferedInputStream(tls.getInputStream())
            val status = readLine(input).orEmpty().split(' ').getOrNull(1)?.toIntOrNull() ?: error("Invalid VeePN API response")
            val responseHeaders = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(input) ?: break
                if (line.isEmpty()) break
                val at = line.indexOf(':'); if (at > 0) responseHeaders[line.substring(0, at).trim().lowercase()] = line.substring(at + 1).trim()
            }
            val bytes = if (responseHeaders["transfer-encoding"]?.contains("chunked", true) == true) readChunked(input) else {
                val length = responseHeaders["content-length"]?.toIntOrNull()
                if (length == null) input.readBytes() else readExact(input, length)
            }
            return status to bytes.toString(Charsets.UTF_8)
        } finally { runCatching { tls.close() } }
    }

    private fun readChunked(input: BufferedInputStream): ByteArray {
        val output = ByteArrayOutputStream()
        while (true) {
            val line = readLine(input)?.substringBefore(';')?.trim() ?: break
            val size = line.toIntOrNull(16) ?: break
            if (size == 0) { while (readLine(input)?.isNotEmpty() == true) Unit; break }
            output.write(readExact(input, size)); readLine(input)
        }
        return output.toByteArray()
    }

    private fun persist() {
        runCatching {
            stateDir.mkdirs()
            state.setProperty("udid", udid); state.setProperty("token", token); state.setProperty("token_at", tokenAt.toString())
            File(stateDir, ".vpeen.tmp").outputStream().use { state.store(it, null) }
            File(stateDir, ".vpeen.tmp").setReadable(false, false); File(stateDir, ".vpeen.tmp").setReadable(true, true)
            File(stateDir, ".vpeen.tmp").setWritable(false, false); File(stateDir, ".vpeen.tmp").setWritable(true, true)
            File(stateDir, ".vpeen.tmp").renameTo(stateFile)
        }
    }

    fun close() = Unit

    companion object {
        private fun readLine(input: BufferedInputStream): String? {
            val out = ByteArrayOutputStream()
            while (true) { val b = input.read(); if (b < 0) return if (out.size() == 0) null else out.toString("ISO-8859-1"); if (b == 10) break; if (b != 13) out.write(b) }
            return out.toString("ISO-8859-1")
        }
        private fun formatHost(host: String) = if (':' in host && !host.startsWith("[")) "[$host]" else host
    }
}

private fun readExact(input: java.io.InputStream, size: Int): ByteArray {
    require(size >= 0)
    val result = ByteArray(size)
    var offset = 0
    while (offset < size) {
        val count = input.read(result, offset, size - offset)
        if (count < 0) throw java.io.EOFException("Unexpected end of stream")
        offset += count
    }
    return result
}
