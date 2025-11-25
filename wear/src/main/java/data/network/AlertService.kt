package data.network

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AlertService {
    // Send alert to backend
    fun sendWatchRemovedAlert(userId: String) {
        Log.d("NETWORK", "Calling backend alert API")

        val client = OkHttpClient()
        val json = JSONObject()
        json.put("userId", userId)
        json.put("timestamp", System.currentTimeMillis())


        val requestBody = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("https://forgiving-lucia-crudely.ngrok-free.dev/api/alert/watch-removed")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NETWORK", "Failed to call backend", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("NETWORK", "Backend response code: ${response.code}")
                response.close()
            }
        })
    }

}