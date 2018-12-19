package com.telen.sdk.demo.devices.minger;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.telen.sdk.common.devices.GenericDevice;
import com.telen.sdk.common.models.DeviceType;
import com.telen.sdk.common.utils.ColorUtils;
import com.telen.sdk.demo.Constants;
import com.telen.sdk.demo.DaggerApplicationWrapper;
import com.telen.sdk.demo.FirestoreManager;
import com.telen.sdk.demo.ILightActions;
import com.telen.sdk.demo.R;
import com.telen.sdk.common.models.Device;

import javax.inject.Inject;

import io.reactivex.CompletableObserver;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CardViewMingerP50 extends CardView implements ILightActions {

    private static final String TAG = CardViewMingerP50.class.getSimpleName();

    private Minger_P50 minger_p50;

    private TextView information;
    private Button connect;
    private Button scan;
    private Button bond;
    private Button disconnect;
    private Button save;
    private SeekBar redSlide;
    private SeekBar greenSlide;
    private SeekBar blueSlide;
    private SeekBar luminositySlide;

    private Device mDevice;
    private CompositeDisposable slideDisposable = new CompositeDisposable();
    private int red;
    private int green;
    private int blue;
    private int luminosity;
    @Inject FirestoreManager mFirestoreManager;

    public CardViewMingerP50(Context context) {
        super(context);
        initView(context);
    }

    public CardViewMingerP50(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public CardViewMingerP50(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {

        DaggerApplicationWrapper.getComponent(context).inject(this);
        mFirestoreManager.registerLightCallback(DeviceType.LIGHTBULB,this);

        LayoutInflater.from(context).inflate(R.layout.cardview_minger_p50,this);
        connect = findViewById(R.id.connect);
        scan = findViewById(R.id.scan);
        disconnect = findViewById(R.id.disconnect);
        bond = findViewById(R.id.bond);
        redSlide = findViewById(R.id.red);
        greenSlide = findViewById(R.id.green);
        blueSlide = findViewById(R.id.blue);
        luminositySlide = findViewById(R.id.luminosity);
        information = findViewById(R.id.information);
        save = findViewById(R.id.save);

        disconnect.setEnabled(false);
        connect.setEnabled(false);
        bond.setEnabled(false);

        redSlide.setProgress(red);
        greenSlide.setProgress(green);
        blueSlide.setProgress(blue);
        luminositySlide.setProgress(luminosity);

        minger_p50 = new Minger_P50(context);

        final SharedPreferences prefs = getContext().getSharedPreferences(Constants.PREFS_APPLICATION, 0);
        String lastDeviceName = prefs.getString(Constants.PREF_MINGER_NAME, null);
        String lastMacAddress = prefs.getString(Constants.PREF_MINGER_MAC, null);
        if(lastDeviceName != null && lastMacAddress != null) {
            mDevice = new Device(lastDeviceName, lastMacAddress);
            deviceReady(mDevice);
        }

        Disposable disposable = minger_p50.isConnected(mDevice)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isConnected -> {
                    Toast.makeText(getContext(), "is connected ? "+isConnected, Toast.LENGTH_LONG).show();
                }, throwable -> {
                    Log.e(TAG,"Minger::isConnected", throwable);
                });

        save.setOnClickListener(view -> {
            if(mDevice!=null) {
                SharedPreferences.Editor ed = prefs.edit();
                ed.putString(Constants.PREF_MINGER_NAME, mDevice.getName());
                ed.putString(Constants.PREF_MINGER_MAC, mDevice.getMacAddress());
                ed.apply();
            }
        });

        scan.setOnClickListener(view -> {
            information.setText("");
            minger_p50.scan()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Device>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Device device) {
                            CardViewMingerP50.this.mDevice = device;
                            deviceReady(device);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "Minger::scan", e);
                        }
                    });
        });

        connect.setOnClickListener(view -> minger_p50.connect(mDevice, false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Device>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Device device) {
                        mDevice = device;
                        Toast.makeText(context, "Successfully connected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::connect",e);
                    }
                }));

        bond.setOnClickListener(view -> minger_p50.connect(mDevice, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Device>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Device device) {
                        mDevice = device;
                        Toast.makeText(context, "Successfully connected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::connect",e);
                    }
                }));

        disconnect.setOnClickListener(view -> minger_p50.disconnect(mDevice)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(context, "Successfully disconnected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"Minger::disconnect",e);
                    }
                })
        );

        redSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int value = seekBar.getProgress();
                minger_p50.apply(mDevice, value, green, blue, luminosity)
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
                                red = value;
                            }
                        });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        greenSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int value = seekBar.getProgress();
                minger_p50.apply(mDevice, red, value, blue, luminosity)
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
                                green = value;
                            }
                        });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        blueSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int value = seekBar.getProgress();
                minger_p50.apply(mDevice, red, green, value, luminosity)
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
                                blue = value;
                            }
                        });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        luminositySlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int value = seekBar.getProgress();
                minger_p50.apply(mDevice, red, green, blue, value)
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
                                luminosity = value;
                            }
                        });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void deviceReady(Device device) {
        information.setText(device.toString());
        disconnect.setEnabled(true);
        connect.setEnabled(true);
        bond.setEnabled(true);
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

    private Single<Device> scanIfNeeded(GenericDevice genericDevice) {
        return  Single.create(emitter -> {
            if(mDevice!=null)
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
}
