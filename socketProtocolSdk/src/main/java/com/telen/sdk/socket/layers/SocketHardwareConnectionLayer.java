package com.telen.sdk.socket.layers;

import android.content.Context;
import android.util.Log;

import com.telen.sdk.common.layers.HardwareLayerInterface;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.Request;
import com.telen.sdk.socket.models.RequestType;
import com.telen.sdk.common.models.Response;
import com.telen.sdk.common.utils.BytesUtils;
import com.telen.sdk.socket.devices.SocketDevice;
import com.telen.sdk.socket.utils.TCPSocketManager;
import com.telen.sdk.socket.utils.UDPSocketManager;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Socket;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

public class SocketHardwareConnectionLayer implements HardwareLayerInterface {

    private static final String TAG = SocketHardwareConnectionLayer.class.getSimpleName();

    private Socket mSocket;
    private DatagramSocket datagramSocket;

    private Context mContext;
    private final OkHttpClient mHttpClient;
    private TCPSocketManager tcpSocketManager;
    private UDPSocketManager udpSocketManager;

    private CompositeDisposable operationsDisposable = new CompositeDisposable();

    public SocketHardwareConnectionLayer(Context context, OkHttpClient okHttpClient, TCPSocketManager tcpSocketManager, UDPSocketManager udpSocketManager) {
        this.mContext = context;
        this.mHttpClient = okHttpClient;
        this.tcpSocketManager = tcpSocketManager;
        this.udpSocketManager = udpSocketManager;
    }

    @Override
    public Completable connect(@NonNull Device device, boolean bind) {
        return Completable.create(emitter -> {
            SocketDevice socketDevice = (SocketDevice)device;

            operationsDisposable.clear();

                RequestType requestType = socketDevice.getType();
                switch (requestType) {
                    case tcp:
                        int port = socketDevice.getPort();
                        String address = socketDevice.getAddress();

                        if(mSocket!=null && mSocket.isConnected())
                            mSocket.close();

                        try {
                            mSocket = mHttpClient.socketFactory().createSocket(Inet4Address.getByName(address), port);
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                        break;
                    case udp:
                        if(datagramSocket!=null)
                            datagramSocket.close();

                        try {
                            datagramSocket =  new DatagramSocket();
                            emitter.onComplete();
                        }
                        catch (Exception e) {
                            emitter.onError(e);
                            return;
                        }
                        break;
                }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable disconnect(Device device) {
        return Completable.create(emitter -> {
            if(mSocket!=null && mSocket.isConnected())
                mSocket.close();
            if(datagramSocket!=null)
                datagramSocket.close();
            emitter.onComplete();
        });
    }

    //TODO imagine a way to determine how to transform into bytes array
    @Override
    public Single<String> sendCommand(Device device, Request request, String command) {
        try {
            RequestType requestType = RequestType.valueOf(request.getType());
            if(requestType == RequestType.tcp)
                return sendTCPCommand(request, BytesUtils.hexStringToByteArray(command))
                        .andThen(Single.just("Done"));
            else {
                Log.d(TAG, "command["+command+"]");
                return sendUDPRequest(command.getBytes(), request)
                        .andThen(Single.just("Done"));
            }
        }
        catch (IllegalArgumentException e) {
            return Single.error(e);
        }
    }

    @Override
    public Single<String> sendCommand(Device device, Request request, byte[] command) {
        //TODO for now it's not used by the datalayer, but could be in the future
        return null;
    }

    @Override
    public Observable<String> listenResponses(Device device, Response response) {
        try {
            RequestType requestType = RequestType.valueOf(response.getType());
            if(requestType == RequestType.tcp)
                return listenTCPResponse();
            else
                return listenUDPResponse().toObservable();
        }
        catch (IllegalArgumentException e) {
            return Observable.error(e);
        }
    }

    @Override
    public Single<Device> scan(String deviceName) {
        //TODO define what is the purpose of this scan
        return null;
    }

    @Override
    public Completable prepareBeforeSendingCommand(Request request) {
        return Completable.complete();
    }

    @Override
    public Single<Boolean> isConnected(Device device) {
        return Single.create(emitter -> {
            SocketDevice socketDevice = (SocketDevice)device;
            switch (socketDevice.getType()) {
                case tcp:
                    if(mSocket!=null && mSocket.isConnected())
                        emitter.onSuccess(Boolean.TRUE);
                    else
                        emitter.onSuccess(Boolean.FALSE);
                        break;
                case udp:
                    if(datagramSocket!=null && datagramSocket.isConnected())
                        emitter.onSuccess(Boolean.TRUE);
                    else
                        emitter.onSuccess(Boolean.FALSE);
                    break;
            }
        });
    }

    ////////////////////// UDP /////////////////////

    private Single<String> listenUDPResponse() {
        return Single.create(emitter -> {
            if(datagramSocket!=null) {
                Disposable  disposable = udpSocketManager.listenResponse(datagramSocket, 2048)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(s -> {
                            if(!emitter.isDisposed()) {
                                emitter.onSuccess(s);
                            }
                        }, throwable -> {
                            if(!emitter.isDisposed())
                                emitter.onError(throwable);
                        });
                operationsDisposable.add(disposable);
            }
            else {
                emitter.onError(new Exception("Socket not ready!"));
            }
        });
    }

    private Completable sendUDPRequest(final byte[] message, Request request) {
        return Completable.create(emitter -> {
            if(datagramSocket!=null) {
                Disposable sendDisposable = udpSocketManager.sendRequest(datagramSocket,message, request.getAddress(), request.getPort(), request.isBroadcast())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(() -> {
                            if(!emitter.isDisposed()) {
                                Log.d(TAG, "sendUDPMessage complete");
                                emitter.onComplete();
                            }
                        }, throwable -> {
                            if(!emitter.isDisposed()) {
                                emitter.onError(throwable);
                            }
                        });
                operationsDisposable.add(sendDisposable);
            }
            else
                emitter.onError(new Exception("Null datagram socket for address "+request.getAddress()+" and port "+request.getPort()+" !"));
        });
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
