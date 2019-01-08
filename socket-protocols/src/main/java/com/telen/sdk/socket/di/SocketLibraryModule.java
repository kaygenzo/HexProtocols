package com.telen.sdk.socket.di;

import android.content.Context;

import com.telen.sdk.common.builder.CommandBuilder;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.common.layers.impl.DataLayerImpl;
import com.telen.sdk.common.validator.DataValidator;
import com.telen.sdk.socket.layers.SocketHardwareConnectionLayer;
import com.telen.sdk.socket.utils.NetworkUtils;
import com.telen.sdk.socket.utils.TCPSocketManager;
import com.telen.sdk.socket.utils.UDPSocketManager;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
public class SocketLibraryModule {

    @Provides
    @SocketScope
    public DataLayerInterface<SocketHardwareConnectionLayer> provideSocketDataLayer(SocketHardwareConnectionLayer hardwareLayer, DataValidator dataValidator, CommandBuilder commandBuilder) {
        return new DataLayerImpl<>(hardwareLayer, dataValidator, commandBuilder);
    }

    @Provides
    @SocketScope
    public SocketHardwareConnectionLayer provideSocketHardwareLayer(Context context, OkHttpClient okHttpClient, TCPSocketManager tcpSocketManager, UDPSocketManager udpSocketManager) {
        return new SocketHardwareConnectionLayer(context, okHttpClient, tcpSocketManager, udpSocketManager);
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

    @Provides
    @SocketScope
    public UDPSocketManager provideUdpSocketManager(NetworkUtils networkUtils) {
        return new UDPSocketManager(networkUtils);
    }

    @Provides
    @SocketScope
    public NetworkUtils provideNetworkUtils() {
        return new NetworkUtils();
    }
}
