package mk.com.cryptosentinel.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mk.com.cryptosentinel.GlobalState.currencies

@Composable
fun CalculatorScreen() {
    var expanded1 by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }

    var selectedOption1 by remember { mutableStateOf(currencies.getOrNull(0)?.name ?: "") }
    var selectedOption2 by remember { mutableStateOf(currencies.getOrNull(1)?.name ?: "") }

    var selectedCurrencyValue by remember { mutableDoubleStateOf(0.0) }

    var result by remember { mutableDoubleStateOf(0.0) }

    val currencyNames = currencies.mapNotNull { it.name }
    val currencyValues = currencies.mapNotNull { it.current_price }

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column {
                Box {
                    OutlinedButton(
                        border = BorderStroke(1.dp, Color(0xFFE17C0D)),
                        onClick = { expanded1 = true },
                    ) {
                        Text(selectedOption1)
                    }
                    DropdownMenu(
                        expanded = expanded1,
                        onDismissRequest = { expanded1 = false }
                    ) {
                        currencyNames.forEach { option ->
                            val index = currencyNames.indexOf(option)
                            val value = currencyValues.getOrNull(index)?.toString() ?: ""
                            DropdownMenuItem(
                                onClick = {
                                    selectedOption1 = option
                                    expanded1 = false
                                },
                                text = { Text(color = Color.White, text = "$option - $value") }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    Text(
                        color = Color.White,
                        text = "Value: ",
                    )
                    Text(
                        color = Color(0xFFE17C0D),
                        text = currencyNames.indexOf(selectedOption1)
                            .takeIf { it >= 0 && it < currencyValues.size }
                            ?.let { currencyValues[it] }.toString()
                    )
                }


            }

            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Swap Icon",
                modifier = Modifier.size(24.dp)
            )

            Column {
                Box {
                    OutlinedButton(
                        border = BorderStroke(1.dp, Color(0xFFE17C0D)),
                        onClick = { expanded2 = true },
                    ) {
                        Text(selectedOption2)
                    }
                    DropdownMenu(
                        expanded = expanded2,
                        onDismissRequest = { expanded2 = false }
                    ) {
                        currencyNames.forEach { option ->
                            val index = currencyNames.indexOf(option)
                            val value = currencyValues.getOrNull(index)?.toString() ?: ""
                            DropdownMenuItem(
                                onClick = {
                                    selectedOption2 = option
                                    expanded2 = false
                                },
                                text = { Text(color = Color.White, text = "$option - $value") }
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    Text(
                        color = Color.White,
                        text = "Value: ",
                    )
                    Text(
                        color = Color(0xFFE17C0D),
                        text = currencyNames.indexOf(selectedOption2)
                                .takeIf { it >= 0 && it < currencyValues.size }
                                ?.let { currencyValues[it] }.toString()
                    )
                    selectedCurrencyValue = currencyNames.indexOf(selectedOption1)
                        .takeIf { it >= 0 && it < currencyValues.size }
                        ?.let { currencyValues[it] } ?: 0.0
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        var inputText by remember { mutableStateOf("") }

        OutlinedTextField(
            value = inputText,
            onValueChange = { newValue ->
                // Regex: max 2 decimal places
                if (newValue.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) {
                    inputText = newValue
                }
                val index1 = currencyNames.indexOf(selectedOption1)
                val index2 = currencyNames.indexOf(selectedOption2)
                val value1 = currencyValues.getOrNull(index1) ?: 1.0
                val value2 = currencyValues.getOrNull(index2) ?: 1.0

                result = inputText.toDoubleOrNull()?.times(value1 / value2) ?: 0.0            },
            label = { Text("$selectedOption1 Amount", color = Color(0xFFE17C0D)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE17C0D),
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .height(70.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.End,
//                color = Color.White // Optional: change text color if needed
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .border(BorderStroke(1.dp, Color(0xFFE17C0D)), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "$selectedOption2:", color = Color.White)
                Text(text = "%.2f".format(result), color = Color.White)
            }
        }
    }
}
