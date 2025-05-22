package mk.com.cryptosentinel.api.model.response

data class HistoricalCurrencies(
    val prices: List<List<Double>>,
    val market_caps: List<List<Double>>,
    val total_volumes: List<List<Double>>
)

