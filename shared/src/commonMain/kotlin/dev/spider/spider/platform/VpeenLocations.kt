package dev.spider.spider.platform

import dev.spider.spider.domain.config.VpeenLocation

expect suspend fun loadVpeenLocations(): List<VpeenLocation>
