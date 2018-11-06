package com.telen.ble.blemanagersample;

import android.content.Context;

import com.telen.ble.sdk.di.DaggerWrapper;

public class DaggerApplicationWrapper {

    private static ApplicationComponent mComponent;

    public static ApplicationComponent getComponent(Context context) {
        if (mComponent == null) {
            initComponent(context);
        }
        return mComponent;
    }

    private static void initComponent (Context context) {
        mComponent = DaggerApplicationComponent
                .builder()
                .libraryComponent(DaggerWrapper.getComponent(context))
                .applicationModule(new ApplicationModule())
                .build();
    }
}