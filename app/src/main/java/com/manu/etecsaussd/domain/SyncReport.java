package com.manu.etecsaussd.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SyncReport {
    public final int subscriptionId;
    public final int successCount;
    public final int failureCount;
    public final boolean hasRetryableFailure;
    public final List<String> errors;

    public SyncReport(
            int subscriptionId,
            int successCount,
            int failureCount,
            boolean hasRetryableFailure,
            List<String> errors
    ) {
        this.subscriptionId = subscriptionId;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.hasRetryableFailure = hasRetryableFailure;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public boolean isComplete() {
        return successCount > 0 && failureCount == 0;
    }
}

