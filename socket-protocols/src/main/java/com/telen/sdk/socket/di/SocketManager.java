package com.telen.sdk.socket.di;

import android.content.Context;

import com.telen.sdk.common.di.CommonProtocolsManager;

public class SocketManager {

    private static SocketLibraryComponent mComponent;

    public static SocketLibraryComponent getInstance(Context context) {
        if (mComponent == null) {
            initComponent(context);
        }
        return mComponent;
    }

    private static void initComponent (Context context) {
        mComponent = DaggerSocketLibraryComponent
                .builder()
                .commonLibraryComponent(CommonProtocolsManager.getInstance(context))
                .build();
    }
}