package com.telen.ble.sdk.di;

import com.telen.ble.sdk.layers.impl.DataLayerImpl;
import com.telen.ble.sdk.model.ResponseFrameFactory;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        LibraryModule.class
})
public interface LibraryComponent {
    DataLayerImpl provideDataLayer();
    ResponseFrameFactory provideResponseFrameFactory();
}
