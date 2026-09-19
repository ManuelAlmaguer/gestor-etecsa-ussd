package com.manu.etecsaussd.domain;

import android.annotation.SuppressLint;

import com.manu.etecsaussd.data.EtecsaDatabase;
import com.manu.etecsaussd.data.entity.BalanceSnapshotEntity;
import com.manu.etecsaussd.data.entity.DataUsageSnapshotEntity;
import com.manu.etecsaussd.data.entity.RechargeStatusEntity;
import com.manu.etecsaussd.data.entity.VoiceSmsSnapshotEntity;
import com.manu.etecsaussd.data.entity.PackageStatusEntity;
import com.manu.etecsaussd.data.model.DashboardSnapshot;
import com.manu.etecsaussd.parser.EtecsaParsers;
import com.manu.etecsaussd.telephony.EtecsaUssdCodes;
import com.manu.etecsaussd.telephony.UssdExecutionException;
import com.manu.etecsaussd.telephony.UssdExecutor;
import com.manu.etecsaussd.telephony.UssdResponse;

import java.util.ArrayList;
import java.util.List;

public final class EtecsaRepository {
    private final EtecsaDatabase database;

    public EtecsaRepository(EtecsaDatabase database) {
        this.database = database;
    }

    public DashboardSnapshot getLatestDashboard() {
        return new DashboardSnapshot(
                database.balanceSnapshotDao().getLatest(),
                database.dataUsageSnapshotDao().getLatest(),
                database.voiceSmsSnapshotDao().getLatest(),
                database.rechargeStatusDao().getLatest(),
                null
        );
    }

    public DashboardSnapshot getLatestDashboard(int subscriptionId) {
        return new DashboardSnapshot(
                database.balanceSnapshotDao().getLatestForSubscription(subscriptionId),
                database.dataUsageSnapshotDao().getLatestForSubscription(subscriptionId),
                database.voiceSmsSnapshotDao().getLatestForSubscription(subscriptionId),
                database.rechargeStatusDao().getLatestForSubscription(subscriptionId),
                database.packageStatusDao().getLatestForSubscription(subscriptionId)
        );
    }

    public PackageStatusEntity getLatestPackageStatus(int subscriptionId) {
        return database.packageStatusDao().getLatestForSubscription(subscriptionId);
    }

    public RechargeStatusEntity getLatestRechargeStatus(int subscriptionId) {
        return database.rechargeStatusDao().getLatestForSubscription(subscriptionId);
    }

    @SuppressLint("MissingPermission")
    public SyncReport syncAll(UssdExecutor ussdExecutor) {
        Integer selectedSubscriptionId = ussdExecutor.getActionSubscriptionId();
        int subscriptionId;
        try {
            subscriptionId = ussdExecutor.resolveSubscriptionIdForUi(selectedSubscriptionId);
        } catch (UssdExecutionException exception) {
            return new SyncReport(
                    -1,
                    0,
                    EtecsaUssdCodes.dashboardCodes().size(),
                    exception.isRetryable(),
                    java.util.Collections.singletonList(exception.getMessage())
            );
        }
        return syncAll(ussdExecutor, subscriptionId);
    }

    @SuppressLint("MissingPermission")
    public SyncReport syncAll(UssdExecutor ussdExecutor, int subscriptionId) {
        int successCount = 0;
        int failureCount = 0;
        boolean retryableFailure = false;
        List<String> errors = new ArrayList<>();
        for (String code : EtecsaUssdCodes.dashboardCodes()) {
            try {
                UssdResponse response = ussdExecutor.executeBlocking(code, subscriptionId);
                persist(code, response);
                successCount++;
            } catch (UssdExecutionException | IllegalArgumentException exception) {
                failureCount++;
                if (exception instanceof UssdExecutionException) {
                    retryableFailure |= ((UssdExecutionException) exception).isRetryable();
                }
                errors.add(code + ": " + safeMessage(exception));
            } catch (RuntimeException exception) {
                failureCount++;
                retryableFailure = true;
                errors.add(code + ": no se pudo guardar la respuesta.");
            }
        }
        return new SyncReport(subscriptionId, successCount, failureCount, retryableFailure, errors);
    }

