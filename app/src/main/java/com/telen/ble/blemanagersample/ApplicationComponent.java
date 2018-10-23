package com.telen.ble.blemanagersample;

import com.telen.ble.blemanagersample.devices.minger.Minger_P50;
import com.telen.ble.blemanagersample.devices.oc100.OC100;
import com.telen.ble.manager.di.LibraryComponent;

import dagger.Component;

@ApplicationScope
@Component(dependencies = { LibraryComponent.class }, modules = {
})
public interface ApplicationComponent {
    void inject(Minger_P50 target);
    void inject(OC100 target);
}
