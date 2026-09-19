package com.manu.etecsaussd.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "data_usage_snapshots",
        indices = {@Index(value = "captured_at")}
)
public class DataUsageSnapshotEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "subscription_id")
    public int subscriptionId;

    @ColumnInfo(name = "lte_megabytes")
    public Long lteMegabytes;

    @ColumnInfo(name = "all_networks_megabytes")
    public Long allNetworksMegabytes;

    @ColumnInfo(name = "total_megabytes")
    public Long totalMegabytes;

    @ColumnInfo(name = "raw_response")
    public String rawResponse;

    @ColumnInfo(name = "captured_at")
    public long capturedAt;

    public DataUsageSnapshotEntity(
            int subscriptionId,
            Long lteMegabytes,
            Long allNetworksMegabytes,
            Long totalMegabytes,
            String rawResponse,
            long capturedAt
    ) {
        this.subscriptionId = subscriptionId;
        this.lteMegabytes = lteMegabytes;
        this.allNetworksMegabytes = allNetworksMegabytes;
        this.totalMegabytes = totalMegabytes;
        this.rawResponse = rawResponse;
        this.capturedAt = capturedAt;
    }
}

