package com.telen.sdk.ble.di;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble2.RxBleClient;
import com.telen.sdk.ble.layers.impl.BleHardwareConnectionLayer;
import com.telen.sdk.common.builder.CommandBuilder;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.common.layers.impl.DataLayerImpl;
import com.telen.sdk.common.validator.DataValidator;

import dagger.Module;
import dagger.Provides;

@Module
public class BleLibraryModule {

    @Provides
    @BleScope
    public DataLayerInterface<BleHardwareConnectionLayer> provideBleDataLayer(BleHardwareConnectionLayer hardwareLayer, DataValidator dataValidator, CommandBuilder commandBuilder) {
        return new DataLayerImpl<>(hardwareLayer, dataValidator, commandBuilder);
    }

    @Provides
    @BleScope
    public BleHardwareConnectionLayer provideHardwareLayer(RxBleClient client, @Nullable BluetoothAdapter bluetoothAdapter, Context context) {
        return new BleHardwareConnectionLayer(client, bluetoothAdapter, context);
    }

    @Provides
    @BleScope
    public RxBleClient provideBleClient(Context context) {
        return RxBleClient.create(context);
    }

    @Provides
    @Nullable
    @BleScope
    public BluetoothAdapter provideBluetoothAdapter(Context context) {
        BluetoothManager manager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        if(manager!=null)
            return manager.getAdapter();
        else
            return null;
    }
}
