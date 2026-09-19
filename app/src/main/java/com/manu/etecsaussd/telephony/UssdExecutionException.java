package com.manu.etecsaussd.telephony;

public class UssdExecutionException extends Exception {
    public enum Reason {
        INVALID_REQUEST,
        PERMISSION_DENIED,
        NO_ACTIVE_SUBSCRIPTION,
        UNSUPPORTED,
        TIMEOUT,
        FAILED
    }

    private final Reason reason;
    private final int failureCode;

    public UssdExecutionException(Reason reason, String message) {
        this(reason, message, -1, null);
    }

    public UssdExecutionException(Reason reason, String message, Throwable cause) {
        this(reason, message, -1, cause);
    }

    public UssdExecutionException(Reason reason, String message, int failureCode) {
        this(reason, message, failureCode, null);
    }

    private UssdExecutionException(Reason reason, String message, int failureCode, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.failureCode = failureCode;
    }

    public Reason getReason() {
        return reason;
    }

    public int getFailureCode() {
        return failureCode;
    }

    public boolean isRetryable() {
        return reason == Reason.TIMEOUT
                || reason == Reason.FAILED
                || reason == Reason.NO_ACTIVE_SUBSCRIPTION;
    }
}

