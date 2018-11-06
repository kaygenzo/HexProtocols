package com.telen.sdk.common.di;

import android.content.Context;

public class CommonDaggerWrapper {

    private static CommonLibraryComponent mComponent;

    public static CommonLibraryComponent getComponent(Context context) {
        if (mComponent == null) {
            initComponent(context);
        }
        return mComponent;
    }

    private static void initComponent (Context context) {
        mComponent = DaggerCommonLibraryComponent
                .builder()
                .commonLibraryModule(new CommonLibraryModule(context))
                .build();
    }
}