package com.telen.sdk.demo.devices.minger;

import android.content.Context;

import com.telen.sdk.demo.DaggerApplicationWrapper;
import com.telen.sdk.demo.DeviceInfo;
import com.telen.sdk.ble.devices.GenericBleDevice;
import com.telen.sdk.ble.layers.impl.BleHardwareConnectionLayer;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.DeviceConfiguration;
import com.telen.sdk.common.models.ProtocolConfiguration;

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

    @Override
    public Single<Boolean> isConnected(Device device) {
        return dataLayer.isConnected(device);
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
