package com.telen.ble.blemanagersample;

import android.content.Context;

import com.telen.ble.blemanagersample.pending.SocketHardwareConnectionLayer;
import com.telen.ble.blemanagersample.pending.TCPSocketManager;
import com.telen.ble.sdk.builder.HexBuilder;
import com.telen.ble.sdk.layers.DataLayerInterface;
import com.telen.ble.sdk.layers.impl.DataLayerImpl;
import com.telen.ble.sdk.validator.DataValidator;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
public class ApplicationModule {

    @Provides
    public DataLayerInterface<SocketHardwareConnectionLayer> provideSocketDataLayer(SocketHardwareConnectionLayer hardwareLayer, DataValidator dataValidator, HexBuilder hexBuilder) {
        return new DataLayerImpl<>(hardwareLayer, dataValidator, hexBuilder);
    }

    @Provides
    public SocketHardwareConnectionLayer provideSocketHardwareLayer(Context context, OkHttpClient okHttpClient, TCPSocketManager tcpSocketManager) {
        return new SocketHardwareConnectionLayer(context, okHttpClient, tcpSocketManager);
    }

    @Provides
    public OkHttpClient provideHttpClient() {
        return new OkHttpClient();
    }

    @Provides
    public TCPSocketManager provideTcpSocketManager() {
        return new TCPSocketManager();
    }
}
