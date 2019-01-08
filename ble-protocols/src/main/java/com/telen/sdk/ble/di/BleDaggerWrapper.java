package com.telen.sdk.ble.di;

import android.content.Context;

import com.telen.sdk.common.di.CommonDaggerWrapper;

public class BleDaggerWrapper {

    private static BleLibraryComponent mComponent;

    public static BleLibraryComponent getComponent(Context context) {
        if (mComponent == null) {
            initComponent(context);
        }
        return mComponent;
    }

    private static void initComponent (Context context) {
        mComponent = DaggerBleLibraryComponent
                .builder()
                .commonLibraryComponent(CommonDaggerWrapper.getComponent(context))
                .build();
    }
}