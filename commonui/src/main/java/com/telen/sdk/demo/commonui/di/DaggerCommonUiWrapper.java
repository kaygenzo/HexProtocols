package com.telen.sdk.demo.commonui.di;

import android.content.Context;

public class DaggerCommonUiWrapper {

    private static CommonUiComponent mComponent;

    public static CommonUiComponent getComponent(Context context) {
        if (mComponent == null) {
            initComponent(context);
        }
        return mComponent;
    }

    private static void initComponent (Context context) {

        mComponent = DaggerCommonUiComponent
                .builder()
                .commonUIModule(new CommonUIModule(context))
                .build();
    }
}