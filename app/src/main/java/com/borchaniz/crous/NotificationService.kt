package com.borchaniz.crous

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.borchaniz.crous.models.Item
import com.borchaniz.crous.models.Residence
import com.borchaniz.crous.models.Response
import retrofit2.Call
import retrofit2.Callback
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser


class NotificationService : Service() {
    lateinit var sharedPreferences: SharedPreferences
    lateinit var notificationManager: NotificationManager
    var i = 0
    val channelId = "my_channel_id"
    val body =
        "{\"precision\":5,\"need_aggregation\":true,\"page\":1,\"pageSize\":24,\"sector\":null,\"idTool\":\"0315afe5-e102-11e9-8c39-005056941f86\",\"occupationModes\":[],\"equipment\":[],\"price\":{\"min\":0,\"max\":null},\"location\":[{\"lon\":1.9784,\"lat\":49.1267},{\"lon\":2.7351,\"lat\":48.5634}]}"
    //slim
//        "{\"precision\":4,\"need_aggregation\":true,\"page\":1,\"pageSize\":24,\"sector\":null,\"idTool\":\"0315afe5-e102-11e9-8c39-005056941f86\",\"occupationModes\":[],\"equipment\":[],\"price\":{\"min\":0,\"max\":null},\"location\":[{\"lon\":-3.762,\"lat\":48.4843},{\"lon\":-1.7649,\"lat\":46.9648}]}"

    var persistentId = -1
    var oldResponse = mutableListOf<Item>()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        sharedPreferences = applicationContext.getSharedPreferences("i", Context.MODE_PRIVATE)

        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            buildNotificationChannel()

        sendNotification("CROUS", "The app is currently running", true)

        Timer().scheduleAtFixedRate(0, 1000 * 60 * 3) {
            getNewCrous()
        }
        return START_REDELIVER_INTENT
    }

    private fun getNewCrous() {
        val repo = RetrofitBuilder.getRetrofitInstance().create(RetrofitRepo::class.java)

        val call: Call<Response> = repo.get(JsonParser().parse(body).asJsonObject)
        call.enqueue(object : Callback<Response> {
            override fun onResponse(call: Call<Response>, response: retrofit2.Response<Response>) {
                val newResponse = response.body()?.results?.items
                val diff = newResponse?.filter { newItem ->
                    !oldResponse.any { oldItem ->
                        newItem.id == oldItem.id
                    }
                }
                oldResponse.addAll(diff.orEmpty())
                for (item in diff.orEmpty()) {
                    sendNotification(
                        item.residence?.label.orEmpty(),
                        "${item.label.orEmpty()}\n${item.label.orEmpty()}\n${item.residence?.address.orEmpty()}"
                    )
                }


            }

            override fun onFailure(call: Call<Response>, t: Throwable) {
            }

        })

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildNotificationChannel() {
        val channelName = "My Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun sendNotification(title: String, text: String, persistent: Boolean = false) {
        i = sharedPreferences.getInt("i", 0)


        val notification = Notification.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setChannelId(channelId)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setOngoing(persistent)
            .build()

        if (persistent) persistentId = i

        notificationManager.notify(i, notification)
        i++

        sharedPreferences.edit().putInt("i", i).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(persistentId)


    }

}