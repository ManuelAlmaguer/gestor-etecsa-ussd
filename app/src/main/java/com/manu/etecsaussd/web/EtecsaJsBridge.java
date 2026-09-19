package com.manu.etecsaussd.web;

import android.Manifest;
import android.telephony.SubscriptionInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.RequiresPermission;

import com.manu.etecsaussd.data.entity.BalanceSnapshotEntity;
import com.manu.etecsaussd.data.entity.DataUsageSnapshotEntity;
import com.manu.etecsaussd.data.entity.RechargeStatusEntity;
import com.manu.etecsaussd.data.entity.VoiceSmsSnapshotEntity;
import com.manu.etecsaussd.data.model.DashboardSnapshot;
import com.manu.etecsaussd.domain.EtecsaRepository;
import com.manu.etecsaussd.domain.SyncReport;
import com.manu.etecsaussd.telephony.EtecsaActionExecutor;
import com.manu.etecsaussd.telephony.UssdExecutionException;
import com.manu.etecsaussd.telephony.UssdExecutor;
import com.manu.etecsaussd.telephony.UssdResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Narrow, asynchronous bridge between the local HTML dashboard and Android services.
 * Every method returns through window.EtecsaNative.onResponse(requestId, payload).
 */
public final class EtecsaJsBridge {
    private static final String REQUEST_ID_PATTERN = "[A-Za-z0-9_-]{1,64}";

