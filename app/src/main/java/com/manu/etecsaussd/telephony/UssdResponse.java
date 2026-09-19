package com.manu.etecsaussd.telephony;

public final class UssdResponse {
    public final String request;
    public final String response;
    public final int subscriptionId;
    public final long receivedAt;

    public UssdResponse(String request, String response, int subscriptionId, long receivedAt) {
        this.request = request;
        this.response = response;
        this.subscriptionId = subscriptionId;
        this.receivedAt = receivedAt;
    }
}

