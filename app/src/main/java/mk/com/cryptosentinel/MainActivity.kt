package mk.com.cryptosentinel

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mk.com.cryptosentinel.GlobalState.currencies
import mk.com.cryptosentinel.api.model.response.Currencies
import mk.com.cryptosentinel.api.service.ApiManager
import mk.com.cryptosentinel.ui.screen.CalculatorScreen
import mk.com.cryptosentinel.ui.screen.HistoryScreen
import mk.com.cryptosentinel.ui.screen.ProfileScreen
import mk.com.cryptosentinel.ui.theme.CryptoSentinelTheme

data class PieData(val value: Float, val color: Color, val label: String)

object GlobalState {
    var currencies by mutableStateOf(emptyList<Currencies>())
}
private lateinit var analytics: FirebaseAnalytics

object NotificationEventBus {
    private val _events = MutableSharedFlow<Pair<String?, String?>>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun send(title: String?, message: String?) {
        _events.tryEmit(title to message)
    }
}
@Composable
fun PieChart(
    pieData: List<PieData>,
    animatedSweep: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val total = pieData.sumOf { it.value.toDouble() }.toFloat()

        pieData.forEachIndexed { index, data ->
            val sweepAngle = (data.value / total) * animatedSweep
            drawArc(
                brush = gradientPalette[index % gradientPalette.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += (data.value / total) * 360f
        }
    }
}

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getFcmToken()
        analytics = Firebase.analytics
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM", "Token: $token")
                    // Send to your server if needed
                }
            }

        setContent {
            CryptoSentinelTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1B1C2E)
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    val apiManager = ApiManager()

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        NotificationEventBus.events.collect { (title, message) ->
            dialogTitle = title
            dialogMessage = message
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle ?: "Notification") },
            text = { Text(dialogMessage ?: "") },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    LaunchedEffect(true) {
        currencies = apiManager.getCurrencies().body() ?: emptyList()
    }

    var selectedIndex by remember { mutableIntStateOf(0) }

    RequestNotificationPermission()
    2
    Scaffold(
        bottomBar = {
            BottomNavigationBar(selectedIndex = selectedIndex) {
                selectedIndex = it
            }
        },
        containerColor = Color(0xFF1B1C2E)
    ) {
        Box(modifier = Modifier.padding(it)) {
            when (selectedIndex) {
                0 -> CurrenciesScreen()
                1 -> CalculatorScreen()
                2 -> HistoryScreen()
                3 -> ProfileScreen()
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    val items = listOf(
        Icons.Default.CurrencyBitcoin to "Currencies",
        Icons.Default.Calculate to "Calculator",
        Icons.Default.History to "History",
        Icons.Default.AccountCircle to "Profile"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2D42))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { index, (icon, label) ->
            CustomNavItem(
                icon = icon,
                label = label,
                isSelected = index == selectedIndex,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

@Composable
fun CustomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor = if (isSelected) Color.White else Color.Gray
    val indicatorColor = if (isSelected) Color(0xFFE17C0D) else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(indicatorColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = iconColor)
        Text(text = label, color = iconColor, fontSize = 12.sp)
    }
}

@Composable
fun CurrenciesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Currencies",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        BalancePieChart()

        Spacer(modifier = Modifier.height(16.dp))

        AssetsSection()
    }
}

val gradientPalette = listOf(
    Brush.linearGradient(listOf(Color(0xFFE17C0D), Color(0xFFFFA726))),
    Brush.linearGradient(listOf(Color(0xFF1CA0F2), Color(0xFF4FC3F7))),
    Brush.linearGradient(listOf(Color(0xFFF4C542), Color(0xFFFFE082))),
    Brush.linearGradient(listOf(Color(0xFF9A68FF), Color(0xFFB388FF))),
    Brush.linearGradient(listOf(Color(0xFF00C49F), Color(0xFF4DD0E1))),
    Brush.linearGradient(listOf(Color(0xFF8884D8), Color(0xFFB39DDB)))
)

@Composable
fun BalancePieChart() {
    var animatedSweep by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currencies) {
        animatedSweep = 0f
        animate(0f, 360f, animationSpec = tween(1000)) { value, _ ->
            animatedSweep = value
        }
    }

    val sortedCurrencies = currencies
        .filter { it.current_price != null && it.current_price > 0 }
        .sortedByDescending { it.current_price }

    val topCurrencies = sortedCurrencies.take(5)
    val othersSum = sortedCurrencies.drop(5).sumOf { it.current_price?.toDouble() ?: 0.0 }.toFloat()

    val pieData = buildList {
        topCurrencies.forEachIndexed { index, currency ->
            add(
                PieData(
                    value = currency.current_price!!.toFloat(),
                    color = Color.White, // Placeholder, will use gradient in Canvas
                    label = currency.name ?: "Unknown"
                )
            )
        }
        if (othersSum > 0) {
            add(
                PieData(
                    value = othersSum,
                    color = Color.White, // Placeholder, will use gradient in Canvas
                    label = "Others"
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .background(Color(0xFF2B2D42))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                pieData = pieData,
                animatedSweep = animatedSweep,
                modifier = Modifier.size(180.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pieData.forEachIndexed { index, data ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                gradientPalette[index % gradientPalette.size],
                                RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${data.label}: ${
                            String.format(
                                java.util.Locale.US,
                                "%.2f",
                                data.value
                            )
                        } USD",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AssetsSection() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(currencies.size) { item ->
            val currency = currencies.getOrNull(item)
            val marketCap = currencies.getOrNull(item)?.market_cap_change_percentage_24h
            if (currency != null) {
                if (marketCap != null) {
                    AssetItem(
                        name = currency.name ?: "Unknown",
                        code = currency.symbol ?: "Unknown",
                        value = (currency.current_price ?: "Unknown").toString(),
                        change = String.format(
                            java.util.Locale.US,
                            "%.2f",
                            currency.market_cap_change_percentage_24h ?: 0.0
                        ) + "%",
                        changeColor = if (marketCap >= 0) {
                            Color.Green
                        } else {
                            Color.Red
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AssetItem(name: String, code: String, value: String, change: String, changeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2D42), shape = RoundedCornerShape(10.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = code, color = Color.Gray, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = change, color = changeColor, fontSize = 12.sp)
        }
    }
}

@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("FCM", "Notification permission granted: $isGranted")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

//@OptIn(ExperimentalPermissionsApi::class)
//@Composable
//fun RequestNotificationPermission() {
//    val permissionState = rememberPermissionState(
//        permission = android.Manifest.permission.POST_NOTIFICATIONS
//    )
//
//    LaunchedEffect(Unit) {
//        if (!permissionState.status.isGranted && !permissionState.status.shouldShowRationale) {
//            permissionState.launchPermissionRequest()
//        }
//    }
//}

fun getFcmToken() {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w("FCM", "Fetching FCM registration token failed", task.exception)
            return@addOnCompleteListener
        }
        val token = task.result
        Log.d("FCM", "Token: $token")
        // Send this token to your server if needed
    }
}