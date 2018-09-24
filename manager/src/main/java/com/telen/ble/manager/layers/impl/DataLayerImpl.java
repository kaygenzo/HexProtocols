package com.telen.ble.manager.layers.impl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.telen.ble.manager.model.Command;
import com.telen.ble.manager.model.Device;
import com.telen.ble.manager.model.Payload;
import com.telen.ble.manager.exceptions.CommandTimeoutException;
import com.telen.ble.manager.layers.DataLayerInterface;
import com.telen.ble.manager.layers.HardwareLayerInterface;
import com.telen.ble.manager.validator.DataValidator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DataLayerImpl implements DataLayerInterface {

    private static final String TAG = DataLayerImpl.class.getSimpleName();

    private long TIMEOUT_MILLIS = 30000l;

    private CompositeDisposable timeoutDisposable = new CompositeDisposable();
    private CompositeDisposable dataListenerDisposable = new CompositeDisposable();

    private HardwareLayerInterface hardwareInteractionLayer;
    private DataValidator dataValidator;

    public DataLayerImpl(HardwareLayerInterface hardwareLayer, DataValidator validator) {
        this.hardwareInteractionLayer = hardwareLayer;
        this.dataValidator = validator;
    }


    @Override
    public Single<Device> scan(String deviceName) {
        return Single.create(emitter -> hardwareInteractionLayer.scan(deviceName)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<Device> connect(Device device, boolean createBond) {
        return Single.create(emitter ->
                hardwareInteractionLayer.connect(device, createBond)
                        .doOnComplete(() -> emitter.onSuccess(device))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(() -> {}, emitter::onError)
        );
    }

    @Override
    public Completable disconnect(Device device) {
            return hardwareInteractionLayer.disconnect(device)
                    .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<String> sendCommand(Device device, Command command, Map<String, Object> data) {
        return Observable.create(emitter -> {
            dataListenerDisposable.clear();
            //let's validate payloads and build the hexa string command
            dataValidator.validateData(command.getRequest().getPayloads(), data)
                    .andThen(buildHexaCommand(command.getRequest().getPayloads(), data))
                    .flatMap(hexaCommand -> {
                        startTimeout(emitter);
                        UUID requestUuid = UUID.fromString(command.getRequest().getCharacteristic());
                        //if we expect some response from remote device, we listen for any response before sending command
                        if(command.getResponse()!=null) {
                            UUID responseUuid = UUID.fromString(command.getResponse().getCharacteristic());
                            hardwareInteractionLayer
                                    .listenResponses(device, responseUuid)
                                    .flatMap(response -> dataValidator.validateData(command.getResponse().getPayloads(), response)
                                            .andThen(Observable.just(response)))
                            .subscribe(new Observer<String>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                    dataListenerDisposable.add(d);
                                }

                                @Override
                                public void onNext(String s) {
                                    emitter.onNext(s);
                                    String endFrame = command.getResponse().getEndFrame();
                                    if(endFrame !=null && endFrame.replace(" ","").equals(s)) {
                                        dataListenerDisposable.clear();
                                        emitter.onComplete();
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                    emitter.onError(e);
                                }

                                @Override
                                public void onComplete() {
                                    emitter.onComplete();
                                }
                            });
                        }
                        return hardwareInteractionLayer.sendCommand(device, requestUuid, hexaCommand);
                    })
                    .subscribe(new SingleObserver<String>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(String responseFrame) {
                            stopTimeout();
                            Log.d(TAG,"Sent -- responseFrame="+responseFrame);
                            if(command.getResponse()==null) {
                                Log.d(TAG,"Don't need to wait for any response, bye bye!");
                                emitter.onComplete();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if(!emitter.isDisposed())
                                emitter.onError(e);
                        }
                    });
        });
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Single<String> buildHexaCommand(@NonNull List<Payload> payloads, @Nullable Map<String, Object> data) {
        return Single.create(emitter -> {
            String[] commandArray = new String[20];
            Arrays.fill(commandArray, "00");

            for (Payload payload: payloads) {
                String identifier = payload.getName();
                Object obj = null;
                if(data != null)
                    obj = data.get(identifier);
                if(obj==null)
                    obj = payload.getValue();
                int start = payload.getStart();
                int end = payload.getEnd();
                StringBuilder hexBuilder = new StringBuilder();
                switch (payload.getType()) {
                    case "HEX":
                        //keep value like this
                        String hex = (String) obj;
                        hexBuilder.append(hex);
                        break;
                    case "INTEGER":
                        //convert integer to hex value
                        Integer integer;
                        if(obj instanceof Integer) {
                            integer = (Integer) obj;
                        }
                        else {
                            integer = Integer.parseInt(obj.toString());
                        }
                        hexBuilder.append(Integer.toHexString(integer));
                        break;
                    case "LONG":
                        //convert integer to hex value
                        Long longValue = (Long) obj;
                        hexBuilder.append(Long.toHexString(longValue));
                        break;
                }
                //if it's an odd string length, let's add 0 at start to be able to build 2-digits packets
                if(hexBuilder.length()%2==1)
                    hexBuilder.insert(0,'0');
                //let's cut the hex value into array of 2-digits
                //String[] splittedValue = hexBuilder.toString().split("(?<=\\G.{2})");
                String[] splittedValue = splitStringByLength(hexBuilder.toString(), 2);
                for(int i=end, j=splittedValue.length-1;i>=start && j>=0;i--,j--) {
                    commandArray[i] = splittedValue[j];
                }
            }

            StringBuilder finalCommand = new StringBuilder();
            for (String byteString: commandArray) {
                finalCommand.append(byteString);
            }
            emitter.onSuccess(finalCommand.toString());
        });
    }

    private void startTimeout(ObservableEmitter emitter) {
        if(TIMEOUT_MILLIS>0) {
            timeoutDisposable.add(Observable.timer(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).subscribe(
                    aLong -> {
                    },
                    throwable -> {
                    },
                    () -> {
                        if(emitter!=null && !emitter.isDisposed())
                            emitter.onError(new CommandTimeoutException("Command timeout triggered"));
                    }
                    )
            );
        }
    }

    private void stopTimeout() {
        timeoutDisposable.clear();
    }

    public void setTimeout(long timeout) {
        this.TIMEOUT_MILLIS = timeout;
    }

    private String[] splitStringByLength(String string, int length) {
        if(string!=null && string.length()>0) {
            String[] result = new String[(string.length()+1)/length];
            int index = 0;
            int cpt = 0;
            while (index<string.length()) {
                String subString = string.substring(index, Math.min(index+length, string.length()));
                result[cpt] = subString;
                cpt++;
                index = index + length;
            }
            return result;
        }
        return null;
    }
}
