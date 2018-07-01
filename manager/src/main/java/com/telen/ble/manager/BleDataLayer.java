package com.telen.ble.manager;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleScanResult;
import com.polidea.rxandroidble2.Timeout;
import com.telen.ble.manager.data.Command;
import com.telen.ble.manager.data.Device;
import com.telen.ble.manager.data.Payload;
import com.telen.ble.manager.exceptions.CommandTimeoutException;
import com.telen.ble.manager.interfaces.BleDataLayerInterface;
import com.telen.ble.manager.interfaces.HardwareLayerInterface;
import com.telen.ble.manager.validator.DataValidator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

public class BleDataLayer implements BleDataLayerInterface {

    private static final String TAG = BleDataLayer.class.getSimpleName();

    private long TIMEOUT_MILLIS = 30000l;

    private Map<Device, RxBleDevice> devices = new HashMap<>();
    private Map<Device, CompositeDisposable> devicesDisposable = new HashMap<>();
    private Map<Device, RxBleConnection> devicesConnection = new HashMap<>();
    private CompositeDisposable timeoutDisposable = new CompositeDisposable();

    private HardwareLayerInterface hardwareInteractionLayer;
    private DataValidator dataValidator;

    public BleDataLayer(HardwareLayerInterface hardwareLayer, DataValidator validator) {
        this.hardwareInteractionLayer = hardwareLayer;
        this.dataValidator = validator;
    }


    @Override
    public Single<Device> connect(String deviceName) {

        return Single.create(emitter -> {
            hardwareInteractionLayer.scan(deviceName)
                    .firstElement()
                    .toSingle()
                    .flatMapCompletable(rxBleScanResult -> hardwareInteractionLayer.connect(rxBleScanResult.getBleDevice())
                            .flatMapCompletable(rxBleConnection -> {
                                Device device = new Device(deviceName, rxBleScanResult.getBleDevice().getMacAddress());
                                devicesConnection.put(device, rxBleConnection);
                                devices.put(device, rxBleScanResult.getBleDevice());

                                CompositeDisposable composite = devicesDisposable.remove(device);
                                if(composite!=null && !composite.isDisposed())
                                    composite.clear();

                                devicesDisposable.put(device, new CompositeDisposable());
                                emitter.onSuccess(device);
                                return Completable.complete();
                            })
                            .subscribeOn(Schedulers.io())
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        });

//        return hardwareInteractionLayer.scanOld(deviceName)
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnNext(scanResult -> Log.d(TAG,"device found: "+scanResult.getBleDevice().getName()))
//                .firstElement()
//                .toSingle()
//                .flatMap(scanResult -> hardwareInteractionLayer.connect(scanResult.getBleDevice())
//                        .flatMap(rxBleConnection -> {
//                            Device device = new Device(deviceName, scanResult.getBleDevice().getMacAddress());
//                            devicesConnection.put(device, rxBleConnection);
//                            devices.put(device, scanResult.getBleDevice());
//
//                            CompositeDisposable composite = devicesDisposable.remove(device);
//                            if(composite!=null && !composite.isDisposed())
//                                composite.clear();
//
//                            devicesDisposable.put(device, new CompositeDisposable());
//                            return Single.just(device);
//                        })
//                )
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Completable disconnect(Device device) {
        return Completable.create(e -> {
            CompositeDisposable disposable = devicesDisposable.remove(device);
            if(disposable!=null && !disposable.isDisposed())
                devicesDisposable.clear();
            devicesConnection.remove(device);
            devices.remove(device);
            e.onComplete();
        });
    }

    @Override
    public Observable<String> sendCommand(Device device, Command command, Map<String, Object> data) {
        return Observable.create(e -> {
            RxBleConnection rxBleConnection = devicesConnection.get(device);
            //let's validate payloads and build the hexa string command
            dataValidator.validateData(command.getRequest().getPayloads(), data)
                    .andThen(buildHexaCommand(command.getRequest().getPayloads(), data))
                    .flatMap(hexaCommand -> {
                        startTimeout(e);
                        UUID uuid = UUID.fromString(command.getRequest().getCharacteristic());
                        return hardwareInteractionLayer.sendCommand(rxBleConnection, uuid, hexaCommand);
                    })
                    .flatMapObservable(responseFrame -> {
                        stopTimeout();
                        Log.d(TAG,"Sent -- responseFrame="+responseFrame);
                        //if we expect some response from remote device, we listen for any response
                        if(command.getResponse()!=null)
                            return hardwareInteractionLayer
                                    .listenResponses(rxBleConnection, UUID.fromString(command.getResponse().getCharacteristic()))
                                    .flatMap(response -> dataValidator.validateData(command.getResponse().getPayloads(), response)
                                            .andThen(Observable.just(response))
                                    );
                        else
                            return Observable.empty();
                    })
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            CompositeDisposable disposable = devicesDisposable.get(device);
                            if(disposable==null)
                            {
                                disposable = new CompositeDisposable();
                                devicesDisposable.put(device, disposable);
                            }
                            disposable.add(d);
                        }

                        @Override
                        public void onNext(String s) {
                            e.onNext(s);
                        }

                        @Override
                        public void onError(Throwable error) {
                            e.onError(error);
                        }

                        @Override
                        public void onComplete() {
                            e.onComplete();
                        }
                    });
        });
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Single<String> buildHexaCommand(@NonNull List<Payload> payloads, @NonNull Map<String, Object> data) {
        return Single.create(emitter -> {
            String[] commandArray = new String[20];
            Arrays.fill(commandArray, "00");

            for (Payload payload: payloads) {
                String identifier = payload.getName();
                Object obj = data.get(identifier);
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
//        if(TIMEOUT_MILLIS>0) {
//            timeoutDisposable.add(Observable.timer(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).subscribe(
//                    aLong -> {
//                    },
//                    throwable -> {
//                    },
//                    () -> {
//                        if(emitter!=null && !emitter.isDisposed())
//                            emitter.onError(new CommandTimeoutException("Command timeout triggered"));
//                    }
//                    )
//            );
//        }
    }

    private void stopTimeout() {
        timeoutDisposable.clear();
    }

    public void setTimeout(long timeout) {
        this.TIMEOUT_MILLIS = timeout;
    }

    public Map<Device, RxBleConnection> getRxConnections() {
        return devicesConnection;
    }

    public Map<Device, CompositeDisposable> getDevicesDisposable() {
        return devicesDisposable;
    }

    public Map<Device, RxBleDevice> getDevices() {
        return devices;
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
