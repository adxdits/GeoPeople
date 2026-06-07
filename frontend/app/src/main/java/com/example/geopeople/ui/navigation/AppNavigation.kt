package com.example.geopeople.ui.navigation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.geopeople.R
import com.example.geopeople.model.GeoCard
import com.example.geopeople.ui.inventory.CardDetailScreen
import com.example.geopeople.ui.inventory.InventoryScreen
import com.example.geopeople.ui.leaderboard.LeaderboardScreen
import com.example.geopeople.ui.map.GameScreen
import com.example.geopeople.viewmodel.GameViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun AppNavigation(viewModel: GameViewModel) {
    val context = LocalContext.current
    val needsPlayerName by viewModel.needsPlayerName.collectAsState()
    val serverConnectionMessage by viewModel.serverConnectionMessage.collectAsState()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var selectedTab by remember { mutableStateOf(MainTab.Map) }
    var selectedDetailCard by remember { mutableStateOf<GeoCard?>(null) }
    val showBottomBar = currentRoute == "main" && selectedDetailCard == null
    var mapViewGeneration by remember { mutableStateOf(0) }
    val mapView = remember(mapViewGeneration) {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(37.4219983, -122.084))
        }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onDetach()
        }
    }

    if (needsPlayerName) {
        PlayerNameScreen(
            onSubmit = { name -> viewModel.createNewPlayer(name) }
        )
        serverConnectionMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissServerConnectionMessage() },
                title = { Text(stringResource(R.string.server_unavailable)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissServerConnectionMessage() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            )
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomTextNavigation(
                    selectedTab = selectedTab,
                    onMapClick = { selectedTab = MainTab.Map },
                    onInventoryClick = { selectedTab = MainTab.Inventory },
                    onProfileClick = {
                        selectedDetailCard = null
                        viewModel.selectCard(null)
                        viewModel.refreshLeaderboard()
                        viewModel.refreshTrades()
                        viewModel.refreshBattles()
                        selectedTab = MainTab.Profile
                    }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "start", Modifier.padding(padding)) {
            composable("start") {
                StartScreen(
                    onStartClick = {
                        navController.navigate("main") {
                            popUpTo("start") { inclusive = true }
                        }
                    }
                )
            }
            composable("main") {
                MainTabsScreen(
                    viewModel = viewModel,
                    mapView = mapView,
                    selectedTab = selectedTab,
                    onLeaderboardClick = {
                        selectedTab = MainTab.Leaderboard
                        viewModel.refreshLeaderboard()
                    },
                    onLeaderboardBack = { selectedTab = MainTab.Inventory },
                    onLogout = {
                        selectedTab = MainTab.Map
                        selectedDetailCard = null
                        mapViewGeneration += 1
                        viewModel.logoutPlayer()
                    },
                    onCardClick = { card ->
                        selectedDetailCard = card
                    }
                )

                selectedDetailCard?.let { card ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        CardDetailScreen(
                            card = card,
                            onBack = { selectedDetailCard = null }
                        )
                    }
                }
            }
        }
    }
}

private enum class MainTab {
    Map,
    Inventory,
    Leaderboard,
    Profile
}

