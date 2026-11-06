package dev.spider.spider.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

object TourTargets {
    private val bounds = mutableStateMapOf<String, Rect>()

    private val owners = mutableMapOf<String, Any>()

    private val scrollers = mutableStateMapOf<String, BringIntoViewRequester>()

    internal fun put(key: String, owner: Any, rect: Rect) {
        owners[key] = owner

        if (bounds[key] != rect) bounds[key] = rect
    }

    internal fun forget(key: String, owner: Any) {
        if (owners[key] === owner) {
            owners.remove(key)
            bounds.remove(key)
        }
    }

    operator fun get(key: String): Rect? = bounds[key]

    internal fun putScroller(key: String, requester: BringIntoViewRequester) {
        scrollers[key] = requester
    }

    internal fun removeScroller(key: String, requester: BringIntoViewRequester) {
        if (scrollers[key] === requester) scrollers.remove(key)
    }

    fun nav(destination: String): String = "nav-$destination"

    const val CORE = "core"
    const val TRAFFIC = "traffic"
    const val CONFIG_CARD = "config-card"
    const val HOME_TOOLS = "home-tools"
    const val NAV_SERVERS = "nav-Servers"
    const val NAV_SETTINGS = "nav-Settings"
    const val SERVERS_ADD = "servers-add"
    const val SERVERS_SUBS = "servers-subs"
    const val SERVERS_PING = "servers-ping"
    const val SERVERS_MORE = "servers-more"
    const val SERVERS_GROUPS = "servers-groups"
    const val SERVER_CARD = "server-card"
    const val SETTINGS_SUPPORT = "settings-support"
    const val SETTINGS_LOOK = "settings-look"
    const val SETTINGS_ROUTING = "settings-routing"
    const val SETTINGS_TUNNELS = "settings-tunnels"
    const val SETTINGS_CORE = "settings-core"
    const val SETTINGS_TOOLS = "settings-tools"
    const val SETTINGS_ABOUT = "settings-about"
}

fun Modifier.tourTarget(key: String): Modifier = this then TourTargetElement(key)

private data class TourTargetElement(val key: String) : ModifierNodeElement<TourTargetNode>() {
    override fun create() = TourTargetNode(key)

    override fun update(node: TourTargetNode) {
        node.setKey(key)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "tourTarget"
        properties["key"] = key
    }
}

private class TourTargetNode(private var key: String) :
    Modifier.Node(), GlobalPositionAwareModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        TourTargets.put(key, this, coordinates.boundsInRoot())
    }

    override fun onDetach() {
        TourTargets.forget(key, this)
    }

    fun setKey(next: String) {
        if (next != key) {
            TourTargets.forget(key, this)
            key = next
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tourTargetInPage(key: String): Modifier {
    val requester = remember(key) { BringIntoViewRequester() }
    DisposableEffect(key, requester) {
        TourTargets.putScroller(key, requester)
        onDispose { TourTargets.removeScroller(key, requester) }
    }
    return this.bringIntoViewRequester(requester).tourTarget(key)
}