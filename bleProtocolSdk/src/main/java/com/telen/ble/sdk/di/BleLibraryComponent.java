package com.telen.ble.sdk.di;

import com.telen.ble.sdk.layers.impl.BleHardwareConnectionLayer;
import com.telen.sdk.common.di.CommonLibraryComponent;
import com.telen.sdk.common.layers.DataLayerInterface;

import dagger.Component;

@Component(dependencies = {
        CommonLibraryComponent.class
}, modules = {
        BleLibraryModule.class
})
@BleScope
public interface BleLibraryComponent {
    DataLayerInterface<BleHardwareConnectionLayer> provideDataLayer();
}
