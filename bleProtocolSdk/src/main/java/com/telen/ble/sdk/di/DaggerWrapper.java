package com.telen.ble.sdk.di;

import android.content.Context;

public class DaggerWrapper {

    private static LibraryComponent mComponent;

    public static LibraryComponent getComponent(Context context) {
        if (mComponent == null) {
            initComponent(context);
        }
        return mComponent;
    }

    private static void initComponent (Context context) {
        mComponent = DaggerLibraryComponent
                .builder()
                .libraryModule(new LibraryModule(context))
                .build();
    }
}