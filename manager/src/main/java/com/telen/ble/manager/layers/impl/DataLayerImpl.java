package com.telen.ble.manager.layers.impl;

import android.util.Log;

import com.telen.ble.manager.HexBuilder;
import com.telen.ble.manager.model.Command;
import com.telen.ble.manager.model.Device;
import com.telen.ble.manager.exceptions.CommandTimeoutException;
import com.telen.ble.manager.layers.DataLayerInterface;
import com.telen.ble.manager.layers.HardwareLayerInterface;
import com.telen.ble.manager.validator.DataValidator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
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
    private HexBuilder hexBuilder;

    public DataLayerImpl(HardwareLayerInterface hardwareLayer, DataValidator validator, HexBuilder hexBuilder) {
        this.hardwareInteractionLayer = hardwareLayer;
        this.dataValidator = validator;
        this.hexBuilder = hexBuilder;
    }


    @Override
    public Single<Device> scan(String deviceName) {
        return Single.create(emitter -> hardwareInteractionLayer.scanOld(deviceName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<Device> connect(Device device, boolean createBond) {
        return Single.create(emitter ->
                hardwareInteractionLayer.connect(device, createBond)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> emitter.onSuccess(device), emitter::onError)
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
                    .andThen(hexBuilder.buildHexaCommand(command.getRequest().getPayloads(), data))
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
                                                stopTimeout();
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
                            Log.d(TAG,"Sent -- responseFrame="+responseFrame);
                            if(command.getResponse()==null) {
                                stopTimeout();
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

    @Override
    public Single<Boolean> isBonded(Device device) {
        return hardwareInteractionLayer.isBonded(device.getMacAddress());
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
}