@Composable
private fun MainTabsScreen(
    viewModel: GameViewModel,
    mapView: MapView,
    selectedTab: MainTab,
    onLeaderboardClick: () -> Unit,
    onLeaderboardBack: () -> Unit,
    onLogout: () -> Unit,
    onCardClick: (GeoCard) -> Unit
) {
    val inventory by viewModel.inventory.collectAsState()
    val currentPlayerId by viewModel.currentPlayerId.collectAsState()
    val playerName by viewModel.playerName.collectAsState()
    val playerScore by viewModel.playerScore.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val tradeTargetInventory by viewModel.tradeTargetInventory.collectAsState()
    val tradeMessage by viewModel.tradeMessage.collectAsState()
    val trades by viewModel.trades.collectAsState()
    val battles by viewModel.battles.collectAsState()
    val battleMessage by viewModel.battleMessage.collectAsState()

    LaunchedEffect(selectedTab) {
        while (selectedTab == MainTab.Profile) {
            viewModel.refreshLeaderboard()
            viewModel.refreshTrades()
            kotlinx.coroutines.delay(5_000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameScreen(
            viewModel = viewModel,
            mapView = mapView
        )

        if (selectedTab == MainTab.Inventory) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                InventoryScreen(
                    inventory = inventory,
                    onLeaderboardClick = onLeaderboardClick,
                    onCardClick = onCardClick
                )
            }
        }

        if (selectedTab == MainTab.Leaderboard) {
            LeaderboardScreen(
                currentPlayerId = currentPlayerId,
                currentPlayerName = playerName,
                currentPlayerScore = playerScore,
                players = leaderboard,
                onRefresh = { viewModel.refreshLeaderboard() },
                onBack = onLeaderboardBack
            )
        }

        if (selectedTab == MainTab.Profile) {
            ProfileScreen(
                currentPlayerId = currentPlayerId,
                playerName = playerName,
                playerScore = playerScore,
                inventory = inventory,
                players = leaderboard,
                targetInventory = tradeTargetInventory,
                trades = trades,
                battles = battles,
                onLoadTargetInventory = { viewModel.loadTradeTargetInventory(it) },
                onClearTargetInventory = { viewModel.clearTradeTargetInventory() },
                onCreateTrade = { targetPlayerId, myCardId, targetCardId ->
                    viewModel.createTradeProposal(targetPlayerId, myCardId, targetCardId)
                },
                onRefreshTrades = { viewModel.refreshTrades() },
                onAcceptTrade = { viewModel.acceptTrade(it) },
                onRejectTrade = { viewModel.rejectTrade(it) },
                onCreateBattle = { targetPlayerId, myCardId ->
                    viewModel.createBattleProposal(targetPlayerId, myCardId)
                },
                onRefreshBattles = { viewModel.refreshBattles() },
                onAcceptBattle = { battleId, cardId -> viewModel.acceptBattle(battleId, cardId) },
                onRejectBattle = { viewModel.rejectBattle(it) },
                onLogout = onLogout
            )
        }

        tradeMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissTradeMessage() },
                title = { Text(stringResource(R.string.trade_title)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissTradeMessage() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            )
        }

        battleMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissBattleMessage() },
                title = { Text(stringResource(R.string.battle_title)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissBattleMessage() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            )
        }
    }
}

