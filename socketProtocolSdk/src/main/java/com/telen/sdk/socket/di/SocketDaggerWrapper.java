package com.telen.sdk.socket.di;

import android.content.Context;

import com.telen.sdk.common.di.CommonDaggerWrapper;

public class SocketDaggerWrapper {

    private static SocketLibraryComponent mComponent;

    public static SocketLibraryComponent getComponent(Context context) {
        if (mComponent == null) {
            initComponent(context);
        }
        return mComponent;
    }

    private static void initComponent (Context context) {
        mComponent = DaggerSocketLibraryComponent
                .builder()
                .commonLibraryComponent(CommonDaggerWrapper.getComponent(context))
                .build();
    }
}