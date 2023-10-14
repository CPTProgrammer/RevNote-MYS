package com.revnote.hoyolab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.view.LayoutInflater
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

fun MainActivity.createNotification(
    channelId: String,
    channelName: String
) {
    val remoteView = RemoteViews(packageName, R.layout.layout_notification);

    val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);

    val notificationManager = getSystemService(NotificationManager::class.java);
    notificationManager?.createNotificationChannel(channel);

    val customNotification = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("原神便笺")
        .setContentText("ContentText")
        .setDefaults(Notification.DEFAULT_ALL)
        .setContent(remoteView)
        .setShowWhen(true)
        .setOngoing(true)
        .build();

    notificationManager.notify(1, customNotification);
}

fun MainActivity.destroyNotification() {
    val notificationManager = getSystemService(NotificationManager::class.java);
    notificationManager.cancel(1);
}