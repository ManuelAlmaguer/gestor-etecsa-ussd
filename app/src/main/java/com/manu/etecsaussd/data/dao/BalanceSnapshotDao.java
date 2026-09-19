package com.manu.etecsaussd.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.manu.etecsaussd.data.entity.BalanceSnapshotEntity;

@Dao
public interface BalanceSnapshotDao {
    @Insert
    long insert(BalanceSnapshotEntity snapshot);

    @Query("SELECT * FROM balance_snapshots ORDER BY captured_at DESC LIMIT 1")
    BalanceSnapshotEntity getLatest();
}

