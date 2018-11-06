package com.telen.ble.blemanagersample;

import com.telen.ble.blemanagersample.devices.minger.Minger_P50;
import com.telen.ble.blemanagersample.devices.ribbon.LightRibbon;

import dagger.Component;

@Component(
        modules = {
        ApplicationModule.class
})
@ApplicationScope
public interface ApplicationComponent {
    void inject(Minger_P50 target);
    void inject(LightRibbon target);
}
