package com.telen.sdk.demo;

import com.telen.sdk.demo.devices.minger.Minger_P50;
import com.telen.sdk.demo.devices.ribbon.LightRibbon;

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
