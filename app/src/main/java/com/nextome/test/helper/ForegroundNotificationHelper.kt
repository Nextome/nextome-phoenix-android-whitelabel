package com.nextome.test.helper

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nextome.test.R


object ForegroundNotificationHelper {

    // Example class to create a notification
    fun createNotification(activity: Activity): Notification {
        val builder = Notification.Builder(activity.applicationContext)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("Scanning for Beacons")
        val intent = Intent(activity, activity.javaClass)
        val pendingIntent = PendingIntent.getActivity(
                activity.applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(pendingIntent)

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "some_channel_id"
            val channelName = "Some Channel"
            val importance = NotificationManager.IMPORTANCE_LOW
            var notificationChannel: NotificationChannel? = null
            notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationChannel.enableLights(true)
            notificationManager.createNotificationChannel(notificationChannel)
            builder.setChannelId("some_channel_id")
        }

        return builder.build()
    }
}
