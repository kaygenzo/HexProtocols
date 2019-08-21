package com.telen.sdk.ble.di;

import android.content.Context;

import com.polidea.rxandroidble2.exceptions.BleException;
import com.telen.sdk.common.di.CommonProtocolsManager;

import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;

public class BleManager {

    private static BleLibraryComponent mComponent;

    public static BleLibraryComponent getInstance(Context context) {
        if (mComponent == null) {
            initComponent(context);
        }
        return mComponent;
    }

    private static void initComponent (Context context) {
        mComponent = DaggerBleLibraryComponent
                .builder()
                .commonLibraryComponent(CommonProtocolsManager.getInstance(context))
                .build();

        RxJavaPlugins.setErrorHandler(throwable -> {
            if (throwable instanceof UndeliverableException && throwable.getCause() instanceof BleException) {
                return; // ignore BleExceptions as they were surely delivered at least once
            }
            // add other custom handlers if needed
            throw new RuntimeException("Unexpected Throwable in RxJavaPlugins error handler", throwable);
        });
    }
}