@Composable
private fun BottomTextNavigation(
    selectedTab: MainTab,
    onMapClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = Color(0xFFF3F4F8),
        tonalElevation = 10.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavigationTab(
                label = stringResource(R.string.nav_map),
                selected = selectedTab == MainTab.Map,
                onClick = onMapClick,
                modifier = Modifier.weight(1f)
            )
            NavigationTab(
                label = stringResource(R.string.nav_inventory),
                selected = selectedTab == MainTab.Inventory,
                onClick = onInventoryClick,
                modifier = Modifier.weight(1f)
            )
            NavigationTab(
                label = stringResource(R.string.nav_profile),
                selected = selectedTab == MainTab.Profile,
                onClick = onProfileClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavigationTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFDCE7FF) else Color.White,
            contentColor = if (selected) Color(0xFF23406B) else Color(0xFF606A78)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFF97B4E8) else Color(0xFFD7DCE5)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 2.dp else 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ProfileScreen(
    currentPlayerId: String?,
    playerName: String,
    playerScore: Int,
    inventory: List<GeoCard>,
    players: List<com.example.geopeople.data.LeaderboardPlayerResponse>,
    targetInventory: List<GeoCard>,
    trades: List<com.example.geopeople.data.TradeProposalResponse>,
    battles: List<com.example.geopeople.data.BattleProposalResponse>,
    onLoadTargetInventory: (String) -> Unit,
    onClearTargetInventory: () -> Unit,
    onCreateTrade: (targetPlayerId: String, myCardId: String, targetCardId: String) -> Unit,
    onRefreshTrades: () -> Unit,
    onAcceptTrade: (String) -> Unit,
    onRejectTrade: (String) -> Unit,
    onCreateBattle: (targetPlayerId: String, myCardId: String) -> Unit,
    onRefreshBattles: () -> Unit,
    onAcceptBattle: (battleId: String, cardId: String) -> Unit,
    onRejectBattle: (String) -> Unit,
    onLogout: () -> Unit
) {
    var showTradeDialog by remember { mutableStateOf(false) }
    var showBattleDialog by remember { mutableStateOf(false) }
    var battleToAccept by remember { mutableStateOf<com.example.geopeople.data.BattleProposalResponse?>(null) }
    val tradePlayers = players.filter { it.id != currentPlayerId && it.cardCount > 0 }
    val pendingTrades = trades.filter { it.status == "pending" }
    val pendingBattles = battles.filter { it.status == "pending" }
    val resolvedBattles = battles.filter { it.status == "accepted" }
    val battleWins = resolvedBattles.count { it.winnerId == currentPlayerId }
    val battleLosses = resolvedBattles.count { it.loserId == currentPlayerId }
    val acceptedTrades = trades.filter { it.status == "accepted" }
    val incomingPendingTrades = pendingTrades.count { it.toPlayerId == currentPlayerId }
    val outgoingPendingTrades = pendingTrades.count { it.fromPlayerId == currentPlayerId }
    val totalPower = inventory.sumOf { it.power }
    val strongestCard = inventory.maxByOrNull { it.power }
    val latestCapturedCard = inventory
        .filter { it.capturedAt != null }
        .maxByOrNull { it.capturedAt.orEmpty() }
        ?: inventory.lastOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1620))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileTile {
                Text(
                    text = stringResource(R.string.nav_profile),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCB05)
                )
                Text(
                    text = stringResource(R.string.leaderboard_player_points, playerScore),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFB7C2CD)
                )
                Text(
                    text = stringResource(R.string.profile_card_count, inventory.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB7C2CD)
                )
            }

            ProfileTile {
                Text(
                    text = stringResource(R.string.profile_personal_stats),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(
                        label = stringResource(R.string.profile_stat_cards),
                        value = inventory.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = stringResource(R.string.profile_stat_score),
                        value = playerScore.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(
                        label = stringResource(R.string.profile_stat_total_power),
                        value = totalPower.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = stringResource(R.string.profile_stat_trades),
                        value = acceptedTrades.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(
                        label = stringResource(R.string.profile_stat_battles),
                        value = resolvedBattles.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = stringResource(R.string.profile_stat_wins),
                        value = battleWins.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = stringResource(R.string.profile_stat_losses),
                        value = battleLosses.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                InfoStatLine(
                    label = stringResource(R.string.profile_stat_best_card),
                    value = strongestCard?.let { "${it.name} (${it.power})" }
                        ?: stringResource(R.string.profile_stat_empty)
                )
                InfoStatLine(
                    label = stringResource(R.string.profile_stat_latest_card),
                    value = latestCapturedCard?.name ?: stringResource(R.string.profile_stat_empty)
                )
                InfoStatLine(
                    label = stringResource(R.string.profile_stat_pending_trades),
                    value = stringResource(
                        R.string.profile_stat_pending_trades_value,
                        incomingPendingTrades,
                        outgoingPendingTrades
                    )
                )
            }

            ProfileTile {
                Text(
                    text = stringResource(R.string.profile_actions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = {
                        onClearTargetInventory()
                        showTradeDialog = true
                    },
                    enabled = inventory.isNotEmpty() && tradePlayers.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.profile_propose_trade), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onRefreshTrades,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.profile_refresh_trade_requests))
                }
                Button(
                    onClick = { showBattleDialog = true },
                    enabled = inventory.isNotEmpty() && tradePlayers.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.profile_challenge_player))
                }
                Button(
                    onClick = onRefreshBattles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.profile_refresh_battles))
                }
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFCB05),
                        contentColor = Color(0xFF17212B)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.profile_change_player),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ProfileTile {
                Text(
                    text = stringResource(R.string.trade_requests_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (pendingTrades.isEmpty()) {
                    Text(
                        text = stringResource(R.string.trade_requests_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB7C2CD)
                    )
                } else {
                    pendingTrades.forEach { trade ->
                        TradeRequestRow(
                            trade = trade,
                            currentPlayerId = currentPlayerId,
                            players = players,
                            inventory = inventory,
                            onAcceptTrade = onAcceptTrade,
                            onRejectTrade = onRejectTrade
                        )
                    }
                }
            }

            ProfileTile {
                Text(
                    text = stringResource(R.string.battle_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (pendingBattles.isEmpty() && resolvedBattles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.battle_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB7C2CD)
                    )
                }
                pendingBattles.forEach { battle ->
                    BattleRequestRow(
                        battle = battle,
                        currentPlayerId = currentPlayerId,
                        players = players,
                        onAcceptClick = { battleToAccept = battle },
                        onRejectBattle = onRejectBattle
                    )
                }
                resolvedBattles.takeLast(3).forEach { battle ->
                    BattleResultRow(
                        battle = battle,
                        currentPlayerId = currentPlayerId,
                        players = players
                    )
                }
            }
        }
    }

    if (showTradeDialog) {
        TradeDialog(
            currentInventory = inventory,
            players = tradePlayers,
            targetInventory = targetInventory,
            onLoadTargetInventory = onLoadTargetInventory,
            onExchange = { targetPlayerId, myCardId, targetCardId ->
                showTradeDialog = false
                onCreateTrade(targetPlayerId, myCardId, targetCardId)
            },
            onDismiss = {
                showTradeDialog = false
                onClearTargetInventory()
            }
        )
    }

    if (showBattleDialog) {
        BattleDialog(
            currentInventory = inventory,
            players = tradePlayers,
            onCreateBattle = { targetPlayerId, myCardId ->
                showBattleDialog = false
                onCreateBattle(targetPlayerId, myCardId)
            },
            onDismiss = { showBattleDialog = false }
        )
    }

    battleToAccept?.let { battle ->
        AcceptBattleDialog(
            inventory = inventory,
            onAccept = { cardId ->
                battleToAccept = null
                onAcceptBattle(battle.id, cardId)
            },
            onDismiss = { battleToAccept = null }
        )
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF202B38),
        border = BorderStroke(1.dp, Color(0xFF334253))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFCB05)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB7C2CD)
            )
        }
    }
}

