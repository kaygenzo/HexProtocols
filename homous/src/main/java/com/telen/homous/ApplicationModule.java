package com.telen.homous;

import android.content.Context;

import com.telen.sdk.demo.commonui.FirestoreManager;
import com.telen.sdk.demo.commonui.di.DaggerCommonUiWrapper;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private Context context;

    public ApplicationModule(Context context) {
        this.context = context;
    }

    @Provides
    @ApplicationScope
    public Context provideContext() {
        return context;
    }

    @Provides
    @ApplicationScope
    public FirestoreManager provideFirestoreManager() {
        return DaggerCommonUiWrapper.getComponent(context).provideFirestoreManager();
    }
}
