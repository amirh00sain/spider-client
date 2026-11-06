package dev.spider.spider.platform

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal object FirefoxVpnHttp {
    fun request(dataDir: File, method: String, path: String, body: String?): String {
        val token = File(dataDir, "local-api.token").readText().trim()
        val config = File(dataDir, "config.json").takeIf { it.isFile }?.readText().orEmpty()
        val listen = Regex("\"dashboard\"\\s*:\\s*\\{[^}]*\"listen\"\\s*:\\s*\"([^\"]+)\"")
            .find(config)?.groupValues?.get(1)
            ?: "127.0.0.1:10820"
        val port = listen.substringAfterLast(':').toIntOrNull() ?: 10820
        val url = URL("http://127.0.0.1:$port/api/${path.removePrefix("/").removePrefix("api/")}")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 30_000
            setRequestProperty("X-Local-Token", token)
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Firefox VPN API ($code): $response")
            return response
        } finally {
            connection.disconnect()
        }
    }
}
