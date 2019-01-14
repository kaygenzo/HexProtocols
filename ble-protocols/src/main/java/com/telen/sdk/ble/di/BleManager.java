package com.telen.sdk.ble.di;

import android.content.Context;

import com.telen.sdk.common.di.CommonProtocolsManager;

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
    }
}