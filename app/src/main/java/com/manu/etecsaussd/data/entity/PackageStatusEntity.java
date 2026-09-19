package com.manu.etecsaussd.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "package_statuses",
        indices = {@Index(value = {"subscription_id", "captured_at"})}
)
public class PackageStatusEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "subscription_id")
    public int subscriptionId;

    @ColumnInfo(name = "expiration_date_iso")
    public String expirationDateIso;

    @ColumnInfo(name = "raw_response")
    public String rawResponse;

    @ColumnInfo(name = "captured_at")
    public long capturedAt;

    public PackageStatusEntity(
            int subscriptionId,
            String expirationDateIso,
            String rawResponse,
            long capturedAt
    ) {
        this.subscriptionId = subscriptionId;
        this.expirationDateIso = expirationDateIso;
        this.rawResponse = rawResponse;
        this.capturedAt = capturedAt;
    }
}
