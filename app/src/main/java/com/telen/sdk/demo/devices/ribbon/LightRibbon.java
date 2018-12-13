package com.telen.sdk.demo.devices.ribbon;

import android.content.Context;
import android.util.Log;

import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.common.models.Command;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.DeviceConfiguration;
import com.telen.sdk.common.models.ProtocolConfiguration;
import com.telen.sdk.socket.models.RequestType;
import com.telen.sdk.demo.DaggerApplicationWrapper;
import com.telen.sdk.demo.DeviceInfo;
import com.telen.sdk.socket.devices.GenericNetworkDevice;
import com.telen.sdk.socket.devices.SocketDevice;
import com.telen.sdk.socket.layers.SocketHardwareConnectionLayer;
import com.telen.sdk.socket.utils.NetworkUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class LightRibbon implements GenericNetworkDevice {

    private static final String TAG = LightRibbon.class.getSimpleName();

    private DeviceConfiguration deviceConfiguration;
    private Context mContext;
    @Inject DataLayerInterface<SocketHardwareConnectionLayer> dataLayer;
    @Inject NetworkUtils networkUtils;

    public LightRibbon(Context context) {
        this.mContext = context;
        DaggerApplicationWrapper.getComponent(context).inject(this);
        deviceConfiguration = ProtocolConfiguration.parse(context, DeviceInfo.RIBBON.getProtocolPath());
    }

    public Completable lightOn(SocketDevice device) {
        return  Completable.create(emitter -> {
            HashMap<String, Object> data = new HashMap<>();
            LightOnCMD lightOnCommand = new LightOnCMD();
            data.put("CHECKSUM", lightOnCommand.getHexDataArray(null)[3] & 0xFF);
            dataLayer.sendCommand(null, deviceConfiguration.getCommand("LIGHT_ON"), data)
                    .subscribe(s -> {
                        Log.d(TAG, "Response received: "+s);
                    }, emitter::onError, emitter::onComplete);
        });
    }

    public Completable lightOff(SocketDevice device) {
        return  Completable.create(emitter -> {
            HashMap<String, Object> data = new HashMap<>();
            LightOffCMD lightOnCommand = new LightOffCMD();
            data.put("CHECKSUM", lightOnCommand.getHexDataArray(null)[3] & 0xFF);
            dataLayer.sendCommand(null, deviceConfiguration.getCommand("LIGHT_OFF"), data)
                    .subscribe(s -> {
                        Log.d(TAG, "Response received: "+s);
                    }, emitter::onError, emitter::onComplete);
        });
    }

    public Completable changeColor(SocketDevice device, int red, int green, int blue) {
        return Completable.create(emitter -> {
            Map<String, Integer> dataCmd = new HashMap<>();
            ChangeColorCMD cmd = new ChangeColorCMD();
            dataCmd.put("RED", red);
            dataCmd.put("GREEN", green);
            dataCmd.put("BLUE", blue);
            byte[] bytes = cmd.getHexDataArray(dataCmd);
            HashMap<String, Object> data = new HashMap<>();
            data.put("RED", bytes[1] & 0xFF);
            data.put("GREEN", bytes[2] & 0xFF);
            data.put("BLUE", bytes[3] & 0xFF);
            data.put("CHECKSUM", bytes[7] & 0xFF);
            dataLayer.sendCommand(null, deviceConfiguration.getCommand("CHANGE_COLOR"), data)
                    .subscribe(s -> {
                        Log.d(TAG, "Response received: "+s);
                    }, emitter::onError, emitter::onComplete);
        });
    }

    @Override
    public Single<Device> scan() {
        return getRemoteAddress().map(remoteAddress -> {
            Log.d(TAG, "complete!!");
            SocketDevice socketDevice = new SocketDevice(LightRibbon.class.getSimpleName());
            socketDevice.setAddress(remoteAddress);
            return socketDevice;
        });
    }

    @Override
    public Single<Device> configureNetwork(SocketDevice device, String ssid, String password) {
        return Single.create(emitter -> {
            getRemoteAddress()
                    .flatMapCompletable(remoteAddress -> {
                        Log.d(TAG, "remoteAddress="+remoteAddress);
                        return sendAck(device, remoteAddress)
                                .andThen(sendSsid(device, remoteAddress, "SSID"))
                                .andThen(sendWKey(device, remoteAddress, "PASSWORD"))
                                .andThen(changeMode(device, remoteAddress, "STA"))
                                .andThen(reboot(device, remoteAddress));
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Log.d(TAG, "complete!!");
                        dataLayer.disconnect(null);
                        //TODO return true device
                        emitter.onSuccess(new Device(null, null));
                    }, throwable -> {
                        Log.e(TAG, "", throwable);
                        dataLayer.disconnect(null);
                        emitter.onError(throwable);
                    });
        });
    }

    @Override
    public Single<Device> connect(SocketDevice device, RequestType requestType) {
        switch (requestType) {
            case tcp:
                device.setPort(5577);
                break;
            case udp:
                device.setPort(48899);
                break;
        }
        device.setType(requestType);
        return connect(device, true);
    }

    @Override
    public Single<Device> connect(Device device, boolean bind) {
        return dataLayer.connect(device, bind);
    }

    @Override
    public Completable disconnect(Device device) {
        return dataLayer.disconnect(device);
    }

    @Override
    public Single<Boolean> isConnected(Device device) {
        return dataLayer.isConnected(device);
    }

    ////////////////////// UDP /////////////////////

    private Single<String> getRemoteAddress() {

        Command command = deviceConfiguration.getCommand("GET_REMOTE_ADDRESS");
        command.getRequest().setAddress(networkUtils.getBroadcastAddress(mContext));

        return dataLayer.sendCommand(null, command)
                .firstOrError()
                .flatMap(response -> {
                    String[] split = response.split(",");
                    if(split.length == 3) {
                        return Single.just(split[0]);
                    }
                    else
                        return Single.error(new Throwable("Invalid response from remote server: response = "+response));
                });
    }

    private Completable sendAck(SocketDevice device, String ip) {
        Command command = deviceConfiguration.getCommand("SEND_ACK");
        command.getRequest().setAddress(ip);

        return dataLayer.sendCommand(device, command)
                .ignoreElements();
    }

    private Completable sendSsid(SocketDevice device, String ip, String ssid) {
        Command command = deviceConfiguration.getCommand("SEND_SSID");
        command.getRequest().setAddress(ip);

        Map<String, Object> data = new HashMap<>();
        data.put("SSID", ssid);

        return dataLayer.sendCommand(device, command, data)
                .flatMapCompletable(response -> {
                    Log.d(TAG, "sendSsid response="+response);
                    if(response.startsWith("+ok="))
                        return Completable.complete();
                    else
                        return Completable.error(new Exception("Bad response from server"));
                });
    }

    private Completable sendWKey(SocketDevice device, String ip, String password) {
        Command command = deviceConfiguration.getCommand("SEND_WSKEY");
        command.getRequest().setAddress(ip);

        Map<String, Object> data = new HashMap<>();
        data.put("PASSWORD", password);

        return dataLayer.sendCommand(device, command, data)
                .flatMapCompletable(response -> {
                    Log.d(TAG, "sendWKey response="+response);
                    if(response.startsWith("+ok="))
                        return Completable.complete();
                    else
                        return Completable.error(new Exception("Bad response from server"));
                });
    }

    private Completable changeMode(SocketDevice device, String ip, String mode) {
        Command command = deviceConfiguration.getCommand("CHANGE_MODE");
        command.getRequest().setAddress(ip);

        Map<String, Object> data = new HashMap<>();
        data.put("MODE", mode);

        return dataLayer.sendCommand(device, command, data)
                .flatMapCompletable(response -> {
                    Log.d(TAG, "changeMode response="+response);
                    if(response.startsWith("+ok="))
                        return Completable.complete();
                    else
                        return Completable.error(new Exception("Bad response from server"));
                });
    }

    private Completable reboot(SocketDevice device, String ip) {
        Command command = deviceConfiguration.getCommand("REBOOT");
        command.getRequest().setAddress(ip);

        return dataLayer.sendCommand(device, command)
                .flatMapCompletable(response -> {
                    Log.d(TAG, "reboot response="+response);
                    if(response.startsWith("+ok="))
                        return Completable.complete();
                    else
                        return Completable.error(new Exception("Bad response from server"));
                });
    }
}
