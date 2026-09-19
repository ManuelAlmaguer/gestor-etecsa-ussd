package com.manu.etecsaussd.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.manu.etecsaussd.data.entity.RechargeStatusEntity;

@Dao
public interface RechargeStatusDao {
    @Insert
    long insert(RechargeStatusEntity status);

    @Query("SELECT * FROM recharge_statuses ORDER BY captured_at DESC LIMIT 1")
    RechargeStatusEntity getLatest();

    @Query("SELECT * FROM recharge_statuses WHERE subscription_id = :subscriptionId ORDER BY captured_at DESC LIMIT 1")
    RechargeStatusEntity getLatestForSubscription(int subscriptionId);
}
