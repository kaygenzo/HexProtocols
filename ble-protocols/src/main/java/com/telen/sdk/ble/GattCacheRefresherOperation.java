package com.telen.sdk.ble;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleCustomOperation;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

public class GattCacheRefresherOperation implements RxBleCustomOperation<Boolean> {
    private static final String TAG = GattCacheRefresherOperation.class.getSimpleName();
    @NonNull
    @Override
    public Observable<Boolean> asObservable(BluetoothGatt bluetoothGatt,
                                         RxBleGattCallback rxBleGattCallback,
                                         Scheduler scheduler) throws Throwable {

        return Observable.fromCallable(() -> refreshDeviceCache(bluetoothGatt))
                .delay(500, TimeUnit.MILLISECONDS, Schedulers.computation()) // << *** Add this line ***
                .subscribeOn(scheduler);
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        // from http://stackoverflow.com/questions/22596951/how-to-programmatically-force-bluetooth-low-energy-service-discovery-on-android
        try {
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = (Boolean) localMethod.invoke(gatt, new Object[0]);
                Log.i(TAG,"Gatt cache refresh successful: [" + bool + "]");
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device", localException);
        }
        return false;
    }
}
