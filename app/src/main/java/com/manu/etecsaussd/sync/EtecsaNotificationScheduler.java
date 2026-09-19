package com.manu.etecsaussd.sync;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.manu.etecsaussd.data.AppPreferences;
import com.manu.etecsaussd.data.entity.PackageStatusEntity;
import com.manu.etecsaussd.data.entity.RechargeStatusEntity;
import com.manu.etecsaussd.domain.EtecsaRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/** Creates durable, per-SIM reminders after every successful refresh. */
public final class EtecsaNotificationScheduler {
    public static final String EVENT_PACKAGE_5 = "package_5";
    public static final String EVENT_PACKAGE_3 = "package_3";
    public static final String EVENT_PACKAGE_1 = "package_1";
    public static final String EVENT_RECHARGE_LIMIT = "recharge_limit";
    public static final String EVENT_RECHARGE_AVAILABLE = "recharge_available";

    private static final String KEY_EVENT = "event";
    private static final String KEY_EVENT_KEY = "event_key";
    private static final String KEY_TARGET_DATE = "target_date";
    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY = "body";

    private EtecsaNotificationScheduler() {
    }

    public static void scheduleForSubscription(
            Context context,
            EtecsaRepository repository,
            int subscriptionId
    ) {
        AppPreferences preferences = AppPreferences.from(context);
        PackageStatusEntity packageStatus = repository.getLatestPackageStatus(subscriptionId);
        if (packageStatus != null && packageStatus.expirationDateIso != null) {
            LocalDate expiration = parseDate(packageStatus.expirationDateIso);
            if (expiration != null) {
                if (preferences.packageReminder5()) {
                    schedule(context, subscriptionId, EVENT_PACKAGE_5, expiration.minusDays(5),
                            "Paquete ETECSA próximo a vencer",
                            "La SIM " + subscriptionId + " tiene un paquete que vence en 5 días.");
                }
                if (preferences.packageReminder3()) {
                    schedule(context, subscriptionId, EVENT_PACKAGE_3, expiration.minusDays(3),
                            "Paquete ETECSA próximo a vencer",
                            "La SIM " + subscriptionId + " tiene un paquete que vence en 3 días.");
                }
                if (preferences.packageReminder1()) {
                    schedule(context, subscriptionId, EVENT_PACKAGE_1, expiration.minusDays(1),
                            "Paquete ETECSA próximo a vencer",
                            "La SIM " + subscriptionId + " tiene un paquete que vence mañana.");
                }
            }
        }

        RechargeStatusEntity rechargeStatus = repository.getLatestRechargeStatus(subscriptionId);
        if (rechargeStatus == null) {
            return;
        }
        LocalDate limitDate = parseDate(rechargeStatus.limitDateIso);
        LocalDate availableDate = parseDate(rechargeStatus.rechargeAvailableDateIso);
        if (preferences.rechargeLimitReminder() && limitDate != null) {
            schedule(context, subscriptionId, EVENT_RECHARGE_LIMIT, limitDate,
                    "Límite mensual de recarga",
                    "Hoy termina el período del límite de 360 CUP de la SIM " + subscriptionId + ".");
        }
        if (preferences.rechargeAvailableReminder() && availableDate != null) {
            schedule(context, subscriptionId, EVENT_RECHARGE_AVAILABLE, availableDate,
                    "Recarga ETECSA disponible",
                    "Desde hoy puedes volver a recargar hasta 360 CUP en la SIM " + subscriptionId + ".");
        }
    }

    private static void schedule(
            Context context,
            int subscriptionId,
            String event,
            LocalDate targetDate,
            String title,
            String body
    ) {
        if (targetDate == null) {
            return;
        }
        String eventKey = event + "_sim_" + subscriptionId + "_" + targetDate;
        LocalDateTime notifyAt = targetDate.atTime(LocalTime.of(9, 0));
        long delayMillis = Duration.between(
                LocalDateTime.now(ZoneId.systemDefault()),
                notifyAt
        ).toMillis();
        if (delayMillis < 1000L) {
            return;
        }

        Data data = new Data.Builder()
                .putString(KEY_EVENT, event)
                .putString(KEY_EVENT_KEY, eventKey)
                .putString(KEY_TARGET_DATE, targetDate.toString())
                .putString(KEY_TITLE, title)
                .putString(KEY_BODY, body)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EtecsaReminderWorker.class)
                .setInputData(data)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                eventKey,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    private static LocalDate parseDate(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
