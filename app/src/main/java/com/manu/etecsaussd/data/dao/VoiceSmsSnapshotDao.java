package com.manu.etecsaussd.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.manu.etecsaussd.data.entity.VoiceSmsSnapshotEntity;

@Dao
public interface VoiceSmsSnapshotDao {
    @Insert
    long insert(VoiceSmsSnapshotEntity snapshot);

    @Query("SELECT * FROM voice_sms_snapshots ORDER BY captured_at DESC LIMIT 1")
    VoiceSmsSnapshotEntity getLatest();
}

