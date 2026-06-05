package com.example.geopeople.ui.navigation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.geopeople.model.GeoCard
import com.example.geopeople.ui.inventory.CardDetailScreen
import com.example.geopeople.ui.inventory.InventoryScreen
import com.example.geopeople.ui.map.GameScreen
import com.example.geopeople.viewmodel.GameViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun AppNavigation(viewModel: GameViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == "main"
    var selectedTab by remember { mutableStateOf(MainTab.Map) }
    var selectedDetailCard by remember { mutableStateOf<GeoCard?>(null) }
    val mapView = remember {
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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomTextNavigation(
                    selectedTab = selectedTab,
                    onMapClick = { selectedTab = MainTab.Map },
                    onInventoryClick = { selectedTab = MainTab.Inventory }
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
                    onCardClick = { card ->
                        selectedDetailCard = card
                        navController.navigate("cardDetail")
                    }
                )
            }
            composable("cardDetail") {
                val card = selectedDetailCard
                if (card == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    CardDetailScreen(
                        card = card,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private enum class MainTab {
    Map,
    Inventory
}

@Composable
private fun MainTabsScreen(
    viewModel: GameViewModel,
    mapView: MapView,
    selectedTab: MainTab,
    onCardClick: (GeoCard) -> Unit
) {
    val inventory by viewModel.inventory.collectAsState()

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
                    onCardClick = onCardClick
                )
            }
        }
    }
}

@Composable
private fun BottomTextNavigation(
    selectedTab: MainTab,
    onMapClick: () -> Unit,
    onInventoryClick: () -> Unit
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
                label = "Carte",
                selected = selectedTab == MainTab.Map,
                onClick = onMapClick,
                modifier = Modifier.weight(1f)
            )
            NavigationTab(
                label = "Inventaire",
                selected = selectedTab == MainTab.Inventory,
                onClick = onInventoryClick,
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
                text = "Pars a la capture autour de toi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Explore la carte, repere les personnages proches et lance une partie.",
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
                    text = "START",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.8.sp
                )
            }
        }
    }
}
