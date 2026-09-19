package com.manu.etecsaussd.telephony;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;

import com.manu.etecsaussd.data.AppPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Routes USSD requests through an explicitly selected subscription whenever possible.
 * The executor is intentionally synchronous at this layer so Worker and native UI code can
 * apply one consistent timeout/error policy around Android's callback API.
 */
public final class UssdExecutor {
    private static final long DEFAULT_TIMEOUT_SECONDS = 35L;

    private final Context context;
    private final SubscriptionManager subscriptionManager;
    private final AppPreferences preferences;
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());
    private final long timeoutSeconds;

    public UssdExecutor(Context context) {
        this(context, DEFAULT_TIMEOUT_SECONDS);
    }

    UssdExecutor(Context context, long timeoutSeconds) {
        this.context = context.getApplicationContext();
        this.subscriptionManager = (SubscriptionManager) this.context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        this.preferences = AppPreferences.from(this.context);
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public Integer getSelectedSubscriptionId() {
        return preferences.getActionSubscriptionId();
    }

    public Integer getActionSubscriptionId() {
        return preferences.getActionSubscriptionId();
    }

    public Integer getDisplaySubscriptionId() {
        return preferences.getDisplaySubscriptionId();
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public void setSelectedSubscriptionId(int subscriptionId) throws UssdExecutionException {
        setActionSubscriptionId(subscriptionId);
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public void setActionSubscriptionId(int subscriptionId) throws UssdExecutionException {
        validateActiveSubscription(subscriptionId);
        preferences.setActionSubscriptionId(subscriptionId);
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public void setDisplaySubscriptionId(int subscriptionId) throws UssdExecutionException {
        validateActiveSubscription(subscriptionId);
        preferences.setDisplaySubscriptionId(subscriptionId);
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public int resolveSubscriptionIdForUi(Integer preferredSubscriptionId) throws UssdExecutionException {
        return resolveSubscriptionId(preferredSubscriptionId, true);
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public int resolveSubscriptionIdForDisplay(Integer preferredSubscriptionId) throws UssdExecutionException {
        return resolveSubscriptionId(preferredSubscriptionId, false);
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private void validateActiveSubscription(int subscriptionId) throws UssdExecutionException {
        boolean active = false;
        for (SubscriptionInfo info : getActiveSubscriptions()) {
            if (info.getSubscriptionId() == subscriptionId) {
                active = true;
                break;
            }
        }
        if (!active) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.NO_ACTIVE_SUBSCRIPTION,
                    "La SIM seleccionada ya no está activa."
            );
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    @SuppressLint("MissingPermission")
    public List<SubscriptionInfo> getActiveSubscriptions() throws UssdExecutionException {
        if (subscriptionManager == null) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.UNSUPPORTED,
                    "SubscriptionManager no está disponible en este dispositivo."
            );
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.PERMISSION_DENIED,
                    "Se necesita permiso para leer las SIM activas."
            );
        }
        List<SubscriptionInfo> active = subscriptionManager.getActiveSubscriptionInfoList();
        if (active == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(active);
    }

    @RequiresPermission(allOf = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
    })
    @SuppressLint("MissingPermission")
    public UssdResponse executeBlocking(String ussdCode, Integer preferredSubscriptionId)
            throws UssdExecutionException {
        if (TextUtils.isEmpty(ussdCode) || !ussdCode.matches("\\*[0-9*]+#")) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.INVALID_REQUEST,
                    "El código USSD no tiene un formato válido."
            );
        }
        if (!hasRequiredPermissions()) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.PERMISSION_DENIED,
                    "Concede los permisos de teléfono para consultar ETECSA."
            );
        }

        int subscriptionId = resolveSubscriptionId(preferredSubscriptionId, true);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.UNSUPPORTED,
                    "TelephonyManager no está disponible en este dispositivo."
            );
        }

        TelephonyManager subscriptionTelephonyManager = telephonyManager.createForSubscriptionId(subscriptionId);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UssdResponse> responseRef = new AtomicReference<>();
        AtomicReference<UssdExecutionException> errorRef = new AtomicReference<>();

        try {
            subscriptionTelephonyManager.sendUssdRequest(
                    ussdCode,
                    new TelephonyManager.UssdResponseCallback() {
                        @Override
                        public void onReceiveUssdResponse(
                                TelephonyManager manager,
                                String request,
                                CharSequence response
                        ) {
                            responseRef.set(new UssdResponse(
                                    request,
                                    response == null ? "" : response.toString(),
                                    subscriptionId,
                                    System.currentTimeMillis()
                            ));
                            latch.countDown();
                        }

                        @Override
                        public void onReceiveUssdResponseFailed(
                                TelephonyManager manager,
                                String request,
                                int failureCode
                        ) {
                            errorRef.set(new UssdExecutionException(
                                    UssdExecutionException.Reason.FAILED,
                                    "ETECSA rechazó la solicitud USSD (código " + failureCode + ").",
                                    failureCode
                            ));
                            latch.countDown();
                        }
                    },
                    callbackHandler
            );
        } catch (SecurityException exception) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.PERMISSION_DENIED,
                    "El sistema no permitió enviar el código USSD.",
                    exception
            );
        } catch (UnsupportedOperationException exception) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.UNSUPPORTED,
                    "Este dispositivo no soporta solicitudes USSD desde la aplicación.",
                    exception
            );
        }

        try {
            if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new UssdExecutionException(
                        UssdExecutionException.Reason.TIMEOUT,
                        "ETECSA no respondió dentro del tiempo esperado."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.TIMEOUT,
                    "La consulta USSD fue interrumpida.",
                    exception
            );
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        if (responseRef.get() == null) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.FAILED,
                    "La respuesta USSD llegó vacía."
            );
        }
        return responseRef.get();
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private int resolveSubscriptionId(Integer preferredSubscriptionId, boolean includeActionPreference)
            throws UssdExecutionException {
        List<SubscriptionInfo> activeSubscriptions = getActiveSubscriptions();
        if (activeSubscriptions.isEmpty()) {
            throw new UssdExecutionException(
                    UssdExecutionException.Reason.NO_ACTIVE_SUBSCRIPTION,
                    "No se encontró una SIM activa para ETECSA."
            );
        }

        if (preferredSubscriptionId != null && containsSubscription(activeSubscriptions, preferredSubscriptionId)) {
            return preferredSubscriptionId;
        }

        if (includeActionPreference) {
            Integer storedSubscriptionId = getSelectedSubscriptionId();
            if (storedSubscriptionId != null && containsSubscription(activeSubscriptions, storedSubscriptionId)) {
                return storedSubscriptionId;
            }
        }

        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (containsSubscription(activeSubscriptions, defaultDataSubscriptionId)) {
            return defaultDataSubscriptionId;
        }
        return activeSubscriptions.get(0).getSubscriptionId();
    }

    private boolean containsSubscription(List<SubscriptionInfo> subscriptions, int subscriptionId) {
        for (SubscriptionInfo info : subscriptions) {
            if (info.getSubscriptionId() == subscriptionId) {
                return true;
            }
        }
        return false;
    }
}
