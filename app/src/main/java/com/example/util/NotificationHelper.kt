package com.example.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.PulseApplication
import com.example.R

object NotificationHelper {
    private const val NOTIFICATION_ID_BASE = 1000

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = PulseApplication.CHANNEL_MESSAGES
    ) {
        // Only show if permission is granted (for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                android.util.Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open MainActivity when clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationId = NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}
