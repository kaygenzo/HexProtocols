package com.telen.ble.manager.devices.minger_p50;

import android.content.Context;

import com.telen.ble.manager.BleDataLayer;
import com.telen.ble.manager.data.Device;
import com.telen.ble.manager.data.DeviceConfiguration;
import com.telen.ble.manager.data.DeviceInfo;
import com.telen.ble.manager.data.ProtocolConfiguration;
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

    @Inject BleDataLayer dataLayer;
    @Inject Context mContext;

    private DeviceConfiguration deviceConfiguration;

    public Minger_P50(Context context) {
        DaggerWrapper.getComponent(context).inject(this);
        deviceConfiguration = ProtocolConfiguration.parse(mContext, DeviceInfo.MINGER);
    }

    @Override
    public Single<Device> connect() {
        return dataLayer.connect(deviceConfiguration.getDeviceNames()[0])
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Completable disconnect(Device device) {
        return dataLayer.disconnect(device);
    }

    public Observable<String> changeColor(Device device, int red, int green, int blue) {
        Map<String, Object> data = new HashMap<>();
        data.put("RED",red);
        data.put("GREEN",green);
        data.put("BLUE",blue);
        return dataLayer.sendCommand(device, deviceConfiguration.getCommand("CHANGE_COLOR"),data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
