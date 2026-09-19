package com.manu.etecsaussd.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.manu.etecsaussd.data.entity.PackageStatusEntity;

@Dao
public interface PackageStatusDao {
    @Insert
    long insert(PackageStatusEntity status);

    @Query("SELECT * FROM package_statuses WHERE subscription_id = :subscriptionId ORDER BY captured_at DESC LIMIT 1")
    PackageStatusEntity getLatestForSubscription(int subscriptionId);
}
