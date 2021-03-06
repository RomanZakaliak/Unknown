package com.example.CSWordApp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationProvider {

    private String CHANNEL_ID = "Default";
    private String Title = "CS Word App";
    private String Message = "";
    private String CHANNEL_NAME = "Default";
    private final int notificationId = (int)(Math.random() * (10 - 1 + 1) + 1);
    private final Context context;

    public NotificationProvider(String CHANNEL_ID, String Title, String Message, String CHANNEL_NAME, Context context){
        this.CHANNEL_ID = CHANNEL_ID;
        this.CHANNEL_NAME = CHANNEL_NAME;
        this.Title = Title;
        this.Message = Message;
        this.context = context;
    }

    public void showNotification() {
        createNotificationChannel();

        VectorDrawable drawable = (VectorDrawable) ContextCompat.getDrawable(context, R.drawable.ic_classroom);
        Bitmap picture = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(picture);
        drawable.setBounds(0,0,canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify_small_icon)
                .setLargeIcon(picture)
                .setContentTitle(Title)
                .setContentText(Message)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(Message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription("text");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