    private void persist(String code, UssdResponse response) {
        long capturedAt = response.receivedAt;
        switch (code) {
            case EtecsaUssdCodes.MAIN_BALANCE:
                EtecsaParsers.MainBalanceData main = EtecsaParsers.parseMainBalance(response.response);
                if (main == null) {
                    throw new IllegalArgumentException("no se encontraron datos de la línea");
                }
                boolean mainDataPersisted = false;
                if (main.balance != null) {
                    database.balanceSnapshotDao().insert(new BalanceSnapshotEntity(
                            response.subscriptionId,
                            main.balance.amountCup,
                            main.lineActiveUntilIso,
                            main.packageExpirationIso,
                            response.response,
                            capturedAt
                    ));
                    mainDataPersisted = true;
                }
                if (main.dataUsage != null) {
                    insertDataUsage(response, main.dataUsage, capturedAt);
                    mainDataPersisted = true;
                }
                if (main.voiceSms != null) {
                    insertVoiceSms(response, main.voiceSms, capturedAt);
                    mainDataPersisted = true;
                }
                if (main.packageExpirationIso != null) {
                    database.packageStatusDao().insert(new PackageStatusEntity(
                            response.subscriptionId,
                            main.packageExpirationIso,
                            response.response,
                            capturedAt
                    ));
                    mainDataPersisted = true;
                }
                if (!mainDataPersisted) {
                    throw new IllegalArgumentException("no se encontraron datos utilizables");
                }
                break;
            case EtecsaUssdCodes.MOBILE_DATA:
                EtecsaParsers.DataUsageData data = EtecsaParsers.parseDataUsage(response.response);
                if (data == null) {
                    throw new IllegalArgumentException("no se encontraron unidades de datos");
                }
                insertDataUsage(response, data, capturedAt);
                break;
            case EtecsaUssdCodes.VOICE_SMS:
                EtecsaParsers.VoiceSmsData voiceSms = EtecsaParsers.parseVoiceSms(response.response);
                if (voiceSms == null) {
                    throw new IllegalArgumentException("no se encontraron minutos o SMS");
                }
                insertVoiceSms(response, voiceSms, capturedAt);
                break;
            case EtecsaUssdCodes.RECHARGE_STATUS:
                EtecsaParsers.RechargeData recharge = EtecsaParsers.parseRechargeStatus(response.response);
                if (recharge == null) {
                    throw new IllegalArgumentException("no se encontró estado de recarga");
                }
                database.rechargeStatusDao().insert(new RechargeStatusEntity(
                        response.subscriptionId,
                        recharge.rechargedThisCycleCup,
                        recharge.limitDateIso,
                        recharge.rechargedThisCycleCup,
                        recharge.remainingRechargeCup,
                        recharge.limitCup,
                        recharge.limitDateIso,
                        recharge.rechargeAvailableDateIso,
                        recharge.limitReached,
                        response.response,
                        capturedAt
                ));
                break;
            default:
                throw new IllegalArgumentException("código USSD no soportado");
        }
    }

    private void insertDataUsage(
            UssdResponse response,
            EtecsaParsers.DataUsageData data,
            long capturedAt
    ) {
        Long allNetworks = data.allNetworksMegabytes != null
                ? data.allNetworksMegabytes
                : data.totalMegabytes != null ? data.totalMegabytes : data.lteMegabytes;
        database.dataUsageSnapshotDao().insert(new DataUsageSnapshotEntity(
                response.subscriptionId,
                data.lteMegabytes,
                allNetworks,
                allNetworks,
                response.response,
                capturedAt
        ));
    }

    private void insertVoiceSms(
            UssdResponse response,
            EtecsaParsers.VoiceSmsData voiceSms,
            long capturedAt
    ) {
        database.voiceSmsSnapshotDao().insert(new VoiceSmsSnapshotEntity(
                response.subscriptionId,
                voiceSms.voiceMinutes,
                voiceSms.voiceSeconds,
                voiceSms.smsMessages,
                response.response,
                capturedAt
        ));
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "error desconocido" : message;
    }
}
