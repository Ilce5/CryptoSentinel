package mk.com.cryptosentinel.api.service

import mk.com.cryptosentinel.api.model.response.Currencies
import mk.com.cryptosentinel.api.model.response.HistoricalCurrencies
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiManager: ApiService {

    private val apiService: ApiService

//    private val _userCardList = MutableStateFlow<List<UserCardResponse>>(emptyList())
//    val userCardList: StateFlow<List<UserCardResponse>> get() = _userCardList

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
//            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

//    override suspend fun getUserCards(
//        uid: String,
//        dbPath: String
//    ): Response<List<UserCardResponse>> {
//        val response = apiService.getUserCards(uid, dbPath)
//        _userCardList.value = response.body().orEmpty()
//        //println(response)
//        return response
//    }

    override suspend fun getCurrencies(vsCurrency: String): Response<List<Currencies>> {
        return try {
            val result = apiService.getCurrencies()
            val code = result.code()
            println("getCurrencies code: $code")
            result
        } catch (e: Exception) {
            println("Exception: ${e.message}")
            Response.error(500, ResponseBody.create(MediaType.get("application/json"), ""))
        }
    }

    override suspend fun getHistoryById(
        id: String,
        vsCurrency: String,
        from: Long,
        to: Long
    ): Response<HistoricalCurrencies> {
        return try {
            val result = apiService.getHistoryById(id, vsCurrency, from, to)
            val code = result.code()
            println("getHistoryById code: $code")
            result
        } catch (e: Exception) {
            println("Exception: ${e.message}")
            Response.error(501, ResponseBody.create(MediaType.get("application/json"), ""))
        }
    }
}