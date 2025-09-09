package com.example.turbolaravelstarterkitexample.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.turbolaravelstarterkitexample.Urls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ImageUploadService {

    suspend fun uploadImage(context: Context, imageUri: Uri, sessionCookies: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            
            // Convert image to base64 as expected by backend
            val imageBytes = inputStream?.readBytes()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            
            // Generate filename
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "android_capture_${timeStamp}.jpg"
            
            // Create JSON payload
            val jsonPayload = JSONObject().apply {
                put("image", base64Image)
                put("filename", filename)
            }
            
            Log.d("ImageUpload", "Uploading image with filename: $filename")
            
                        val url = URL("${Urls.baseUrl}/images/capture")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 30000
                readTimeout = 30000
                
                // Add session cookies if available
                if (!sessionCookies.isNullOrEmpty()) {
                    setRequestProperty("Cookie", sessionCookies)
                    Log.d("ImageUpload", "Added session cookies to request")
                }
            }
            
            Log.d("ImageUpload", "Starting upload to: $url")
            
            // Send JSON data
            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.writeBytes(jsonPayload.toString())
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            Log.d("ImageUpload", "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                Log.d("ImageUpload", "Upload successful: $response")
                
                // Parse response to get image ID
                val jsonResponse = JSONObject(response)
                val imageId = jsonResponse.optString("image_id")
                
                Log.d("ImageUpload", "Received image ID: $imageId")
                return@withContext imageId
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream))
                val errorResponse = errorReader.readText()
                errorReader.close()
                
                Log.e("ImageUpload", "Upload failed with code $responseCode: $errorResponse")
                null
            }
        } catch (e: Exception) {
            Log.e("ImageUpload", "Error uploading image", e)
            null
        }
    }
}
