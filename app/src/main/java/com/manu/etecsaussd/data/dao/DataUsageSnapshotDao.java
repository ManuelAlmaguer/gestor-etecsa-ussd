package com.manu.etecsaussd.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.manu.etecsaussd.data.entity.DataUsageSnapshotEntity;

@Dao
public interface DataUsageSnapshotDao {
    @Insert
    long insert(DataUsageSnapshotEntity snapshot);

    @Query("SELECT * FROM data_usage_snapshots ORDER BY captured_at DESC LIMIT 1")
    DataUsageSnapshotEntity getLatest();

    @Query("SELECT * FROM data_usage_snapshots WHERE subscription_id = :subscriptionId ORDER BY captured_at DESC LIMIT 1")
    DataUsageSnapshotEntity getLatestForSubscription(int subscriptionId);
}
