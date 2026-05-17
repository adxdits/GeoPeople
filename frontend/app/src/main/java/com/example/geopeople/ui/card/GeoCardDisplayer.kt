package com.example.geopeople.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geopeople.model.GeoCard

private val CardBackground   = Color(0xFF1A1C2C)
private val CardSurface      = Color(0xFF252840)
private val AccentTeal       = Color(0xFF4ECDC4)
private val AccentAmber      = Color(0xFFFFBE0B)
private val TextPrimary      = Color(0xFFF0F0F5)
private val TextSecondary    = Color(0xFF9A9BB5)
private val PowerLow         = Color(0xFF4ECDC4)
private val PowerMid         = Color(0xFFFFBE0B)
private val PowerHigh        = Color(0xFFFF6B6B)

private fun powerColor(power: Int) = when {
    power <= 3  -> PowerLow
    power <= 7  -> PowerMid
    else        -> PowerHigh
}


@Composable
private fun CoordinateChip(label: String, value: Double) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
        )
        Text(
                text = "%.4f".format(value),
                color = AccentTeal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PowerBadge(power: Int, modifier: Modifier = Modifier) {
    val color = powerColor(power)
    Box(
            modifier = modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, color, CircleShape)
                    .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = "$power",
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
        )
    }
}


@Composable
fun GeoCardDisplayer(card: GeoCard, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight().background(CardBackground),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            Box(
                    modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .background(
                                    Brush.verticalGradient(listOf(AccentTeal, AccentAmber))
                            )
            )

            Column(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = card.name,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        Text(
                                text = "#${card.id}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    PowerBadge(power = card.power)
                }

                Text(
                        text = card.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                )

                HorizontalDivider(
                        color = Color.White.copy(alpha = 0.07f),
                        thickness = 1.dp
                )

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(14.dp)
                    )
                    CoordinateChip(label = "LAT", value = card.latitude)
                    CoordinateChip(label = "LNG", value = card.longitude)
                }
            }
        }
    }
}