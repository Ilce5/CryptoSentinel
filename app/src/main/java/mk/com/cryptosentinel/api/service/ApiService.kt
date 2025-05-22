package mk.com.cryptosentinel.api.service

import mk.com.cryptosentinel.api.model.response.Currencies
import mk.com.cryptosentinel.api.model.response.HistoricalCurrencies
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("coins/markets")
    suspend fun getCurrencies(
        @Query("vs_currency") vsCurrency: String = "usd"
    ): Response<List<Currencies>>

    @GET("coins/{id}/market_chart/range")
    suspend fun getHistoryById(
        @Path("id") id: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<HistoricalCurrencies>

}
