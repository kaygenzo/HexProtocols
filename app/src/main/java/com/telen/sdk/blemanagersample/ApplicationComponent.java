package com.telen.sdk.blemanagersample;

import com.telen.sdk.blemanagersample.devices.minger.Minger_P50;
import com.telen.sdk.blemanagersample.devices.ribbon.LightRibbon;

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
