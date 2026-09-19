package com.manu.etecsaussd.sync;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.manu.etecsaussd.data.AppPreferences;

public final class EtecsaReminderWorker extends Worker {
    private static final String CHANNEL_ID = "etecsa_alerts";
    private static final String KEY_EVENT = "event";
    private static final String KEY_EVENT_KEY = "event_key";
    private static final String KEY_TARGET_DATE = "target_date";
    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY = "body";

    public EtecsaReminderWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppPreferences preferences = AppPreferences.from(context);
        String event = getInputData().getString(KEY_EVENT);
        String eventKey = getInputData().getString(KEY_EVENT_KEY);
        String targetDate = getInputData().getString(KEY_TARGET_DATE);
        if (!preferences.notificationsEnabled() || !isEventEnabled(preferences, event)) {
            return Result.success();
        }
        if (eventKey == null || targetDate == null || targetDate.equals(preferences.getNotificationEventSent(eventKey))) {
            return Result.success();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }

        createNotificationChannel(context);
        String title = getInputData().getString(KEY_TITLE);
        String body = getInputData().getString(KEY_BODY);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title == null ? "Manu ETECSA" : title)
                .setContentText(body == null ? "Revisa el estado de tu línea." : body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(eventKey.hashCode(), notification.build());
        preferences.setNotificationEventSent(eventKey, targetDate);
        return Result.success();
    }

    private boolean isEventEnabled(AppPreferences preferences, String event) {
        if (EtecsaNotificationScheduler.EVENT_PACKAGE_5.equals(event)) return preferences.packageReminder5();
        if (EtecsaNotificationScheduler.EVENT_PACKAGE_3.equals(event)) return preferences.packageReminder3();
        if (EtecsaNotificationScheduler.EVENT_PACKAGE_1.equals(event)) return preferences.packageReminder1();
        if (EtecsaNotificationScheduler.EVENT_RECHARGE_LIMIT.equals(event)) return preferences.rechargeLimitReminder();
        if (EtecsaNotificationScheduler.EVENT_RECHARGE_AVAILABLE.equals(event)) return preferences.rechargeAvailableReminder();
        return false;
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alertas de Manu ETECSA",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Avisos de paquetes y límite mensual de recargas");
        manager.createNotificationChannel(channel);
    }
}
