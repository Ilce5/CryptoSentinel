package mk.com.cryptosentinel.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mk.com.cryptosentinel.api.service.ApiManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import mk.com.cryptosentinel.GlobalState.currencies
import kotlin.math.roundToInt


// Assume ApiManager and data classes are defined elsewhere
// data class HistoricalPrice(val timestamp: Long, val price: Double)

data class HistoricalPrice(
    val timestamp: Long,
    val price: Double
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryScreen() {
    val scope = rememberCoroutineScope()
    var selectedCurrency by remember { mutableStateOf("Bitcoin") }
    var fromDate by remember { mutableStateOf(TextFieldValue(LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE))) }
    var toDate by remember { mutableStateOf(TextFieldValue(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))) }
    var historicalData by remember { mutableStateOf<List<HistoricalPrice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val currenciesName = currencies.map { it.name }
    var expanded by remember { mutableStateOf(false) }
    val apiManager = ApiManager()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Currency Dropdown
        Box {
            OutlinedTextField(
                value = selectedCurrency,
                onValueChange = { },
                label = { Text("Currency", color = Color(0xFFE17C0D)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE17C0D),
                    unfocusedBorderColor = Color(0xFFE17C0D),
                ),
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Currency")
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                currenciesName.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency?.uppercase() ?: "") },
                        onClick = {
                            selectedCurrency = currency.toString()
                            expanded = false
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val fromTimestamp = LocalDate.parse(fromDate.text).toUnixTimestamp()
                                    val toTimestamp = LocalDate.parse(toDate.text).toUnixTimestamp()

                                    val response = apiManager.getHistoryById(
                                        currencies.find { it.name == selectedCurrency }?.id ?: "",
                                        "usd",
                                        fromTimestamp,
                                        toTimestamp
                                    )
                                    val body = response.body()
                                    historicalData = body?.prices?.map {
                                        HistoricalPrice(timestamp = it[0].toLong(), price = it[1])
                                    } ?: emptyList()
                                } catch (e: Exception) {
                                    error = "Failed to load data: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
            }
        }

        // Date Input Fields
        OutlinedTextField(
            value = fromDate,
            onValueChange = { fromDate = it },
            label = { Text("From Date (YYYY-MM-DD)", color = Color(0xFFE17C0D)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE17C0D),
                unfocusedBorderColor = Color(0xFFE17C0D),
            ),
            modifier = Modifier.fillMaxWidth(),
            isError = !isValidDate(fromDate.text)
        )
        OutlinedTextField(
            value = toDate,
            onValueChange = { toDate = it },
            label = { Text("To Date (YYYY-MM-DD)", color = Color(0xFFE17C0D)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE17C0D),
                unfocusedBorderColor = Color(0xFFE17C0D),
            ),
            modifier = Modifier.fillMaxWidth(),
            isError = !isValidDate(toDate.text)
        )

        // Fetch Data Button
        Button(
            onClick = {
                if (isValidDate(fromDate.text) && isValidDate(toDate.text)) {
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            val fromTimestamp = LocalDate.parse(fromDate.text).toUnixTimestamp()
                            val toTimestamp = LocalDate.parse(toDate.text).toUnixTimestamp()

                            val response = apiManager.getHistoryById(
                                selectedCurrency,
                                "usd",
                                fromTimestamp,
                                toTimestamp
                            )
                            val body = response.body()
                            historicalData = body?.prices?.map {
                                HistoricalPrice(timestamp = it[0].toLong(), price = it[1])
                            } ?: emptyList()
                        } catch (e: Exception) {
                            error = "Failed to load data: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE17C0D),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = isValidDate(fromDate.text) && isValidDate(toDate.text)
        ) {
            Text("Load Historical Data")
        }

        // Loading and Error States
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Line Chart
        if (historicalData.isNotEmpty()) {
            LineChart(
                data = historicalData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun isValidDate(date: String): Boolean {
    return try {
        LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LineChart(
    data: List<HistoricalPrice>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val lineColor = Color(0xFFE17C0D)
    val labelStyle = Color(0xFFE17C0D)
    val textColor = Color.White

    val startDate = Instant.ofEpochSecond(data.first().timestamp / 1000)
        .atZone(ZoneOffset.UTC).toLocalDate().toString()
    val endDate = Instant.ofEpochSecond(data.last().timestamp / 1000)
        .atZone(ZoneOffset.UTC).toLocalDate().toString()

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    var touchX by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .pointerInput(true) {
                detectTapGestures { offset ->
                    touchX = offset.x
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val maxPrice = data.maxOf { it.price }
            val minPrice = data.minOf { it.price }
            val priceRange = if (maxPrice == minPrice) 1.0 else maxPrice - minPrice

            val width = size.width
            val height = size.height
            val spacing = width / (data.size - 1)

            val points = data.mapIndexed { index, priceData ->
                val x = index * spacing
                val y = height - (((priceData.price - minPrice) / priceRange).toFloat() * height)
                Offset(x, y)
            }

            val progressIndex = (points.size * animatedProgress.value).toInt().coerceAtMost(points.lastIndex)
            val animatedPoints = points.take(progressIndex + 1)

            val path = Path().apply {
                animatedPoints.forEachIndexed { i, point ->
                    if (i == 0) moveTo(point.x, point.y)
                    else lineTo(point.x, point.y)
                }
            }

            // Draw gradient fill under the line
            val filledPath = Path().apply {
                if (animatedPoints.isNotEmpty()) {
                    moveTo(animatedPoints.first().x, height)
                    animatedPoints.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                    lineTo(animatedPoints.last().x, height)
                    close()
                }
            }
            drawPath(
                path = filledPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.4f), Color.Transparent),
                    startY = 0f,
                    endY = height
                ),
                style = Fill
            )

// Draw the stroke line on top of the gradient
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4f)
            )


            // Draw points
            animatedPoints.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = point
                )
            }

            // Draw tooltip if touched
            touchX?.let { x ->
                val closestIndex = ((x / spacing).roundToInt()).coerceIn(0, data.lastIndex)
                val point = points[closestIndex]
                val price = data[closestIndex].price
                val date = Instant.ofEpochSecond(data[closestIndex].timestamp / 1000)
                    .atZone(ZoneOffset.UTC).toLocalDate().toString()

                // Vertical line
                drawLine(
                    color = lineColor,
                    start = Offset(point.x, 0f),
                    end = Offset(point.x, height),
                    strokeWidth = 2f
                )

                // Dot
                drawCircle(
                    color = lineColor,
                    radius = 8f,
                    center = point
                )

                // Tooltip text
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 36f
                        isAntiAlias = true
                    }

                    val text = "${"%.2f".format(price)}\n$date"
                    val xOffset = point.x.coerceIn(10f, width - 200f)
                    val yOffset = point.y.coerceIn(40f, height - 80f)

                    val lines = text.split("\n")
                    lines.forEachIndexed { i, line ->
                        drawText(line, xOffset, yOffset + i * 40f, paint)
                    }
                }
            }
        }

        // Bottom labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = startDate, color = textColor)
            Text(text = endDate, color = textColor)
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
fun LocalDate.toUnixTimestamp(): Long {
    return this.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
}