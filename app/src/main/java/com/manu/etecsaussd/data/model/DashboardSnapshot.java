package com.manu.etecsaussd.data.model;

import com.manu.etecsaussd.data.entity.BalanceSnapshotEntity;
import com.manu.etecsaussd.data.entity.DataUsageSnapshotEntity;
import com.manu.etecsaussd.data.entity.RechargeStatusEntity;
import com.manu.etecsaussd.data.entity.VoiceSmsSnapshotEntity;

public final class DashboardSnapshot {
    public final BalanceSnapshotEntity balance;
    public final DataUsageSnapshotEntity dataUsage;
    public final VoiceSmsSnapshotEntity voiceSms;
    public final RechargeStatusEntity rechargeStatus;

    public DashboardSnapshot(
            BalanceSnapshotEntity balance,
            DataUsageSnapshotEntity dataUsage,
            VoiceSmsSnapshotEntity voiceSms,
            RechargeStatusEntity rechargeStatus
    ) {
        this.balance = balance;
        this.dataUsage = dataUsage;
        this.voiceSms = voiceSms;
        this.rechargeStatus = rechargeStatus;
    }
}