@Composable
private fun InfoStatLine(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF202B38),
        border = BorderStroke(1.dp, Color(0xFF334253))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB7C2CD),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfileTile(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF17212B),
        border = BorderStroke(1.dp, Color(0xFF334253))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun TradeRequestRow(
    trade: com.example.geopeople.data.TradeProposalResponse,
    currentPlayerId: String?,
    players: List<com.example.geopeople.data.LeaderboardPlayerResponse>,
    inventory: List<GeoCard>,
    onAcceptTrade: (String) -> Unit,
    onRejectTrade: (String) -> Unit
) {
    val incoming = trade.toPlayerId == currentPlayerId
    val otherPlayerId = if (incoming) trade.fromPlayerId else trade.toPlayerId
    val otherName = players.firstOrNull { it.id == otherPlayerId }?.name ?: stringResource(R.string.generic_player)
    val myCardId = if (incoming) trade.toCardId else trade.fromCardId
    val otherCardId = if (incoming) trade.fromCardId else trade.toCardId
    val myCard = inventory.firstOrNull { it.id == myCardId }?.name ?: stringResource(R.string.generic_card)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF202B38),
        border = BorderStroke(1.dp, Color(0xFF334253))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (incoming) stringResource(R.string.trade_from_player, otherName) else stringResource(R.string.trade_sent_to_player, otherName),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.trade_my_card, myCard),
                color = Color(0xFFB7C2CD),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.trade_proposed_card, otherCardId),
                color = Color(0xFFB7C2CD),
                style = MaterialTheme.typography.bodySmall
            )
            if (incoming) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAcceptTrade(trade.id) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.trade_accept))
                    }
                    OutlinedButton(onClick = { onRejectTrade(trade.id) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.trade_reject))
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleRequestRow(
    battle: com.example.geopeople.data.BattleProposalResponse,
    currentPlayerId: String?,
    players: List<com.example.geopeople.data.LeaderboardPlayerResponse>,
    onAcceptClick: () -> Unit,
    onRejectBattle: (String) -> Unit
) {
    val incoming = battle.toPlayerId == currentPlayerId
    val otherPlayerId = if (incoming) battle.fromPlayerId else battle.toPlayerId
    val otherName = players.firstOrNull { it.id == otherPlayerId }?.name ?: stringResource(R.string.generic_player)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF202B38),
        border = BorderStroke(1.dp, Color(0xFF334253))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (incoming) stringResource(R.string.battle_from_player, otherName) else stringResource(R.string.battle_sent_to_player, otherName),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (incoming) stringResource(R.string.battle_opponent_hidden_card) else stringResource(R.string.battle_own_hidden_card),
                color = Color(0xFFB7C2CD),
                style = MaterialTheme.typography.bodySmall
            )
            if (incoming) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAcceptClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.trade_accept))
                    }
                    OutlinedButton(onClick = { onRejectBattle(battle.id) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.trade_reject))
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleResultRow(
    battle: com.example.geopeople.data.BattleProposalResponse,
    currentPlayerId: String?,
    players: List<com.example.geopeople.data.LeaderboardPlayerResponse>
) {
    val won = battle.winnerId == currentPlayerId
    val opponentId = if (battle.fromPlayerId == currentPlayerId) battle.toPlayerId else battle.fromPlayerId
    val opponentName = players.firstOrNull { it.id == opponentId }?.name ?: stringResource(R.string.generic_player)
    val myCardName = if (battle.fromPlayerId == currentPlayerId) battle.playerACardName else battle.playerBCardName
    val myCardPower = if (battle.fromPlayerId == currentPlayerId) battle.playerACardPower else battle.playerBCardPower
    val opponentCardName = if (battle.fromPlayerId == currentPlayerId) battle.playerBCardName else battle.playerACardName
    val opponentCardPower = if (battle.fromPlayerId == currentPlayerId) battle.playerBCardPower else battle.playerACardPower
    val winnerCard = battle.winnerCardName?.let { name ->
        battle.winnerCardPower?.let { power -> "$name ($power)" } ?: name
    } ?: stringResource(R.string.battle_winning_card)
    val loserCard = battle.loserCardName?.let { name ->
        battle.loserCardPower?.let { power -> "$name ($power)" } ?: name
    } ?: stringResource(R.string.battle_losing_card)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (won) Color(0xFF263B2D) else Color(0xFF3A2730),
        border = BorderStroke(1.dp, Color(0xFF334253))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (won) stringResource(R.string.battle_victory_against, opponentName) else stringResource(R.string.battle_defeat_against, opponentName),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = battle.message ?: stringResource(R.string.battle_finished),
                color = Color(0xFFB7C2CD),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.battle_my_card_power, myCardName ?: stringResource(R.string.generic_card), myCardPower?.toString() ?: "?"),
                color = Color(0xFFB7C2CD),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.battle_opponent_card_power, opponentCardName ?: stringResource(R.string.generic_card), opponentCardPower?.toString() ?: "?"),
                color = Color(0xFFB7C2CD),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.battle_result_cards, winnerCard, loserCard),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (won) stringResource(R.string.battle_gain_card) else stringResource(R.string.battle_lose_card),
                color = Color(0xFFFFCB05),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BattleDialog(
    currentInventory: List<GeoCard>,
    players: List<com.example.geopeople.data.LeaderboardPlayerResponse>,
    onCreateBattle: (targetPlayerId: String, myCardId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<String?>(null) }
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    val canSend = selectedPlayerId != null && selectedCardId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battle_challenge_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.trade_section_player))
                players.forEach { player ->
                    TradeChoiceButton(
                        text = stringResource(R.string.trade_player_card_count, player.name, player.cardCount),
                        selected = selectedPlayerId == player.id,
                        onClick = { selectedPlayerId = player.id }
                    )
                }
                Text(stringResource(R.string.battle_hidden_card))
                currentInventory.take(6).forEach { card ->
                    TradeChoiceButton(
                        text = stringResource(R.string.trade_card_choice, card.name, card.power),
                        selected = selectedCardId == card.id,
                        onClick = { selectedCardId = card.id }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSend,
                onClick = { onCreateBattle(selectedPlayerId.orEmpty(), selectedCardId.orEmpty()) }
            ) {
                Text(stringResource(R.string.battle_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.trade_cancel))
            }
        }
    )
}

