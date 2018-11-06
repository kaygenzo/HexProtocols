package com.telen.sdk.demo;

import android.content.Context;

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
                .applicationModule(new ApplicationModule(context))
                .build();
    }
}