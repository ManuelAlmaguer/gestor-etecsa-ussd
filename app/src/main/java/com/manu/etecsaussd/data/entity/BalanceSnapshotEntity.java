package com.manu.etecsaussd.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "balance_snapshots",
        indices = {@Index(value = "captured_at")}
)
public class BalanceSnapshotEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "subscription_id")
    public int subscriptionId;

    @ColumnInfo(name = "amount_cup")
    public double amountCup;

    @ColumnInfo(name = "raw_response")
    public String rawResponse;

    @ColumnInfo(name = "captured_at")
    public long capturedAt;

    public BalanceSnapshotEntity(int subscriptionId, double amountCup, String rawResponse, long capturedAt) {
        this.subscriptionId = subscriptionId;
        this.amountCup = amountCup;
        this.rawResponse = rawResponse;
        this.capturedAt = capturedAt;
    }
}