@Composable
private fun AcceptBattleDialog(
    inventory: List<GeoCard>,
    onAccept: (cardId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCardId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battle_choose_card_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.battle_choose_card_help))
                inventory.take(6).forEach { card ->
                    TradeChoiceButton(
                        text = stringResource(R.string.trade_card_choice, card.name, card.power),
                        selected = selectedCardId == card.id,
                        onClick = { selectedCardId = card.id }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCardId != null,
                onClick = { onAccept(selectedCardId.orEmpty()) }
            ) {
                Text(stringResource(R.string.battle_fight))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.trade_cancel))
            }
        }
    )
}

@Composable
private fun TradeDialog(
    currentInventory: List<GeoCard>,
    players: List<com.example.geopeople.data.LeaderboardPlayerResponse>,
    targetInventory: List<GeoCard>,
    onLoadTargetInventory: (String) -> Unit,
    onExchange: (targetPlayerId: String, myCardId: String, targetCardId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<String?>(null) }
    var selectedMyCardId by remember { mutableStateOf<String?>(null) }
    var selectedTargetCardId by remember { mutableStateOf<String?>(null) }
    val selectedPlayer = players.firstOrNull { it.id == selectedPlayerId }
    val canExchange = selectedPlayerId != null && selectedMyCardId != null && selectedTargetCardId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trade_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.trade_section_player))
                players.forEach { player ->
                    Button(
                        onClick = {
                            selectedPlayerId = player.id
                            selectedTargetCardId = null
                            onLoadTargetInventory(player.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedPlayerId == player.id) Color(0xFFFFCB05) else Color(0xFF253342),
                            contentColor = if (selectedPlayerId == player.id) Color(0xFF17212B) else Color.White
                        )
                    ) {
                        Text(stringResource(R.string.trade_player_card_count, player.name, player.cardCount))
                    }
                }

                Text(stringResource(R.string.trade_section_my_card))
                currentInventory.take(5).forEach { card ->
                    TradeChoiceButton(
                        text = stringResource(R.string.trade_card_choice, card.name, card.power),
                        selected = selectedMyCardId == card.id,
                        onClick = { selectedMyCardId = card.id }
                    )
                }

                Text(selectedPlayer?.let { stringResource(R.string.trade_section_player_card, it.name) } ?: stringResource(R.string.trade_section_other_card))
                if (selectedPlayerId == null) {
                    Text(stringResource(R.string.trade_select_player_first))
                } else if (targetInventory.isEmpty()) {
                    Text(stringResource(R.string.trade_no_card_available))
                } else {
                    targetInventory.take(5).forEach { card ->
                        TradeChoiceButton(
                            text = stringResource(R.string.trade_card_choice, card.name, card.power),
                            selected = selectedTargetCardId == card.id,
                            onClick = { selectedTargetCardId = card.id }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canExchange,
                onClick = {
                    onExchange(
                        selectedPlayerId.orEmpty(),
                        selectedMyCardId.orEmpty(),
                        selectedTargetCardId.orEmpty()
                    )
                }
            ) {
                Text(stringResource(R.string.trade_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.trade_cancel))
            }
        }
    )
}

@Composable
private fun TradeChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFFFCB05) else Color(0xFF253342),
            contentColor = if (selected) Color(0xFF17212B) else Color.White
        )
    ) {
        Text(text)
    }
}

@Composable
private fun PlayerNameScreen(onSubmit: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val canSubmit = name.trim().isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1620))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB7C2CD)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.player_name_label)) }
            )
            Button(
                onClick = { onSubmit(name) },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFCB05),
                    contentColor = Color(0xFF17212B)
                )
            ) {
                Text(
                    text = stringResource(R.string.continue_button),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StartScreen(onStartClick: () -> Unit) {
    val context = LocalContext.current
    val background = remember {
        runCatching {
            context.assets.open("backgroundStartimage.jpg").use(BitmapFactory::decodeStream)?.asImageBitmap()
        }.getOrNull()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (background != null) {
            Image(
                bitmap = background,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF78C8F9))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x700B2034))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.start_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.start_screen_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(
                        width = 3.dp,
                        color = Color(0xFF2A75BB),
                        shape = RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, Color(0xFFFFF1A8)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFCB05),
                    contentColor = Color(0xFF1B1F24)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.start_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.8.sp
                )
            }
        }
    }
}

