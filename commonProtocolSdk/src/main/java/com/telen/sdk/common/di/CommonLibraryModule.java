package com.telen.sdk.common.di;

import android.content.Context;

import com.telen.sdk.common.builder.HexBuilder;
import com.telen.sdk.common.models.ResponseFrameFactory;
import com.telen.sdk.common.validator.DataValidator;

import dagger.Module;
import dagger.Provides;

@Module
public class CommonLibraryModule {

    public CommonLibraryModule(Context context) {
        this.context = context;
    }

    private Context context;

    @Provides
    @CommonScope
    public DataValidator provideDataValidator() {
        return new DataValidator();
    }

    @Provides
    @CommonScope
    public Context provideContext() {
        return context;
    }

    @Provides
    @CommonScope
    public HexBuilder provideHexBuilder() {
        return new HexBuilder();
    }

    @Provides
    @CommonScope
    public ResponseFrameFactory provideResponseFrameFactory() {
        return new ResponseFrameFactory();
    }
}
