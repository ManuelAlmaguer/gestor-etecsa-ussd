package com.manu.etecsaussd;

import android.app.Application;

import com.manu.etecsaussd.data.EtecsaDatabase;
import com.manu.etecsaussd.domain.EtecsaRepository;
import com.manu.etecsaussd.telephony.UssdExecutor;

public final class EtecsaApplication extends Application {
    private EtecsaDatabase database;
    private UssdExecutor ussdExecutor;
    private EtecsaRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        database = EtecsaDatabase.getInstance(this);
        ussdExecutor = new UssdExecutor(this);
        repository = new EtecsaRepository(database);
    }

    public EtecsaDatabase getDatabase() {
        return database;
    }

    public UssdExecutor getUssdExecutor() {
        return ussdExecutor;
    }

    public EtecsaRepository getRepository() {
        return repository;
    }
}

