package dev.spider.spider.ui.spider

enum class SpiderTileSize(val code: Char, val cols: Int, val rows: Int) {
    Small('s', 1, 1),
    Wide('w', 2, 1),
    Tall('t', 1, 2);

    fun next(): SpiderTileSize = when (this) {
        Small -> Wide
        Wide -> Tall
        Tall -> Small
    }

    companion object {
        fun of(code: Char): SpiderTileSize? = entries.firstOrNull { it.code == code }
    }
}

data class SpiderTileSlot(val key: String, val size: SpiderTileSize)

data class SpiderTileLayout(val visible: List<SpiderTileSlot>, val hidden: List<String>) {
    fun without(key: String): SpiderTileLayout =
        SpiderTileLayout(visible.filterNot { it.key == key }, (hidden + key).distinct())

    fun with(key: String, size: SpiderTileSize = SpiderTiles.defaultSize(key)): SpiderTileLayout =
        SpiderTileLayout(visible.filterNot { it.key == key } + SpiderTileSlot(key, size), hidden - key)

    fun resized(key: String): SpiderTileLayout =
        copy(visible = visible.map { if (it.key == key) it.copy(size = it.size.next()) else it })

    fun moved(key: String, toIndex: Int): SpiderTileLayout {
        val from = visible.indexOfFirst { it.key == key }
        if (from < 0) return this
        val list = visible.toMutableList()
        val slot = list.removeAt(from)
        list.add(toIndex.coerceIn(0, list.size), slot)
        return copy(visible = list)
    }
}

data class SpiderPlaced(val slot: SpiderTileSlot, val col: Int, val row: Int, val cols: Int, val rows: Int)

object SpiderTiles {
    val DEFAULT: List<SpiderTileSlot> = listOf(
        SpiderTileSlot("appearance", SpiderTileSize.Wide),
        SpiderTileSlot("vpn", SpiderTileSize.Tall),
        SpiderTileSlot("dns", SpiderTileSize.Small),
        SpiderTileSlot("vpeen", SpiderTileSize.Small),
        SpiderTileSlot("routing", SpiderTileSize.Small),
        SpiderTileSlot("tor", SpiderTileSize.Wide),
        SpiderTileSlot("speed-test", SpiderTileSize.Small),
        SpiderTileSlot("monitor", SpiderTileSize.Small),
        SpiderTileSlot("per-app", SpiderTileSize.Wide),
        SpiderTileSlot("core", SpiderTileSize.Small),
        SpiderTileSlot("mode", SpiderTileSize.Small),
        SpiderTileSlot("sing-box", SpiderTileSize.Small),
        SpiderTileSlot("ssh", SpiderTileSize.Small),
        SpiderTileSlot("map", SpiderTileSize.Wide),
        SpiderTileSlot("vault", SpiderTileSize.Wide),
        SpiderTileSlot("mux", SpiderTileSize.Small),
        SpiderTileSlot("fragment", SpiderTileSize.Small),
        SpiderTileSlot("observatory", SpiderTileSize.Small),
        SpiderTileSlot("advanced", SpiderTileSize.Small),
        SpiderTileSlot("assets", SpiderTileSize.Wide),
        SpiderTileSlot("support", SpiderTileSize.Small),
        SpiderTileSlot("ai", SpiderTileSize.Small),
        SpiderTileSlot("tour", SpiderTileSize.Small),
        SpiderTileSlot("setup", SpiderTileSize.Small),
        SpiderTileSlot("rate", SpiderTileSize.Small),
        SpiderTileSlot("firefox-vpn", SpiderTileSize.Small),
        SpiderTileSlot("about", SpiderTileSize.Wide),
    )

    private val PROMINENT = setOf("appearance")

    private val TONES: Map<String, SpiderToneKind> = mapOf(
        "appearance" to SpiderToneKind.Stage,
        "vpn" to SpiderToneKind.Lime,
        "dns" to SpiderToneKind.Cyan,
        "routing" to SpiderToneKind.Pink,
        "tor" to SpiderToneKind.Violet,
        "speed-test" to SpiderToneKind.Blue,
        "monitor" to SpiderToneKind.Coral,
        "per-app" to SpiderToneKind.Surface,
        "core" to SpiderToneKind.Violet,
        "mode" to SpiderToneKind.Cyan,
        "sing-box" to SpiderToneKind.Blue,
        "ssh" to SpiderToneKind.Coral,
        "map" to SpiderToneKind.Cyan,
        "vpeen" to SpiderToneKind.Lime,
        "vault" to SpiderToneKind.Violet,
        "mux" to SpiderToneKind.Pink,
        "fragment" to SpiderToneKind.Violet,
        "observatory" to SpiderToneKind.Blue,
        "advanced" to SpiderToneKind.Surface,
        "assets" to SpiderToneKind.Pink,
        "support" to SpiderToneKind.Coral,
        "ai" to SpiderToneKind.Violet,
        "tour" to SpiderToneKind.Cyan,
        "setup" to SpiderToneKind.Lime,
        "rate" to SpiderToneKind.Lime,
        "firefox-vpn" to SpiderToneKind.Blue,
        "about" to SpiderToneKind.Stage,
    )

