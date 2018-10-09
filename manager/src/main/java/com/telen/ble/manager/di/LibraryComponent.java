package com.telen.ble.manager.di;

import com.telen.ble.manager.layers.impl.DataLayerImpl;
import com.telen.ble.manager.model.ResponseFrameFactory;

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
