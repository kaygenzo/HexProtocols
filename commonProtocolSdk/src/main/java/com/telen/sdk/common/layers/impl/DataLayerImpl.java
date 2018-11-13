package com.telen.sdk.common.layers.impl;

import android.util.Log;

import com.telen.sdk.common.builder.CommandBuilder;
import com.telen.sdk.common.exceptions.CommandTimeoutException;
import com.telen.sdk.common.layers.DataLayerInterface;
import com.telen.sdk.common.layers.HardwareLayerInterface;
import com.telen.sdk.common.models.Command;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.Response;
import com.telen.sdk.common.validator.DataValidator;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DataLayerImpl<T extends HardwareLayerInterface> implements DataLayerInterface<T> {

    private static final String TAG = DataLayerImpl.class.getSimpleName();

    private static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 5000;
    private static final long DEFAULT_RESPONSE_TIMEOUT_MILLIS = 3000;

    private CompositeDisposable mRequestTimeoutDisposable = new CompositeDisposable();
    private CompositeDisposable mResponseTimeoutDisposable = new CompositeDisposable();
    private CompositeDisposable dataListenerDisposable = new CompositeDisposable();

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
            dataListenerDisposable.clear();
            //let's validate payloads and build the hexa string command
            dataValidator.validateData(command.getRequest().getPayloads(), data)
                    .andThen(hardwareInteractionLayer.prepareBeforeSendingCommand(command.getRequest()))
                    .andThen(commandBuilder.dataCommandBuilder(command.getRequest().getPayloads(), data, command.getRequest().getLength()))
                    .flatMap(hexaCommand -> {

                        if(command.getRequest().getTimeout() > 0)
                            mRequestTimeout = command.getRequest().getTimeout();
                        startRequestTimeout(emitter);

                        //if we expect some response from remote device, we listen for any response before sending command
                        if(command.getResponse()!=null) {

                            if(command.getResponse().getTimeout() > 0)
                                mResponseTimeout = command.getResponse().getTimeout();

                            startResponseTimeout(emitter, command.getResponse());

                            hardwareInteractionLayer
                                    .listenResponses(device, command.getResponse())
                                    .flatMap(response -> dataValidator.validateData(command.getResponse().getPayloads(), response)
                                            .andThen(Observable.just(response))
                                    )
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
                                                stopResponseTimeout();
                                                dataListenerDisposable.clear();
                                                emitter.onComplete();
                                            }
                                            else
                                                resetResponseTimeout(emitter, command.getResponse());
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
                        return hardwareInteractionLayer.sendCommand(device, command.getRequest(), hexaCommand);
                    })
                    .subscribe(new SingleObserver<String>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(String responseFrame) {
                            Log.d(TAG,"Sent -- responseFrame="+responseFrame);
                            stopRequestTimeout();
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

    @Override
    public Observable<String> sendCommand(Device device, Command command) {
        return sendCommand(device, command, null);
    }

    @Override
    public Single<Boolean> isConnected(Device device) {
        return hardwareInteractionLayer.isConnected(device);
    }

    private void startTimeout(final ObservableEmitter emitter, final CompositeDisposable disposable, final long timeout, final boolean isCompleteOnTimeout) {
        if(timeout>0) {
            disposable.add(
                    Observable.timer(timeout, TimeUnit.MILLISECONDS)
                            .subscribe(aLong -> {
                                    },
                                    throwable -> {
                                    },
                                    () -> {
                                        if(emitter!=null && !emitter.isDisposed()) {
                                            if(isCompleteOnTimeout) {
                                                Log.d(TAG, "Protocol define this timeout as normal so we just finish subscription");
                                                emitter.onComplete();
                                            }
                                            else
                                                emitter.onError(new CommandTimeoutException("Command timeout triggered"));
                                        }
                                    }
                            )
            );
        }
    }

    private void startRequestTimeout(ObservableEmitter emitter) {
        startTimeout(emitter, mRequestTimeoutDisposable, mRequestTimeout, false);
    }

    private void startResponseTimeout(@NonNull ObservableEmitter emitter, @NonNull Response reponse) {
        startTimeout(emitter, mResponseTimeoutDisposable, mResponseTimeout, reponse.isCompleteOnTimeout());
    }

    private void stopRequestTimeout() {
        mRequestTimeoutDisposable.clear();
    }

    private void stopResponseTimeout() {
        mResponseTimeoutDisposable.clear();
    }

    private void resetResponseTimeout(@NonNull ObservableEmitter emitter, @NonNull Response response) {
        stopResponseTimeout();
        startResponseTimeout(emitter, response);
    }

    public void setRequestTimeout(long timeout) {
        this.mRequestTimeout = timeout;
    }

    public void setResponseTimeout(long timeout) {
        this.mResponseTimeout = timeout;
    }
}
