package com.example.turbolaravelstarterkitexample.data

import android.util.Log
import com.example.turbolaravelstarterkitexample.Urls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MenuService {
    
    suspend fun fetchMenuItems(): List<MenuItem> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${Urls.baseUrl}/api/native-menu")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            Log.d("MenuService", "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                Log.d("MenuService", "Response: $response")
                
                val jsonResponse = JSONObject(response)
                val menuItemsArray = jsonResponse.getJSONArray("menu_items")
                
                val menuItems = mutableListOf<MenuItem>()
                for (i in 0 until menuItemsArray.length()) {
                    val item = menuItemsArray.getJSONObject(i)
                    menuItems.add(
                        MenuItem(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            icon = item.getString("icon"),
                            url = item.getString("url")
                        )
                    )
                }
                
                Log.d("MenuService", "Parsed ${menuItems.size} menu items")
                menuItems
            } else {
                Log.e("MenuService", "HTTP error: $responseCode")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("MenuService", "Error fetching menu items", e)
            // Return fallback menu items
            listOf(
                MenuItem("dashboard", "Dashboard", "home", "${Urls.baseUrl}/dashboard"),
                MenuItem("settings", "Settings", "settings", "${Urls.baseUrl}/settings")
            )
        }
    }
}
