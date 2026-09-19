package com.manu.etecsaussd.domain;

import android.annotation.SuppressLint;

import com.manu.etecsaussd.data.EtecsaDatabase;
import com.manu.etecsaussd.data.entity.BalanceSnapshotEntity;
import com.manu.etecsaussd.data.entity.DataUsageSnapshotEntity;
import com.manu.etecsaussd.data.entity.RechargeStatusEntity;
import com.manu.etecsaussd.data.entity.VoiceSmsSnapshotEntity;
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
                database.rechargeStatusDao().getLatest()
        );
    }

    @SuppressLint("MissingPermission")
    public SyncReport syncAll(UssdExecutor ussdExecutor) {
        int subscriptionId = -1;
        int successCount = 0;
        int failureCount = 0;
        boolean retryableFailure = false;
        List<String> errors = new ArrayList<>();
        Integer selectedSubscriptionId = ussdExecutor.getSelectedSubscriptionId();

        for (String code : EtecsaUssdCodes.dashboardCodes()) {
            try {
                UssdResponse response = ussdExecutor.executeBlocking(code, selectedSubscriptionId);
                subscriptionId = response.subscriptionId;
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
                EtecsaParsers.BalanceData balance = EtecsaParsers.parseBalance(response.response);
                if (balance == null) {
                    throw new IllegalArgumentException("no se encontró un saldo CUP");
                }
                database.balanceSnapshotDao().insert(new BalanceSnapshotEntity(
                        response.subscriptionId,
                        balance.amountCup,
                        response.response,
                        capturedAt
                ));
                break;
            case EtecsaUssdCodes.MOBILE_DATA:
                EtecsaParsers.DataUsageData data = EtecsaParsers.parseDataUsage(response.response);
                if (data == null) {
                    throw new IllegalArgumentException("no se encontraron unidades de datos");
                }
                database.dataUsageSnapshotDao().insert(new DataUsageSnapshotEntity(
                        response.subscriptionId,
                        data.lteMegabytes,
                        data.allNetworksMegabytes,
                        data.totalMegabytes,
                        response.response,
                        capturedAt
                ));
                break;
            case EtecsaUssdCodes.VOICE_SMS:
                EtecsaParsers.VoiceSmsData voiceSms = EtecsaParsers.parseVoiceSms(response.response);
                if (voiceSms == null) {
                    throw new IllegalArgumentException("no se encontraron minutos o SMS");
                }
                database.voiceSmsSnapshotDao().insert(new VoiceSmsSnapshotEntity(
                        response.subscriptionId,
                        voiceSms.voiceMinutes,
                        voiceSms.smsMessages,
                        response.response,
                        capturedAt
                ));
                break;
            case EtecsaUssdCodes.RECHARGE_STATUS:
                EtecsaParsers.RechargeData recharge = EtecsaParsers.parseRechargeStatus(response.response);
                if (recharge == null) {
                    throw new IllegalArgumentException("no se encontró estado de recarga");
                }
                database.rechargeStatusDao().insert(new RechargeStatusEntity(
                        response.subscriptionId,
                        recharge.amountCup,
                        recharge.expirationDateIso,
                        response.response,
                        capturedAt
                ));
                break;
            default:
                throw new IllegalArgumentException("código USSD no soportado");
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "error desconocido" : message;
    }
}
