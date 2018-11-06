package com.telen.sdk.blemanagersample;

import android.content.Context;

import com.telen.sdk.ble.di.BleDaggerWrapper;
import com.telen.sdk.ble.layers.impl.BleHardwareConnectionLayer;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.socket.di.SocketDaggerWrapper;
import com.telen.sdk.socket.layers.SocketHardwareConnectionLayer;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private Context context;

    public ApplicationModule(Context context) {
        this.context = context;
    }

    @Provides
    @ApplicationScope
    public DataLayerInterface<BleHardwareConnectionLayer> provideBleDataLayer(Context context) {
        return BleDaggerWrapper.getComponent(context).provideDataLayer();
    }

    @Provides
    @ApplicationScope
    public DataLayerInterface<SocketHardwareConnectionLayer> provideSocketDataLayer(Context context) {
        return SocketDaggerWrapper.getComponent(context).provideSocketDataLayer();
    }

    @Provides
    @ApplicationScope
    public Context provideContext() {
        return context;
    }
}
