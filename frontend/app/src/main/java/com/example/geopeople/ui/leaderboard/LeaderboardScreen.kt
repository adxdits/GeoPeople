package com.example.geopeople.ui.leaderboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.geopeople.R
import com.example.geopeople.data.LeaderboardPlayerResponse

private val Background = Color(0xFF0E1620)
private val Panel = Color(0xFF17212B)
private val PanelLight = Color(0xFF202B38)
private val Border = Color(0xFF334253)
private val TextMain = Color(0xFFF2F5F8)
private val TextMuted = Color(0xFFB7C2CD)
private val Accent = Color(0xFFFFCB05)

@Composable
fun LeaderboardScreen(
    currentPlayerId: String?,
    currentPlayerName: String,
    currentPlayerScore: Int,
    players: List<LeaderboardPlayerResponse>,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Header(
                    currentPlayerName = currentPlayerName,
                    currentPlayerScore = currentPlayerScore,
                    onRefresh = onRefresh,
                    onBack = onBack
                )
            }

            if (players.isEmpty()) {
                item {
                    EmptyLeaderboard()
                }
            } else {
                itemsIndexed(players) { index, player ->
                    PlayerRankRow(
                        rank = index + 1,
                        player = player,
                        isCurrentPlayer = player.id == currentPlayerId
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    currentPlayerName: String,
    currentPlayerScore: Int,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Panel,
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.leaderboard_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Text(
                        text = currentPlayerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currentPlayerScore",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Accent
                    )
                    Text(
                        text = stringResource(R.string.leaderboard_your_points),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) {
                    Text(stringResource(R.string.action_back))
                }
                OutlinedButton(onClick = onRefresh) {
                    Text(stringResource(R.string.action_refresh))
                }
            }
        }
    }
}

@Composable
private fun EmptyLeaderboard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Panel,
        border = BorderStroke(1.dp, Border)
    ) {
        Text(
            text = stringResource(R.string.leaderboard_empty),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

@Composable
private fun PlayerRankRow(
    rank: Int,
    player: LeaderboardPlayerResponse,
    isCurrentPlayer: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isCurrentPlayer) Color(0xFF253342) else PanelLight,
        border = BorderStroke(1.dp, if (isCurrentPlayer) Color(0xFF5E7EA5) else Border)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (rank == 1) Accent else TextMuted
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.leaderboard_player_cards, player.cardCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
            Text(
                text = stringResource(R.string.leaderboard_player_points, player.score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Accent
            )
        }
    }
}
