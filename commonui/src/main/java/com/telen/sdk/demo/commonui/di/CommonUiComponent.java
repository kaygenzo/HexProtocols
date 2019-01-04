package com.telen.sdk.demo.commonui.di;

import com.telen.sdk.demo.commonui.FirestoreManager;
import com.telen.sdk.demo.commonui.devices.minger.CardViewMingerP50;
import com.telen.sdk.demo.commonui.devices.minger.Minger_P50;
import com.telen.sdk.demo.commonui.devices.ribbon.CardViewLightRibbon;
import com.telen.sdk.demo.commonui.devices.ribbon.LightRibbon;

import dagger.Component;

@Component(
        modules = {
        CommonUIModule.class
})
@CommonUIScope
public interface CommonUiComponent {
    void inject(CardViewMingerP50 target);
    void inject(CardViewLightRibbon target);
    void inject(Minger_P50 target);
    void inject(LightRibbon target);
    FirestoreManager provideFirestoreManager();
}
