package com.telen.ble.blemanagersample.devices.ribbon;

import android.content.Context;
import android.util.Log;

import com.telen.ble.blemanagersample.DaggerApplicationWrapper;
import com.telen.ble.blemanagersample.DeviceInfo;
import com.telen.ble.blemanagersample.GenericWifiDevice;
import com.telen.ble.blemanagersample.pending.SocketHardwareConnectionLayer;
import com.telen.ble.sdk.layers.DataLayerInterface;
import com.telen.ble.sdk.model.Device;
import com.telen.ble.sdk.model.DeviceConfiguration;
import com.telen.ble.sdk.model.ProtocolConfiguration;
import com.telen.ble.sdk.utils.BytesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

public class LightRibbon implements GenericWifiDevice {

    private static final String TAG = LightRibbon.class.getSimpleName();

    private static final long TIMEOUT_RESPONSE_MILLIS = 3000L;

    private DeviceConfiguration deviceConfiguration;
    @Inject DataLayerInterface<SocketHardwareConnectionLayer> dataLayer;

//    private static final String OS = "Android";
//    private static final String APP_VERSION = "8.0.0";
//    private static final String MAC_ADDRESS = "DC4F22C0D904";

    private DatagramSocket mSocket;
    private final OkHttpClient mOkHttpClient;

    public LightRibbon(Context context) {
        DaggerApplicationWrapper.getComponent(context).inject(this);
        this.mOkHttpClient = new OkHttpClient();
        deviceConfiguration = ProtocolConfiguration.parse(context, DeviceInfo.RIBBON.getProtocolPath());
    }

