package com.telen.sdk.socket.di;

import com.telen.sdk.common.di.CommonLibraryComponent;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.socket.layers.SocketHardwareConnectionLayer;
import com.telen.sdk.socket.utils.NetworkUtils;

import dagger.Component;

@Component(dependencies = {
        CommonLibraryComponent.class
}, modules = {
        SocketLibraryModule.class
})
@SocketScope
public interface SocketLibraryComponent {
    DataLayerInterface<SocketHardwareConnectionLayer> provideSocketDataLayer();
    NetworkUtils provideNetworkUtils();
}