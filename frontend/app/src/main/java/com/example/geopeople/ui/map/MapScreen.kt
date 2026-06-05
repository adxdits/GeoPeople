package com.example.geopeople.ui.map

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.geopeople.model.GeoCard
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    mapView: MapView,
    playerLocation: Location?,
    visibleCards: List<GeoCard>,
    capturedIds: Set<String>,
    onCardClick: (GeoCard) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            playerLocation?.let { loc ->
                val point = GeoPoint(loc.latitude, loc.longitude)
                val marker = Marker(map).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Vous êtes ici"
                    icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                }
                map.overlays.add(marker)
                map.controller.animateTo(point)
            }

            visibleCards.forEach { card ->
                val isCaptured = capturedIds.contains(card.id)
                val marker = Marker(map).apply {
                    position = GeoPoint(card.latitude, card.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = card.name
                    snippet = if (isCaptured) "Déjà capturée" else "Puissance: ${card.power}"
                    icon = markerIconCopy(icon, isCaptured)
                    setOnMarkerClickListener { _, _ ->
                        onCardClick(card)
                        true
                    }
                }
                map.overlays.add(marker)
            }

            map.invalidate()
        }
    )
}

private fun markerIconCopy(source: Drawable?, captured: Boolean): Drawable? {
    val copy = source?.constantState?.newDrawable()?.mutate() ?: source?.mutate()
    if (captured) {
        copy?.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0f)
        })
        copy?.alpha = 170
    } else {
        copy?.clearColorFilter()
        copy?.alpha = 255
    }
    return copy
}
