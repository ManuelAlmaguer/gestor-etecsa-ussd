package com.manu.etecsaussd.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.manu.etecsaussd.data.dao.BalanceSnapshotDao;
import com.manu.etecsaussd.data.dao.DataUsageSnapshotDao;
import com.manu.etecsaussd.data.dao.RechargeStatusDao;
import com.manu.etecsaussd.data.dao.VoiceSmsSnapshotDao;
import com.manu.etecsaussd.data.entity.BalanceSnapshotEntity;
import com.manu.etecsaussd.data.entity.DataUsageSnapshotEntity;
import com.manu.etecsaussd.data.entity.RechargeStatusEntity;
import com.manu.etecsaussd.data.entity.VoiceSmsSnapshotEntity;

@Database(
        entities = {
                BalanceSnapshotEntity.class,
                DataUsageSnapshotEntity.class,
                VoiceSmsSnapshotEntity.class,
                RechargeStatusEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class EtecsaDatabase extends RoomDatabase {
    private static volatile EtecsaDatabase instance;

    public abstract BalanceSnapshotDao balanceSnapshotDao();

    public abstract DataUsageSnapshotDao dataUsageSnapshotDao();

    public abstract VoiceSmsSnapshotDao voiceSmsSnapshotDao();

    public abstract RechargeStatusDao rechargeStatusDao();

    public static EtecsaDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (EtecsaDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    EtecsaDatabase.class,
                                    "etecsa_history.db"
                            )
                            .build();
                }
            }
        }
        return instance;
    }
}

