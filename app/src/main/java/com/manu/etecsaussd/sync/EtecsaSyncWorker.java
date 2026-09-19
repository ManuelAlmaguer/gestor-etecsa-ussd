package com.manu.etecsaussd.sync;

import android.content.Context;
import android.telephony.SubscriptionInfo;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.manu.etecsaussd.EtecsaApplication;
import com.manu.etecsaussd.domain.EtecsaRepository;
import com.manu.etecsaussd.domain.SyncReport;
import com.manu.etecsaussd.telephony.UssdExecutor;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Daily background refresh. USSD work is only attempted after runtime permissions exist. */
public final class EtecsaSyncWorker extends Worker {
    public static final String UNIQUE_WORK_NAME = "etecsa-daily-sync";

    public EtecsaSyncWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    @SuppressWarnings("MissingPermission")
    public Result doWork() {
        EtecsaApplication application = (EtecsaApplication) getApplicationContext();
        UssdExecutor ussdExecutor = application.getUssdExecutor();
        if (!ussdExecutor.hasRequiredPermissions()) {
            return Result.failure();
        }

        EtecsaRepository repository = application.getRepository();
        List<SubscriptionInfo> activeSubscriptions;
        try {
            activeSubscriptions = ussdExecutor.getActiveSubscriptions();
        } catch (Exception exception) {
            return Result.failure();
        }
        if (activeSubscriptions.isEmpty()) {
            return Result.retry();
        }

        int totalSuccesses = 0;
        boolean retryableFailure = false;
        for (SubscriptionInfo subscription : activeSubscriptions) {
            SyncReport report = repository.syncAll(ussdExecutor, subscription.getSubscriptionId());
            totalSuccesses += report.successCount;
            retryableFailure |= report.hasRetryableFailure;
            if (report.successCount > 0) {
                EtecsaNotificationScheduler.scheduleForSubscription(
                        getApplicationContext(),
                        repository,
                        subscription.getSubscriptionId()
                );
            }
        }
        if (totalSuccesses == 0 && retryableFailure) {
            return Result.retry();
        }
        return totalSuccesses == 0 ? Result.failure() : Result.success();
    }

    public static void enqueueDailySync(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                EtecsaSyncWorker.class,
                24,
                TimeUnit.HOURS
        )
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                )
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }
}
