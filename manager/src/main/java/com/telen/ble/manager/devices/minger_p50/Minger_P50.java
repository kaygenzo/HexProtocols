package com.telen.ble.manager.devices.minger_p50;

import android.content.Context;

import com.telen.ble.manager.layers.impl.DataLayerImpl;
import com.telen.ble.manager.model.Device;
import com.telen.ble.manager.model.DeviceConfiguration;
import com.telen.ble.manager.model.DeviceInfo;
import com.telen.ble.manager.model.ProtocolConfiguration;
import com.telen.ble.manager.devices.GenericDevice;
import com.telen.ble.manager.di.DaggerWrapper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Minger_P50 implements GenericDevice {

    @Inject
    DataLayerImpl dataLayer;
    @Inject Context mContext;

    private DeviceConfiguration deviceConfiguration;

    public Minger_P50(Context context) {
        DaggerWrapper.getComponent(context).inject(this);
        deviceConfiguration = ProtocolConfiguration.parse(mContext, DeviceInfo.MINGER);
    }

    @Override
    public Single<Device> scan() {
        return dataLayer.scan(deviceConfiguration.getDeviceNames()[0])
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<Device> connect(Device device, boolean createBond) {
        return dataLayer.connect(device, createBond)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Completable disconnect(Device device) {
        return dataLayer.disconnect(device)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<String> apply(Device device, int red, int green, int blue, int value) {
        Map<String, Object> data = new HashMap<>();
        data.put("RED",red);
        data.put("GREEN",green);
        data.put("BLUE",blue);
        data.put("LUMINOSITY_1",value);
        data.put("LUMINOSITY_2",value);
        return dataLayer.sendCommand(device, deviceConfiguration.getCommand("CHANGE_COLOR"),data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
