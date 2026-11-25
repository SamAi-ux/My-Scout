package com.example.data.remote

import com.example.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun fetchIndonesiaWeather(
        city: String = "Jakarta",
        latitude: Double = -6.2088,
        longitude: Double = 106.8456
    ): WeatherSnapshot = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                "&timezone=Asia%2FJakarta"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext fallbackWeather(city)
                val raw = resp.body?.string() ?: return@withContext fallbackWeather(city)
                val current = JSONObject(raw).optJSONObject("current")
                    ?: return@withContext fallbackWeather(city)
                val temp = current.optDouble("temperature_2m", 29.0)
                val humidity = current.optInt("relative_humidity_2m", 70)
                val code = current.optInt("weather_code", 1)
                val wind = current.optDouble("wind_speed_10m", 10.0)
                WeatherSnapshot(
                    locationName = city,
                    temperatureC = temp,
                    conditionLabel = mapWmoCodeToIndonesian(code),
                    humidityPercent = humidity,
                    windKmh = wind,
                    isLive = true
                )
            }
        } catch (_: Exception) {
            fallbackWeather(city)
        }
    }

    private fun fallbackWeather(city: String) = WeatherSnapshot(
        locationName = city,
        temperatureC = 29.0,
        conditionLabel = "Cerah Berawan",
        humidityPercent = 68,
        windKmh = 11.0,
        isLive = false
    )

    private fun mapWmoCodeToIndonesian(code: Int): String = when (code) {
        0 -> "Cerah"
        1, 2 -> "Cerah Berawan"
        3 -> "Berawan"
        45, 48 -> "Berkabut"
        51, 53, 55 -> "Gerimis"
        61, 63, 65, 80, 81, 82 -> "Hujan"
        95, 96, 99 -> "Hujan Petir"
        else -> "Berawan"
    }
}
