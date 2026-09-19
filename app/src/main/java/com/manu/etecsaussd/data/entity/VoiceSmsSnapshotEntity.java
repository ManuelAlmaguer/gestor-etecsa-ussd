package com.manu.etecsaussd.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "voice_sms_snapshots",
        indices = {@Index(value = "captured_at")}
)
public class VoiceSmsSnapshotEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "subscription_id")
    public int subscriptionId;

    @ColumnInfo(name = "voice_minutes")
    public Long voiceMinutes;

    @ColumnInfo(name = "voice_seconds")
    public Long voiceSeconds;

    @ColumnInfo(name = "sms_messages")
    public Long smsMessages;

    @ColumnInfo(name = "raw_response")
    public String rawResponse;

    @ColumnInfo(name = "captured_at")
    public long capturedAt;

    public VoiceSmsSnapshotEntity(
            int subscriptionId,
            Long voiceMinutes,
            Long voiceSeconds,
            Long smsMessages,
            String rawResponse,
            long capturedAt
    ) {
        this.subscriptionId = subscriptionId;
        this.voiceMinutes = voiceMinutes;
        this.voiceSeconds = voiceSeconds;
        this.smsMessages = smsMessages;
        this.rawResponse = rawResponse;
        this.capturedAt = capturedAt;
    }
}
