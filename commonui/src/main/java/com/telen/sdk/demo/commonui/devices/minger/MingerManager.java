package com.telen.sdk.demo.commonui.devices.minger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.telen.sdk.common.devices.GenericDevice;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.DeviceType;
import com.telen.sdk.common.utils.ColorUtils;
import com.telen.sdk.demo.commonui.Constants;
import com.telen.sdk.demo.commonui.FirestoreManager;
import com.telen.sdk.demo.commonui.ILightActions;

import io.reactivex.CompletableObserver;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class MingerManager implements ILightActions {

    private static final String TAG = MingerManager.class.getSimpleName();

    private Minger_P50 minger_p50;
    private Device mDevice;

    public static final int RED_INDEX           = 0;
    public static final int GREEN_INDEX         = 1;
    public static final int BLUE_INDEX          = 2;
    public static final int LUMINOSITY_INDEX    = 3;

    private int red;
    private int green;
    private int blue;
    private int luminosity;

    public interface OnScanCompletedListener {
        void onScanSuccess(Device device);
    }

    private CompositeDisposable slideDisposable = new CompositeDisposable();

    private FirestoreManager mFirestoreManager;
    private Context mContext;
    final SharedPreferences prefs;

    public MingerManager(Context context, FirestoreManager mFirestoreManager, Minger_P50 minger) {
        this.mFirestoreManager = mFirestoreManager;
        this.minger_p50 = minger;
        this.mContext = context;
        prefs = mContext.getSharedPreferences(Constants.PREFS_APPLICATION, 0);
    }

    void init() {
        String lastDeviceName = prefs.getString(Constants.PREF_MINGER_NAME, null);
        String lastMacAddress = prefs.getString(Constants.PREF_MINGER_MAC, null);
        if(lastDeviceName != null && lastMacAddress != null) {
            mDevice = new Device(lastDeviceName, lastMacAddress);
        }

        mFirestoreManager.registerLightCallback(DeviceType.LIGHTBULB,this);
    }

    void destroy() {
        mFirestoreManager.unregisterLightCallback(DeviceType.LIGHTBULB, this);
    }

    void save() {
        if(mDevice!=null) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(Constants.PREF_MINGER_NAME, mDevice.getName());
            ed.putString(Constants.PREF_MINGER_MAC, mDevice.getMacAddress());
            ed.apply();
        }
    }

    void scan(OnScanCompletedListener listener) {
        minger_p50.scan()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Device>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Device device) {
                        MingerManager.this.mDevice = device;
                        if(listener!=null)
                            listener.onScanSuccess(device);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Minger::scan", e);
                    }
                });
    }

    void connect() {
        minger_p50.connect(mDevice, false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Device>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Device device) {
                        mDevice = device;
                        Toast.makeText(mContext, "Successfully connected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::connect",e);
                    }
                });
    }

    void bond() {
        minger_p50.connect(mDevice, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Device>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Device device) {
                        mDevice = device;
                        Toast.makeText(mContext, "Successfully connected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::connect",e);
                    }
                });
    }

    void disconnect() {
        minger_p50.disconnect(mDevice)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(mContext, "Successfully disconnected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::disconnect",e);
                    }
                });
    }

    @Override
    public void lightOn() {
        scanIfNeeded(minger_p50)
                .flatMap(device-> minger_p50.connect(device, false))
                .flatMapObservable(minger_p50::lighOn)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                }, throwable -> {
                    Log.e(TAG,"Minger::lightOn", throwable);
                });
    }

    @Override
    public void lightOff() {
        scanIfNeeded(minger_p50)
                .flatMap(device-> minger_p50.connect(device, false))
                .flatMapObservable(minger_p50::lighOff)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                }, throwable -> {
                    Log.e(TAG,"Minger::lightOff", throwable);
                });
    }

    @Override
    public void setColor(int color) {
        int[] rgbArray = ColorUtils.getRGB(color);
        int luminosity = color!=0 ? 0 : 255;
        scanIfNeeded(minger_p50)
                .flatMap(device-> minger_p50.connect(device, false))
                .flatMapObservable(device -> minger_p50.apply(device, rgbArray[0], rgbArray[1], rgbArray[2], luminosity))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                }, throwable -> {
                    Log.e(TAG,"Minger::setColor", throwable);
                });
    }

    void updateRed(int redColor) {
        minger_p50.apply(mDevice, redColor, green, blue, luminosity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        slideDisposable.clear();
                        slideDisposable.add(d);
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG,"responseFrame="+s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::apply",e);
                    }

                    @Override
                    public void onComplete() {
                        red = redColor;
                    }
                });
    }

    void updateGreen(int greenColor) {
        minger_p50.apply(mDevice, red, greenColor, blue, luminosity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        slideDisposable.clear();
                        slideDisposable.add(d);
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG,"responseFrame="+s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::apply",e);
                    }

                    @Override
                    public void onComplete() {
                        green = greenColor;
                    }
                });
    }

    void updateBlue(int blueColor) {
        minger_p50.apply(mDevice, red, green, blueColor, luminosity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        slideDisposable.clear();
                        slideDisposable.add(d);
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG,"responseFrame="+s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::apply",e);
                    }

                    @Override
                    public void onComplete() {
                        blue = blueColor;
                    }
                });
    }

    void updateLuminosity(int luminosityValue) {
        minger_p50.apply(mDevice, red, green, blue, luminosity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        slideDisposable.clear();
                        slideDisposable.add(d);
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG,"responseFrame="+s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::apply",e);
                    }

                    @Override
                    public void onComplete() {
                        luminosity = luminosityValue;
                    }
                });
    }

    private Single<Device> scanIfNeeded(GenericDevice genericDevice) {
        return  Single.create(emitter -> {
            if(mDevice!=null && !TextUtils.isEmpty(mDevice.getMacAddress()))
                emitter.onSuccess(mDevice);
            else
                genericDevice.scan()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(device -> {
                            mDevice = device;
                            emitter.onSuccess(device);
                        }, throwable -> {
                            emitter.onError(throwable);
                            Log.e(TAG,"Minger::scanIfNeeded",throwable);
                        });
        });
    }

    Device getDevice() {
        return mDevice;
    }

    int[] getColorConfiguration() {
        return new int[] {red, green, blue, luminosity};
    }
}
