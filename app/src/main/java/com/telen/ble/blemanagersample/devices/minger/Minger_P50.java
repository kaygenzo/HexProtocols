package com.telen.ble.blemanagersample.devices.minger;

import android.content.Context;

import com.telen.ble.blemanagersample.DaggerApplicationWrapper;
import com.telen.ble.blemanagersample.DeviceInfo;
import com.telen.ble.sdk.devices.GenericBleDevice;
import com.telen.ble.sdk.layers.DataLayerInterface;
import com.telen.ble.sdk.layers.impl.BleHardwareConnectionLayer;
import com.telen.ble.sdk.layers.impl.DataLayerImpl;
import com.telen.ble.sdk.model.Device;
import com.telen.ble.sdk.model.DeviceConfiguration;
import com.telen.ble.sdk.model.ProtocolConfiguration;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Minger_P50 implements GenericBleDevice {

    @Inject DataLayerInterface<BleHardwareConnectionLayer> dataLayer;
    private Context mContext;

    private DeviceConfiguration deviceConfiguration;

    public Minger_P50(Context context) {
        this.mContext = context;
        DaggerApplicationWrapper.getComponent(context).inject(this);
        deviceConfiguration = ProtocolConfiguration.parse(mContext, DeviceInfo.MINGER.getProtocolPath());
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

    public Observable<String> lighOn(Device device) {
        Map<String, Object> data = new HashMap<>();
        data.put("RED",0);
        data.put("GREEN",0);
        data.put("BLUE",0);
        data.put("LUMINOSITY_1",255);
        data.put("LUMINOSITY_2",255);
        return dataLayer.sendCommand(device, deviceConfiguration.getCommand("CHANGE_COLOR"),data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<String> lighOff(Device device) {
        Map<String, Object> data = new HashMap<>();
        data.put("RED",0);
        data.put("GREEN",0);
        data.put("BLUE",0);
        data.put("LUMINOSITY_1",0);
        data.put("LUMINOSITY_2",0);
        return dataLayer.sendCommand(device, deviceConfiguration.getCommand("CHANGE_COLOR"),data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
