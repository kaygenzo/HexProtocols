package com.telen.ble.manager.di;

import android.content.Context;

import com.polidea.rxandroidble2.RxBleClient;
import com.telen.ble.manager.layers.impl.DataLayerImpl;
import com.telen.ble.manager.layers.impl.BleHardwareConnectionLayer;
import com.telen.ble.manager.layers.HardwareLayerInterface;
import com.telen.ble.manager.validator.DataValidator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LibraryModule {

    public LibraryModule(Context context) {
        this.context = context;
    }

    private Context context;

    @Singleton
    @Provides
    public DataLayerImpl provideDataLayer(HardwareLayerInterface hardwareLayer, DataValidator dataValidator) {
        return new DataLayerImpl(hardwareLayer, dataValidator);
    }

    @Singleton
    @Provides
    public HardwareLayerInterface provideHardwareLayer(RxBleClient client) {
        return new BleHardwareConnectionLayer(client);
    }

    @Singleton
    @Provides
    public DataValidator provideDataValidator() {
        return new DataValidator();
    }

    @Singleton
    @Provides
    public RxBleClient provideBleClient(Context context) {
        return RxBleClient.create(context);
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return context;
    }
}