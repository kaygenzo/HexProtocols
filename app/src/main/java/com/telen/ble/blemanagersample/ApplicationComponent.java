package com.telen.ble.blemanagersample;

import com.telen.ble.blemanagersample.devices.minger.Minger_P50;
import com.telen.ble.blemanagersample.devices.ribbon.LightRibbon;
import com.telen.ble.sdk.di.LibraryComponent;

import dagger.Component;

@ApplicationScope
@Component(dependencies = { LibraryComponent.class }, modules = {
        ApplicationModule.class
})
public interface ApplicationComponent {
    void inject(Minger_P50 target);
    void inject(LightRibbon target);
}
