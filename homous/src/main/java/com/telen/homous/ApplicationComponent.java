package com.telen.homous;

import dagger.Component;

@Component(
        modules = {
        ApplicationModule.class
})
@ApplicationScope
public interface ApplicationComponent {
    void inject(MainActivity target);
}
