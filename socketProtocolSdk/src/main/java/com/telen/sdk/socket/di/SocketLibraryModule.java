package com.telen.sdk.socket.di;

import android.content.Context;

import com.telen.sdk.common.builder.HexBuilder;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.common.layers.impl.DataLayerImpl;
import com.telen.sdk.common.validator.DataValidator;
import com.telen.sdk.socket.layers.SocketHardwareConnectionLayer;
import com.telen.sdk.socket.utils.TCPSocketManager;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
public class SocketLibraryModule {

    @Provides
    @SocketScope
    public DataLayerInterface<SocketHardwareConnectionLayer> provideSocketDataLayer(SocketHardwareConnectionLayer hardwareLayer, DataValidator dataValidator, HexBuilder hexBuilder) {
        return new DataLayerImpl<>(hardwareLayer, dataValidator, hexBuilder);
    }

    @Provides
    @SocketScope
    public SocketHardwareConnectionLayer provideSocketHardwareLayer(Context context, OkHttpClient okHttpClient, TCPSocketManager tcpSocketManager) {
        return new SocketHardwareConnectionLayer(context, okHttpClient, tcpSocketManager);
    }

    @Provides
    @SocketScope
    public OkHttpClient provideHttpClient() {
        return new OkHttpClient();
    }

    @Provides
    @SocketScope
    public TCPSocketManager provideTcpSocketManager() {
        return new TCPSocketManager();
    }
}
