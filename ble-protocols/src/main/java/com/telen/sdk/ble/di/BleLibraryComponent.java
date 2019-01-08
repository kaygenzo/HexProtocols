package com.telen.sdk.ble.di;

import com.telen.sdk.ble.layers.impl.BleHardwareConnectionLayer;
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
