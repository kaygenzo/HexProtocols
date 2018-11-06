package com.telen.ble.sdk.di;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.polidea.rxandroidble2.RxBleClient;
import com.telen.ble.sdk.builder.HexBuilder;
import com.telen.ble.sdk.layers.DataLayerInterface;
import com.telen.ble.sdk.layers.impl.BleHardwareConnectionLayer;
import com.telen.ble.sdk.layers.impl.DataLayerImpl;
import com.telen.ble.sdk.model.ResponseFrameFactory;
import com.telen.ble.sdk.validator.DataValidator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LibraryModule {

    public LibraryModule(Context context) {
        this.context = context;
    }

    private Context context;

    @Singleton
    @Provides
    public DataLayerInterface<BleHardwareConnectionLayer> provideBleDataLayer(BleHardwareConnectionLayer hardwareLayer, DataValidator dataValidator, HexBuilder hexBuilder) {
        return new DataLayerImpl<>(hardwareLayer, dataValidator, hexBuilder);
    }

    @Singleton
    @Provides
    public BleHardwareConnectionLayer provideHardwareLayer(RxBleClient client, BluetoothAdapter bluetoothAdapter, Context context) {
        return new BleHardwareConnectionLayer(client, bluetoothAdapter, context);
    }

    @Singleton
    @Provides
    public DataValidator provideDataValidator() {
        return new DataValidator();
    }

    @Singleton
    @Provides
    public RxBleClient provideBleClient(Context context) {
        return RxBleClient.create(context);
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return context;
    }

    @Provides
    @Singleton
    public BluetoothAdapter provideBluetoothAdapter(Context context) {
        BluetoothManager manager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        if(manager!=null)
            return manager.getAdapter();
        else
            return null;
    }

    @Provides
    @Singleton
    public HexBuilder provideHexBuilder() {
        return new HexBuilder();
    }

    @Provides
    @Singleton
    public ResponseFrameFactory provideResponseFrameFactory() {
        return new ResponseFrameFactory();
    }
}
