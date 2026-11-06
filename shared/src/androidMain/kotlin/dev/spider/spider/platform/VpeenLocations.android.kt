package dev.spider.spider.platform

import dev.spider.spider.core.VpeenEngine
import dev.spider.spider.domain.config.VpeenLocation

actual suspend fun loadVpeenLocations(): List<VpeenLocation> = VpeenEngine.fetchFreeLocations()
