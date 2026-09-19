package com.manu.etecsaussd.sync;

import android.content.Context;

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

import java.util.concurrent.TimeUnit;

/** Daily background refresh. USSD work is only attempted after runtime permissions exist. */
public final class EtecsaSyncWorker extends Worker {
    public static final String UNIQUE_WORK_NAME = "etecsa-daily-sync";

    public EtecsaSyncWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        EtecsaApplication application = (EtecsaApplication) getApplicationContext();
        UssdExecutor ussdExecutor = application.getUssdExecutor();
        if (!ussdExecutor.hasRequiredPermissions()) {
            return Result.failure();
        }

        EtecsaRepository repository = application.getRepository();
        SyncReport report = repository.syncAll(ussdExecutor);
        if (report.successCount == 0 && report.hasRetryableFailure) {
            return Result.retry();
        }
        if (report.successCount == 0) {
            return Result.failure();
        }
        return Result.success();
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