    private final WebView webView;
    private final EtecsaRepository repository;
    private final UssdExecutor ussdExecutor;
    private final EtecsaActionExecutor actionExecutor;
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);

    public EtecsaJsBridge(WebView webView, EtecsaRepository repository, UssdExecutor ussdExecutor) {
        this.webView = webView;
        this.repository = repository;
        this.ussdExecutor = ussdExecutor;
        this.actionExecutor = new EtecsaActionExecutor(ussdExecutor);
    }

    @JavascriptInterface
    public void getDashboard(String requestId) {
        runAsync(requestId, this::dashboardPayload);
    }

    @JavascriptInterface
    public void sync(String requestId) {
        runAsync(requestId, () -> {
            SyncReport report = repository.syncAll(ussdExecutor);
            JSONObject payload = new JSONObject();
            payload.put("ok", report.successCount > 0);
            payload.put("partial", report.failureCount > 0);
            payload.put("successCount", report.successCount);
            payload.put("failureCount", report.failureCount);
            payload.put("subscriptionId", report.subscriptionId);
            JSONArray errors = new JSONArray();
            for (String error : report.errors) {
                errors.put(error);
            }
            payload.put("errors", errors);
            payload.put("data", dashboardPayload().get("data"));
            return payload;
        });
    }

    @JavascriptInterface
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public void getSimCards(String requestId) {
        runAsync(requestId, () -> {
            JSONObject payload = new JSONObject();
            payload.put("ok", true);
            payload.put("selectedSubscriptionId", nullableInteger(ussdExecutor.getSelectedSubscriptionId()));
            payload.put("sims", simCardsJson());
            return payload;
        });
    }

    @JavascriptInterface
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public void selectSim(int subscriptionId, String requestId) {
        runAsync(requestId, () -> {
            ussdExecutor.setSelectedSubscriptionId(subscriptionId);
            JSONObject payload = new JSONObject();
            payload.put("ok", true);
            payload.put("selectedSubscriptionId", subscriptionId);
            payload.put("sims", simCardsJson());
            return payload;
        });
    }

    @JavascriptInterface
    public void purchasePackage(String packageOption, String requestId) {
        runAsync(requestId, () -> {
            UssdResponse response = actionExecutor.purchasePackage(
                    packageOption,
                    ussdExecutor.getSelectedSubscriptionId()
            );
            return actionPayload(response);
        });
    }

    @JavascriptInterface
    public void transferBalance(
            String phoneNumber,
            String password,
            String amount,
            String requestId
    ) {
        runAsync(requestId, () -> {
            UssdResponse response = actionExecutor.transferBalance(
                    phoneNumber,
                    password,
                    amount,
                    ussdExecutor.getSelectedSubscriptionId()
            );
            return actionPayload(response);
        });
    }

    private JSONObject dashboardPayload() throws Exception {
        DashboardSnapshot dashboard = repository.getLatestDashboard();
        JSONObject payload = new JSONObject();
        payload.put("ok", true);
        payload.put("data", dashboardJson(dashboard));
        return payload;
    }

    private JSONObject dashboardJson(DashboardSnapshot dashboard) throws Exception {
        JSONObject data = new JSONObject();
        BalanceSnapshotEntity balance = dashboard.balance;
        if (balance == null) {
            data.put("balance", JSONObject.NULL);
        } else {
            JSONObject balanceJson = new JSONObject();
            balanceJson.put("amountCup", balance.amountCup);
            balanceJson.put("capturedAt", balance.capturedAt);
            balanceJson.put("subscriptionId", balance.subscriptionId);
            data.put("balance", balanceJson);
        }

        DataUsageSnapshotEntity dataUsage = dashboard.dataUsage;
        if (dataUsage == null) {
            data.put("dataUsage", JSONObject.NULL);
        } else {
            JSONObject dataJson = new JSONObject();
            dataJson.put("lteMegabytes", nullableLong(dataUsage.lteMegabytes));
            dataJson.put("allNetworksMegabytes", nullableLong(dataUsage.allNetworksMegabytes));
            dataJson.put("totalMegabytes", nullableLong(dataUsage.totalMegabytes));
            dataJson.put("capturedAt", dataUsage.capturedAt);
            dataJson.put("subscriptionId", dataUsage.subscriptionId);
            data.put("dataUsage", dataJson);
        }

        VoiceSmsSnapshotEntity voiceSms = dashboard.voiceSms;
        if (voiceSms == null) {
            data.put("voiceSms", JSONObject.NULL);
        } else {
            JSONObject voiceJson = new JSONObject();
            voiceJson.put("voiceMinutes", nullableLong(voiceSms.voiceMinutes));
            voiceJson.put("smsMessages", nullableLong(voiceSms.smsMessages));
            voiceJson.put("capturedAt", voiceSms.capturedAt);
            voiceJson.put("subscriptionId", voiceSms.subscriptionId);
            data.put("voiceSms", voiceJson);
        }

        RechargeStatusEntity recharge = dashboard.rechargeStatus;
        if (recharge == null) {
            data.put("rechargeStatus", JSONObject.NULL);
        } else {
            JSONObject rechargeJson = new JSONObject();
            rechargeJson.put("amountCup", recharge.amountCup == null ? JSONObject.NULL : recharge.amountCup);
            rechargeJson.put("expirationDateIso", recharge.expirationDateIso == null
                    ? JSONObject.NULL
                    : recharge.expirationDateIso);
            rechargeJson.put("capturedAt", recharge.capturedAt);
            rechargeJson.put("subscriptionId", recharge.subscriptionId);
            data.put("rechargeStatus", rechargeJson);
        }
        return data;
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private JSONArray simCardsJson() throws Exception {
        JSONArray sims = new JSONArray();
        List<SubscriptionInfo> activeSubscriptions = ussdExecutor.getActiveSubscriptions();
        for (SubscriptionInfo info : activeSubscriptions) {
            JSONObject sim = new JSONObject();
            sim.put("subscriptionId", info.getSubscriptionId());
            sim.put("slotIndex", info.getSimSlotIndex() + 1);
            sim.put("displayName", String.valueOf(info.getDisplayName()));
            sim.put("carrierName", String.valueOf(info.getCarrierName()));
            sims.put(sim);
        }
        return sims;
    }

    private JSONObject actionPayload(UssdResponse response) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("ok", true);
        payload.put("request", response.request);
        payload.put("response", response.response);
        payload.put("subscriptionId", response.subscriptionId);
        return payload;
    }

    private Object nullableInteger(Integer value) {
        return value == null ? JSONObject.NULL : value;
    }

    private Object nullableLong(Long value) {
        return value == null ? JSONObject.NULL : value;
    }

    private void runAsync(String requestId, BridgeTask task) {
        String safeRequestId = normalizeRequestId(requestId);
        ioExecutor.execute(() -> {
            try {
                send(safeRequestId, task.run());
            } catch (UssdExecutionException exception) {
                send(safeRequestId, errorPayload(exception.getMessage(), exception.getReason().name()));
            } catch (Exception exception) {
                send(safeRequestId, errorPayload(
                        exception.getMessage() == null ? "No se pudo completar la operación." : exception.getMessage(),
                        "UNEXPECTED_ERROR"
                ));
            }
        });
    }

    private JSONObject errorPayload(String message, String code) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("ok", false);
            payload.put("errorCode", code);
            payload.put("error", message == null ? "No se pudo completar la operación." : message);
        } catch (Exception ignored) {
            // JSONObject construction with these primitive values cannot fail in practice.
        }
        return payload;
    }

    private String normalizeRequestId(String requestId) {
        return requestId != null && requestId.matches(REQUEST_ID_PATTERN)
                ? requestId
                : UUID.randomUUID().toString();
    }

    private void send(String requestId, JSONObject payload) {
        String script = "window.EtecsaNative && window.EtecsaNative.onResponse("
                + JSONObject.quote(requestId)
                + ","
                + payload
                + ");";
        webView.post(() -> webView.evaluateJavascript(script, null));
    }

    private interface BridgeTask {
        JSONObject run() throws Exception;
    }
}

