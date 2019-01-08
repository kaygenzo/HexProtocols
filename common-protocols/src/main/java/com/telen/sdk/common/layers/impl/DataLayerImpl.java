package com.telen.sdk.common.layers.impl;

import android.util.Log;

import com.telen.sdk.common.builder.CommandBuilder;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.common.layers.HardwareLayerInterface;
import com.telen.sdk.common.models.Command;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.Response;
import com.telen.sdk.common.validator.DataValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DataLayerImpl<T extends HardwareLayerInterface> implements DataLayerInterface<T> {

    private static final String TAG = DataLayerImpl.class.getSimpleName();

    private static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 5000;
    private static final long DEFAULT_RESPONSE_TIMEOUT_MILLIS = 3000;

    private Map<String, CompositeDisposable> dataListenerDisposableMap = new HashMap<>();

    private T hardwareInteractionLayer;
    private DataValidator dataValidator;
    private CommandBuilder commandBuilder;

    private long mRequestTimeout = DEFAULT_REQUEST_TIMEOUT_MILLIS;
    private long mResponseTimeout = DEFAULT_RESPONSE_TIMEOUT_MILLIS;

    public DataLayerImpl(T hardwareLayer, DataValidator validator, CommandBuilder commandBuilder) {
        this.hardwareInteractionLayer = hardwareLayer;
        this.dataValidator = validator;
        this.commandBuilder = commandBuilder;
    }

    @Override
    public Single<Device> scan(String deviceName) {
        return Single.create(emitter -> hardwareInteractionLayer.scan(deviceName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<Device> connect(Device device, boolean bind) {
        return Single.create(emitter ->
                hardwareInteractionLayer.connect(device, bind)
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

            if(!dataListenerDisposableMap.containsKey(command.getIdentifier())) {
                dataListenerDisposableMap.put(command.getIdentifier(), new CompositeDisposable());
            }

            final CompositeDisposable dataListenerDisposable = dataListenerDisposableMap.get(command.getIdentifier());
            dataListenerDisposable.clear();

            //let's validate payloads and build the hexa string command
            dataValidator.validateData(command.getRequest().getPayloads(), data)
                    .andThen(hardwareInteractionLayer.prepareBeforeSendingCommand(command.getRequest()))
                    .andThen(commandBuilder.dataCommandBuilder(command.getRequest().getPayloads(), data, command.getRequest().getLength()))
                    .flatMap(hexaCommand -> {

                        if(command.getRequest().getTimeout() > 0)
                            mRequestTimeout = command.getRequest().getTimeout();

                        //if we expect some response from remote device, we listen for any response before sending command
                        if(command.getResponse()!=null) {

                            if(command.getResponse().getTimeout() > 0)
                                mResponseTimeout = command.getResponse().getTimeout();

                            Observable<String> responseObservable = observe(device, command.getResponse());
                            //only for testing because tests are synchronous
                            if(mResponseTimeout >= 0) {
                                responseObservable = responseObservable
                                        .timeout(mResponseTimeout, TimeUnit.MILLISECONDS)
                                        .onErrorResumeNext(throwable -> {
                                            if(command.getResponse().isCompleteOnTimeout())
                                                return Observable.empty();
                                            else
                                                return Observable.error(throwable);
                                        });
                            }

                            Disposable listenerDisposable = responseObservable.subscribe(s -> {
                                emitter.onNext(s);

                                String endFrame = command.getResponse().getEndFrame();
                                if(endFrame !=null && endFrame.replace(" ","").equals(s)) {
                                    emitter.onComplete();
                                    dataListenerDisposable.clear();
                                }
                            }, throwable -> {
                                if(!emitter.isDisposed())
                                    emitter.onError(throwable);
                            }, () -> {
                                if(!emitter.isDisposed())
                                    emitter.onComplete();
                            });
                            dataListenerDisposable.add(listenerDisposable);
                        }

                        Single<String> requestObservable = hardwareInteractionLayer.sendCommand(device, command.getRequest(), hexaCommand);
                        //only for testing because tests are synchronous
                        if(mRequestTimeout >= 0) {
                            requestObservable = requestObservable
                                    .timeout(mRequestTimeout, TimeUnit.MILLISECONDS);
                        }

                        return requestObservable;

                    })
                    .subscribe(new SingleObserver<String>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(String responseFrame) {
                            Log.d(TAG,"Sent -- responseFrame="+responseFrame);
                            if(command.getResponse()==null) {
                                Log.d(TAG,"Don't need to wait for any response, bye bye!");
                                if(!emitter.isDisposed())
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
    public Observable<String> sendCommand(Device device, Command command) {
        return sendCommand(device, command, null);
    }

    @Override
    public Single<Boolean> isConnected(Device device) {
        return hardwareInteractionLayer.isConnected(device);
    }

    @Override
    public Observable<String> observe(Device device, Response response) {
        return hardwareInteractionLayer.listenResponses(device, response)
                .flatMap(responseFrame ->
                        dataValidator.validateData(response.getFrames(), responseFrame)
                                .andThen(Observable.just(responseFrame))
                );
    }

    public void setRequestTimeout(long timeout) {
        this.mRequestTimeout = timeout;
    }

    public void setResponseTimeout(long timeout) {
        this.mResponseTimeout = timeout;
    }
}