    public Completable lightOn() {
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

    public Completable lightOff() {
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

    public Completable changeColor(int red, int green, int blue) {
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
        return null;
    }

    @Override
    public Single<Device> connect(Device device, boolean createBond) {
        return Single.create(emitter -> {
            final int port = 48899;

            if(mSocket!=null)
                mSocket.close();

            try {
                mSocket =  new DatagramSocket();
            }
            catch (Exception e) {
                emitter.onError(e);
                return;
            }

            getRemoteAddress("10.10.123.255", port)
                    .flatMapCompletable(remoteAddress -> {
                        Log.d(TAG, "remoteAddress="+remoteAddress);
                        return sendAck(remoteAddress, port)
                                .andThen(sendSsid(remoteAddress, port))
                                .andThen(sendWKey(remoteAddress, port))
                                .andThen(changeMode(remoteAddress, port))
                                .andThen(reboot(remoteAddress, port));
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Log.d(TAG, "complete!!");
                        if(mSocket!=null)
                            mSocket.close();
                    }, throwable -> {
                        Log.e(TAG, "", throwable);
                        if(mSocket!=null)
                            mSocket.close();
                    });
        });
    }

    @Override
    public Completable disconnect(Device device) {
        return null;
    }

    ////////////////////// TCP /////////////////////

    //TODO wait only for 1 response, maybe manage multiline in the future
    private Observable<String> listenTCPMessage(final InputStream in) {
        return Observable.create(emitter -> {
            Log.d(TAG,"listenTCPMessage");
//            BufferedReader mBufferIn = new BufferedReader(new InputStreamReader(in));
            try {
                if(in!=null) {
                    //TODO rather than infinite loop, listen for dispose state of emitter
                    while (true) {
                        byte[] buffer = new byte[4096];
                        int read = in.read(buffer);
                        if (read > 0) {
                            String response = new String(buffer, 0, read);
                            Log.d(TAG, "listenTCPMessage <-- " + response);
                            emitter.onNext(response);
                            break;
                        }
                    }
                    emitter.onComplete();
                }
            } catch (IOException e) {
                e.printStackTrace();
                emitter.onError(e);
            }
        });
    }

    private Completable sendTCPMessage(OutputStream out, byte[] messageAsBytes) {
        return Completable.create(emitter -> {
            Log.d(TAG,"sendTCPMessage --> "+BytesUtils.byteArrayToHex(messageAsBytes));
            out.write(BytesUtils.hexStringToByteArray(BytesUtils.byteArrayToHex(messageAsBytes)));
            out.flush();
            emitter.onComplete();
        });
    }

    ////////////////////// UDP /////////////////////

    private Single<String> getRemoteAddress(String broadcastAddress, int port) {
        byte[] password = "HF-A11ASSISTHREAD".getBytes();
        return sendRequest(password, broadcastAddress, port, true, true)
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

    private Completable sendAck(String ip, int port) {
        byte[] ack = "+ok".getBytes();
        return sendRequest(ack, ip, port, false, false)
                .ignoreElements();
    }

    private Completable sendSsid(String ip, int port) {
        byte[] wsssid = "AT+WSSSID={SSID}\r".getBytes();
        return sendRequest(wsssid, ip, port, false, true)
                .flatMapCompletable(response -> {
                    Log.d(TAG, "sendSsid response="+response);
                    if(response.startsWith("+ok="))
                        return Completable.complete();
                    else
                        return Completable.error(new Exception("Bad response from server"));
                });
    }

    private Completable sendWKey(String ip, int port) {
        byte[] wskey = "AT+WSKEY=WPA2PSK,AES,{PASSWORD}\r".getBytes();
        return sendRequest(wskey, ip, port, false, true)
                .flatMapCompletable(response -> {
                    Log.d(TAG, "sendWKey response="+response);
                    if(response.startsWith("+ok="))
                        return Completable.complete();
                    else
                        return Completable.error(new Exception("Bad response from server"));
                });
    }

    private Completable changeMode(String ip, int port) {
        byte[] wmode = "AT+WMODE=STA\r".getBytes();
        return sendRequest(wmode, ip, port, false, true)
                .flatMapCompletable(response -> {
                    Log.d(TAG, "changeMode response="+response);
                    if(response.startsWith("+ok="))
                        return Completable.complete();
                    else
                        return Completable.error(new Exception("Bad response from server"));
                });
    }

    private Completable reboot(String ip, int port) {
        byte[] reboot = "AT+Z\r".getBytes();
        return sendRequest(reboot, ip, port, false, true)
                .flatMapCompletable(response -> {
                    Log.d(TAG, "reboot response="+response);
                    if(response.startsWith("+ok="))
                        return Completable.complete();
                    else
                        return Completable.error(new Exception("Bad response from server"));
                });
    }

    private Single<String> listenResponse(int bufferSize) {
        return Single.create((SingleOnSubscribe<String>)  emitter -> {
            //Log.d(TAG, "listenResponse");
            byte[] buffer = new byte[bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
            String currentAddress = getCurrentIPAddress();
            String message;
            do {
                mSocket.receive(packet);
                message = new String(packet.getData(), 0, packet.getLength());
//                String rcvd = message + ", from address: " + packet.getAddress() + ", port: " + packet.getPort();
//                Log.d(TAG, rcvd);
            } while (packet.getAddress().getHostAddress().contains(currentAddress));
            emitter.onSuccess(message);
        }).timeout(TIMEOUT_RESPONSE_MILLIS, TimeUnit.MILLISECONDS);
    }

    private Observable<String> sendRequest(final byte[] message, final String address, final int port, boolean isBroadcast, boolean waitForResponse) {
        return Observable.create(emitter -> {
//            Log.d(TAG, "sendRequest");
            mSocket.setBroadcast(isBroadcast);
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(address), port);
            mSocket.send(packet);
            if(waitForResponse) {
                listenResponse(2048)
                        .subscribeOn(Schedulers.io())
                        .subscribe(s -> {
                            emitter.onNext(s);
                            emitter.onComplete();
                        }, emitter::onError);
            }
            else
                emitter.onComplete();
        });
    }

    private String getCurrentIPAddress()
    {
        try
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                        return inetAddress.getHostAddress();
                }
            }
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
