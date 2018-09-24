package com.telen.ble.manager.devices.minger_p50;

import android.content.Context;

import com.telen.ble.manager.layers.impl.DataLayerImpl;
import com.telen.ble.manager.model.Device;
import com.telen.ble.manager.model.DeviceConfiguration;
import com.telen.ble.manager.model.DeviceInfo;
import com.telen.ble.manager.model.ProtocolConfiguration;
import com.telen.ble.manager.devices.GenericDevice;
import com.telen.ble.manager.di.DaggerWrapper;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class SimulatorBLE implements GenericDevice {

    @Inject
    DataLayerImpl dataLayer;
    @Inject Context mContext;

    private DeviceConfiguration deviceConfiguration;

    public SimulatorBLE(Context context) {
        DaggerWrapper.getComponent(context).inject(this);
        deviceConfiguration = ProtocolConfiguration.parse(mContext, DeviceInfo.SIMULATOR);
    }

    @Override
    public Single<Device> connect() {
        return dataLayer.connect(deviceConfiguration.getDeviceNames()[0])
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    @Override
    public Completable disconnect(Device device) {
        return dataLayer.disconnect(device)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }
}
