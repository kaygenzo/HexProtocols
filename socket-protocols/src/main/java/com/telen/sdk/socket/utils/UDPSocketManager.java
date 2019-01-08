package com.telen.sdk.socket.utils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import io.reactivex.Completable;
import io.reactivex.Single;

public class UDPSocketManager {
    private static final String TAG = UDPSocketManager.class.getSimpleName();

    private NetworkUtils networkUtils;

    public UDPSocketManager(NetworkUtils networkUtils) {
        this.networkUtils = networkUtils;
    }

    public Single<String> listenResponse(DatagramSocket socket, int bufferSize) {
        return Single.create(emitter -> {
            //Log.d(TAG, "listenResponse");
            byte[] buffer = new byte[bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
            String currentAddress = networkUtils.getCurrentIPAddress();
            String message;
            do {
                try {
                    socket.receive(packet);
                    message = new String(packet.getData(), 0, packet.getLength());
                }
                catch (SocketException e) {
                    if(!emitter.isDisposed())
                        emitter.onError(e);
                    return;
                }
            } while (currentAddress!=null && packet.getAddress().getHostAddress().contains(currentAddress));
            emitter.onSuccess(message);
        });
    }

    public Completable sendRequest(final DatagramSocket socket, final byte[] message, final String address, final int port, boolean isBroadcast) {
        return Completable.create(emitter -> {
//            Log.d(TAG, "sendRequest");
            try {
                socket.setBroadcast(isBroadcast);
                DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(address), port);
                socket.send(packet);
                emitter.onComplete();
            }
            catch (SocketException e) {
                if(!emitter.isDisposed())
                    emitter.onError(e);
            }
        });
    }
}
