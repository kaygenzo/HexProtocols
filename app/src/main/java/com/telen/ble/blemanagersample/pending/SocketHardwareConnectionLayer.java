package com.telen.ble.blemanagersample.pending;

import android.content.Context;
import android.util.Log;

import com.telen.ble.sdk.layers.HardwareLayerInterface;
import com.telen.ble.sdk.model.Device;
import com.telen.ble.sdk.model.Request;
import com.telen.ble.sdk.model.Response;
import com.telen.ble.sdk.utils.BytesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

public class SocketHardwareConnectionLayer implements HardwareLayerInterface {

    private static final String TAG = SocketHardwareConnectionLayer.class.getSimpleName();
    private static final long TIMEOUT_RESPONSE_MILLIS = 3000L;

    private DatagramSocket mDatagramSocket;
    private Socket mSocket;
    private Context mContext;
    private final OkHttpClient mHttpClient;
    private TCPSocketManager tcpSocketManager;

    private CompositeDisposable operationsDisposable = new CompositeDisposable();

    public SocketHardwareConnectionLayer(Context context, OkHttpClient okHttpClient, TCPSocketManager tcpSocketManager) {
        this.mContext = context;
        this.mHttpClient = okHttpClient;
        this.tcpSocketManager = tcpSocketManager;
    }

    @Override
    public Completable connect(Device device, boolean createBond) {
        return null;
    }

    @Override
    public Completable disconnect(Device device) {
        return null;
    }

    @Override
    public Single<String> sendCommand(Device device, Request request, String command) {
        return sendCommand(device, request, BytesUtils.hexStringToByteArray(command));
    }

    @Override
    public Single<String> sendCommand(Device device, Request request, byte[] command) {
        return sendTCPCommand(request, command).andThen(Single.just("Done"));
    }

    @Override
    public Observable<String> listenResponses(Device device, Response response) {
        return listenTCPResponse();
    }

    @Override
    public Single<Device> scan(String deviceName) {
        return null;
    }

    @Override
    public Single<Device> scanOld(String deviceName) {
        return null;
    }

    @Override
    public Single<Boolean> isBonded(String macAddress) {
        return null;
    }

    @Override
    public Completable preProcessBeforeSendingCommand(Request request) {
        return Completable.create(emitter -> {
            int port = request.getPort();
            String address = request.getAddress();

            operationsDisposable.clear();

            if(mSocket!=null && mSocket.isConnected())
                mSocket.close();

            try {
                mSocket = mHttpClient.socketFactory().createSocket(Inet4Address.getByName(address), port);
                emitter.onComplete();
            }catch (UnknownHostException e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    ////////////////////// UDP /////////////////////

    private Single<String> listenResponse(int bufferSize) {
        return Single.create((SingleOnSubscribe<String>) emitter -> {
            byte[] buffer = new byte[bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
            String currentAddress = getCurrentIPAddress();
            String message;
            do {
                mDatagramSocket.receive(packet);
                message = new String(packet.getData(), 0, packet.getLength());
            } while (packet.getAddress().getHostAddress().contains(currentAddress));
            emitter.onSuccess(message);
        }).timeout(TIMEOUT_RESPONSE_MILLIS, TimeUnit.MILLISECONDS);
    }

    private Observable<String> sendRequest(final byte[] message, final String address, final int port, boolean isBroadcast, boolean waitForResponse) {
        return Observable.create(emitter -> {
            mDatagramSocket.setBroadcast(isBroadcast);
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(address), port);
            mDatagramSocket.send(packet);
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

    ////////////////////// TCP /////////////////////

    private Completable sendTCPCommand(Request request, byte[] dataToSend) {
        return Completable.create(emitter -> {
            if(mSocket!=null) {
                Disposable sendDisposable = tcpSocketManager.sendTCPMessage(mSocket.getOutputStream(), dataToSend)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(() -> {
                            if(!emitter.isDisposed()) {
                                Log.d(TAG, "sendTCPMessage complete");
                                emitter.onComplete();
                            }
                        }, throwable -> {
                            if(!emitter.isDisposed()) {
                                Log.e(TAG, "", throwable);
                                emitter.onError(throwable);
                            }
                        });
                operationsDisposable.add(sendDisposable);
            }
            else
                emitter.onError(new Exception("Null socket for address "+request.getAddress()+" and port "+request.getPort()+" !"));
        });
    }

    private Observable<String> listenTCPResponse() {
        return Observable.create(emitter -> {
            if(mSocket!=null && mSocket.isConnected()) {
                Disposable  disposable = tcpSocketManager.listenTCPMessage(mSocket.getInputStream())
                        .subscribe(s -> {
                            if(!emitter.isDisposed())
                                emitter.onNext(s);
                        }, throwable -> {
                            if(!emitter.isDisposed())
                                emitter.onError(throwable);
                        }, () -> {
                            if(!emitter.isDisposed())
                                emitter.onComplete();
                        });
                operationsDisposable.add(disposable);
            }
            else {
                emitter.onError(new Exception("Socket not ready!"));
            }
        });
    }
}
