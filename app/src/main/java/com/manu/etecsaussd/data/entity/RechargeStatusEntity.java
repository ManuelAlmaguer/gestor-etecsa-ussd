package com.manu.etecsaussd.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recharge_statuses",
        indices = {@Index(value = "captured_at")}
)
public class RechargeStatusEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "subscription_id")
    public int subscriptionId;

    @ColumnInfo(name = "amount_cup")
    public Double amountCup;

    @ColumnInfo(name = "expiration_date_iso")
    public String expirationDateIso;

    @ColumnInfo(name = "raw_response")
    public String rawResponse;

    @ColumnInfo(name = "captured_at")
    public long capturedAt;

    public RechargeStatusEntity(
            int subscriptionId,
            Double amountCup,
            String expirationDateIso,
            String rawResponse,
            long capturedAt
    ) {
        this.subscriptionId = subscriptionId;
        this.amountCup = amountCup;
        this.expirationDateIso = expirationDateIso;
        this.rawResponse = rawResponse;
        this.capturedAt = capturedAt;
    }
}

