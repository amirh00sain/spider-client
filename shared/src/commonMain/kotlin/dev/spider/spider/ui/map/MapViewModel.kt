package dev.spider.spider.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.spider.spider.data.map.WorldMap
import dev.spider.spider.data.net.IpDetails
import dev.spider.spider.data.net.IpDetailsRepository
import dev.spider.spider.data.net.NetworkInfo
import dev.spider.spider.data.net.NetworkInfoRepository
import dev.spider.spider.platform.currentTimeMillis
import dev.spider.spider.core.VpnManager
import dev.spider.spider.domain.model.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: NetworkInfoRepository = NetworkInfoRepository(),
    private val detailsRepository: IpDetailsRepository = IpDetailsRepository(),
) : ViewModel() {
    private val _countries = MutableStateFlow<List<WorldMap.Country>>(emptyList())
    val countries = _countries.asStateFlow()

    private val _info = MutableStateFlow(NetworkInfo())
    val info = _info.asStateFlow()

    var preferredIpApiUrl: String? = null

    private var job: Job? = null

    private val _details = MutableStateFlow(IpDetails())
    val details = _details.asStateFlow()

    private var detailsJob: Job? = null
    private var detailsKey: String? = null

    init {
        viewModelScope.launch { _countries.value = WorldMap.countries() }
    }

    fun refresh(force: Boolean = false) {
        job?.cancel()
        _info.value = _info.value.copy(loading = true, pingLoading = true, error = null)
        job = viewModelScope.launch(Dispatchers.IO) {
            val ping = async {
                if (VpnManager.status.value.state == ConnectionState.Connected) runCatching { repository.realDelay() }.getOrNull() else null
            }
            val resolved = repository.fetchInfo(preferredIpApiUrl, force = force)
            _info.value = resolved.copy(loading = false, pingLoading = false, pingMs = ping.await() ?: resolved.pingMs)
        }
    }

    fun loadDetails(force: Boolean = false) {
        val status = VpnManager.status.value
        val key = "${status.sessionId}/${status.state}/${dev.spider.spider.core.AutoSelect.exitGeneration()}"
        val shown = _details.value
        if (!force && key == detailsKey && shown.fetchedAt > 0 && currentTimeMillis() - shown.fetchedAt < DETAILS_TTL_MS) return
        if (!force && shown.loading && key == detailsKey) return
        detailsJob?.cancel()
        detailsKey = key
        _details.value = shown.copy(loading = true, failed = false)
        detailsJob = viewModelScope.launch(Dispatchers.IO) {
            val next = try {
                detailsRepository.fetch()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                IpDetails(failed = true)
            }
            _details.value = next.copy(loading = false)
        }
    }

    private companion object {
        const val DETAILS_TTL_MS = 60_000L
    }
}
