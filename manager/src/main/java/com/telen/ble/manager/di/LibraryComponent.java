package com.telen.ble.manager.di;

import com.telen.ble.manager.devices.minger_p50.Minger_P50;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        LibraryModule.class
})
public interface LibraryComponent {
    void inject(Minger_P50 device);
}
