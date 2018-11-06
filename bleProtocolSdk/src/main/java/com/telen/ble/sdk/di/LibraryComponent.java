package com.telen.ble.sdk.di;

import android.content.Context;

import com.telen.ble.sdk.builder.HexBuilder;
import com.telen.ble.sdk.layers.DataLayerInterface;
import com.telen.ble.sdk.layers.impl.BleHardwareConnectionLayer;
import com.telen.ble.sdk.model.ResponseFrameFactory;
import com.telen.ble.sdk.validator.DataValidator;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        LibraryModule.class
})
public interface LibraryComponent {
    DataLayerInterface<BleHardwareConnectionLayer> provideDataLayer();
    ResponseFrameFactory provideResponseFrameFactory();
    DataValidator provideDataValidator();
    HexBuilder provideHexBuilder();
    Context provideContext();
}
