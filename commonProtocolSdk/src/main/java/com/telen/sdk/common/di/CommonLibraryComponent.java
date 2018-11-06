package com.telen.sdk.common.di;

import android.content.Context;

import com.telen.sdk.common.builder.HexBuilder;
import com.telen.sdk.common.models.ResponseFrameFactory;
import com.telen.sdk.common.validator.DataValidator;

import dagger.Component;

@Component(modules = {
        CommonLibraryModule.class
})
@CommonScope
public interface CommonLibraryComponent {
    ResponseFrameFactory provideResponseFrameFactory();
    DataValidator provideDataValidator();
    HexBuilder provideHexBuilder();
    Context provideContext();
}
