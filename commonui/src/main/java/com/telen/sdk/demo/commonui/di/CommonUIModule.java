package com.telen.sdk.demo.commonui.di;

import android.content.Context;

import com.telen.sdk.ble.di.BleDaggerWrapper;
import com.telen.sdk.ble.layers.impl.BleHardwareConnectionLayer;
import com.telen.sdk.common.di.CommonDaggerWrapper;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.common.models.ResponseFrameFactory;
import com.telen.sdk.demo.commonui.FirestoreManager;
import com.telen.sdk.demo.commonui.devices.minger.MingerManager;
import com.telen.sdk.demo.commonui.devices.minger.Minger_P50;
import com.telen.sdk.socket.di.SocketDaggerWrapper;
import com.telen.sdk.socket.layers.SocketHardwareConnectionLayer;
import com.telen.sdk.socket.utils.NetworkUtils;

import dagger.Module;
import dagger.Provides;

@Module
public class CommonUIModule {

    private Context context;

    public CommonUIModule(Context context) {
        this.context = context;
    }

    @Provides
    @CommonUIScope
    public DataLayerInterface<BleHardwareConnectionLayer> provideBleDataLayer(Context context) {
        return BleDaggerWrapper.getComponent(context).provideDataLayer();
    }

    @Provides
    @CommonUIScope
    public DataLayerInterface<SocketHardwareConnectionLayer> provideSocketDataLayer(Context context) {
        return SocketDaggerWrapper.getComponent(context).provideSocketDataLayer();
    }

    @Provides
    @CommonUIScope
    public Context provideContext() {
        return context;
    }

    @Provides
    @CommonUIScope
    public NetworkUtils provideNetworkUtils() {
        return SocketDaggerWrapper.getComponent(context).provideNetworkUtils();
    }

    @Provides
    @CommonUIScope
    public FirestoreManager provideFirestoreManager() {
        return new FirestoreManager();
    }

    @Provides
    @CommonUIScope
    public ResponseFrameFactory provideResponseFrameFactory() {
        return CommonDaggerWrapper.getComponent(context).provideResponseFrameFactory();
    }

    @Provides
    @CommonUIScope
    public MingerManager provideMingerManager(Context context, FirestoreManager firestoreManager, Minger_P50 mingerDevice) {
        return new MingerManager(context, firestoreManager, mingerDevice);
    }

    @Provides
    @CommonUIScope
    public Minger_P50 provideMingerDevice(Context context) {
        return new Minger_P50(context);
    }
}