    fun isProminent(key: String): Boolean = key in PROMINENT

    fun toneOf(key: String): SpiderToneKind = TONES[key] ?: SpiderToneKind.Surface

    fun defaultSize(key: String): SpiderTileSize = DEFAULT.firstOrNull { it.key == key }?.size ?: SpiderTileSize.Small

    fun ordinalOf(key: String): Int = DEFAULT.indexOfFirst { it.key == key }.let { if (it < 0) DEFAULT.size + 1 else it + 1 }

    fun decode(raw: String, available: Collection<String>): SpiderTileLayout {
        val allowed = available.toSet()
        val text = raw.trim()
        if (text.isEmpty()) return SpiderTileLayout(DEFAULT.filter { it.key in allowed }, emptyList())
        val visiblePart = text.substringBefore('|')
        val hiddenPart = if ('|' in text) text.substringAfter('|') else ""
        val seen = HashSet<String>()
        val visible = visiblePart.split(',').mapNotNull { item ->
            val key = item.substringBefore(':').trim()
            if (key.isEmpty() || key !in allowed || !seen.add(key)) return@mapNotNull null
            val size = item.substringAfter(':', "").trim().firstOrNull()?.let(SpiderTileSize::of) ?: defaultSize(key)
            SpiderTileSlot(key, size)
        }
        val hidden = hiddenPart.split(',').map { it.trim() }.filter { it.isNotEmpty() && it in allowed && seen.add(it) }
        val missing = DEFAULT.map { it.key }.filter { it in allowed && it !in seen } +
            available.filter { it !in seen && DEFAULT.none { slot -> slot.key == it } }
        return SpiderTileLayout(visible + missing.distinct().map { SpiderTileSlot(it, defaultSize(it)) }, hidden)
    }

    fun encode(layout: SpiderTileLayout): String =
        layout.visible.joinToString(",") { "${it.key}:${it.size.code}" } + "|" + layout.hidden.joinToString(",")

    fun pack(slots: List<SpiderTileSlot>, columns: Int): List<SpiderPlaced> {
        val width = columns.coerceAtLeast(1)
        val taken = ArrayList<BooleanArray>()
        fun free(row: Int, col: Int): Boolean = row >= taken.size || !taken[row][col]
        fun take(row: Int, col: Int) {
            while (taken.size <= row) taken += BooleanArray(width)
            taken[row][col] = true
        }
        val placed = ArrayList<SpiderPlaced>(slots.size)
        for (slot in slots) {
            val w = slot.size.cols.coerceAtMost(width)
            val h = slot.size.rows
            var row = 0
            var done = false
            while (!done) {
                for (col in 0..width - w) {
                    val fits = (row until row + h).all { r -> (col until col + w).all { c -> free(r, c) } }
                    if (fits) {
                        for (r in row until row + h) for (c in col until col + w) take(r, c)
                        placed += SpiderPlaced(slot, col, row, w, h)
                        done = true
                        break
                    }
                }
                row++
            }
        }
        return placed
    }

    fun rowsOf(placed: List<SpiderPlaced>): Int = placed.maxOfOrNull { it.row + it.rows } ?: 0

    fun dropIndex(layout: SpiderTileLayout, key: String, columns: Int, gridX: Float, gridY: Float, stickiness: Float = 0.2f): Int {
        val current = layout.visible.indexOfFirst { it.key == key }
        if (current < 0) return -1
        fun distanceAt(index: Int): Float {
            val me = pack(layout.moved(key, index).visible, columns).first { it.slot.key == key }
            val dx = me.col + me.cols / 2f - gridX
            val dy = me.row + me.rows / 2f - gridY
            return dx * dx + dy * dy
        }
        val stay = distanceAt(current)
        var best = current
        var bestDistance = stay
        for (index in layout.visible.indices) {
            if (index == current) continue
            val d = distanceAt(index)
            if (d < bestDistance) {
                best = index
                bestDistance = d
            }
        }
        return if (best != current && bestDistance < stay - stickiness) best else current
    }
}
