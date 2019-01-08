package com.telen.sdk.socket.utils;

import android.util.Log;

import com.telen.sdk.common.utils.BytesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

public class TCPSocketManager {

    private static final String TAG = TCPSocketManager.class.getSimpleName();

    //TODO wait only for 1 response, maybe manage multiline in the future
    public Observable<String> listenTCPMessage(final InputStream in) {
        return Observable.create( (ObservableOnSubscribe<String>)  emitter -> {
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
                            break;
                        }
                    }

                    if(!emitter.isDisposed())
                        emitter.onComplete();
                }
                else if(!emitter.isDisposed())
                    emitter.onError(new Exception("TCP InputStream is closed, it's illegal here!"));
            } catch (IOException e) {
                e.printStackTrace();
                if(!emitter.isDisposed())
                    emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io()).observeOn(Schedulers.io());
    }

    public Completable sendTCPMessage(OutputStream out, byte[] messageAsBytes) {
        return Completable.create(emitter -> {
            Log.d(TAG,"sendTCPMessage --> "+BytesUtils.byteArrayToHex(messageAsBytes));
            out.write(BytesUtils.hexStringToByteArray(BytesUtils.byteArrayToHex(messageAsBytes)));
            out.flush();
            emitter.onComplete();
        }).subscribeOn(Schedulers.io());
    }
}
