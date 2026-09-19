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

    @ColumnInfo(name = "recharged_this_cycle_cup")
    public Double rechargedThisCycleCup;

    @ColumnInfo(name = "remaining_recharge_cup")
    public Double remainingRechargeCup;

    @ColumnInfo(name = "limit_cup")
    public double limitCup;

    @ColumnInfo(name = "limit_date_iso")
    public String limitDateIso;

    @ColumnInfo(name = "recharge_available_date_iso")
    public String rechargeAvailableDateIso;

    @ColumnInfo(name = "limit_reached")
    public boolean limitReached;

    @ColumnInfo(name = "raw_response")
    public String rawResponse;

    @ColumnInfo(name = "captured_at")
    public long capturedAt;

    public RechargeStatusEntity(
            int subscriptionId,
            Double amountCup,
            String expirationDateIso,
            Double rechargedThisCycleCup,
            Double remainingRechargeCup,
            double limitCup,
            String limitDateIso,
            String rechargeAvailableDateIso,
            boolean limitReached,
            String rawResponse,
            long capturedAt
    ) {
        this.subscriptionId = subscriptionId;
        this.amountCup = amountCup;
        this.expirationDateIso = expirationDateIso;
        this.rechargedThisCycleCup = rechargedThisCycleCup;
        this.remainingRechargeCup = remainingRechargeCup;
        this.limitCup = limitCup;
        this.limitDateIso = limitDateIso;
        this.rechargeAvailableDateIso = rechargeAvailableDateIso;
        this.limitReached = limitReached;
        this.rawResponse = rawResponse;
        this.capturedAt = capturedAt;
    }
}
