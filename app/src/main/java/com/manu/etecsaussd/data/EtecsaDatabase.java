package com.manu.etecsaussd.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.manu.etecsaussd.data.dao.BalanceSnapshotDao;
import com.manu.etecsaussd.data.dao.DataUsageSnapshotDao;
import com.manu.etecsaussd.data.dao.RechargeStatusDao;
import com.manu.etecsaussd.data.dao.VoiceSmsSnapshotDao;
import com.manu.etecsaussd.data.dao.PackageStatusDao;
import com.manu.etecsaussd.data.entity.BalanceSnapshotEntity;
import com.manu.etecsaussd.data.entity.DataUsageSnapshotEntity;
import com.manu.etecsaussd.data.entity.RechargeStatusEntity;
import com.manu.etecsaussd.data.entity.VoiceSmsSnapshotEntity;
import com.manu.etecsaussd.data.entity.PackageStatusEntity;

@Database(
        entities = {
                BalanceSnapshotEntity.class,
                DataUsageSnapshotEntity.class,
                VoiceSmsSnapshotEntity.class,
                RechargeStatusEntity.class,
                PackageStatusEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class EtecsaDatabase extends RoomDatabase {
    private static volatile EtecsaDatabase instance;

    public abstract BalanceSnapshotDao balanceSnapshotDao();

    public abstract DataUsageSnapshotDao dataUsageSnapshotDao();

    public abstract VoiceSmsSnapshotDao voiceSmsSnapshotDao();

    public abstract RechargeStatusDao rechargeStatusDao();

    public abstract PackageStatusDao packageStatusDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE balance_snapshots ADD COLUMN line_active_until_iso TEXT");
            database.execSQL("ALTER TABLE balance_snapshots ADD COLUMN package_expiration_iso TEXT");
            database.execSQL("ALTER TABLE voice_sms_snapshots ADD COLUMN voice_seconds INTEGER");
            database.execSQL("ALTER TABLE recharge_statuses ADD COLUMN recharged_this_cycle_cup REAL");
            database.execSQL("ALTER TABLE recharge_statuses ADD COLUMN remaining_recharge_cup REAL");
            database.execSQL("ALTER TABLE recharge_statuses ADD COLUMN limit_cup REAL NOT NULL DEFAULT 360.0");
            database.execSQL("ALTER TABLE recharge_statuses ADD COLUMN limit_date_iso TEXT");
            database.execSQL("ALTER TABLE recharge_statuses ADD COLUMN recharge_available_date_iso TEXT");
            database.execSQL("ALTER TABLE recharge_statuses ADD COLUMN limit_reached INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS package_statuses ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "subscription_id INTEGER NOT NULL, "
                            + "expiration_date_iso TEXT, "
                            + "raw_response TEXT, "
                            + "captured_at INTEGER NOT NULL)"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_package_statuses_subscription_id_captured_at "
                            + "ON package_statuses(subscription_id, captured_at)"
            );
        }
    };

    public static EtecsaDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (EtecsaDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                            EtecsaDatabase.class,
                                    "etecsa_history.db"
                            )
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
