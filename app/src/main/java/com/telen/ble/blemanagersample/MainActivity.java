package com.telen.ble.blemanagersample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import com.telen.ble.manager.data.Device;
import com.telen.ble.manager.devices.minger_p50.Minger_P50;

import io.reactivex.CompletableObserver;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 0;
    private static final String TAG = MainActivity.class.getSimpleName();

    private Device mDevice;
    private CompositeDisposable slideDisposable = new CompositeDisposable();
    private int red;
    private int green;
    private int blue;
    private int luminosity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_CODE);
        } else {
            permissionsGranted();
        }
    }

    private void permissionsGranted() {
        final Minger_P50 minger_p50 = new Minger_P50(this);
        //final SimulatorBLE simulator = new SimulatorBLE(this);
        findViewById(R.id.connect).setOnClickListener(view -> minger_p50.connect()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Device>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Device device) {
                        Log.d(TAG,"onSuccess device="+device);
                        mDevice = device;
                        Toast.makeText(MainActivity.this, "Successfully connected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"",e);
                    }
                }));

        findViewById(R.id.disconnect).setOnClickListener(view ->
                minger_p50.disconnect(mDevice)
                        .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(MainActivity.this, "Successfully disconnected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                })
        );

        ((SeekBar)findViewById(R.id.red)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int value = seekBar.getProgress();
                minger_p50.apply(mDevice, value, green, blue, luminosity)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<String>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                slideDisposable.clear();
                                slideDisposable.add(d);
                            }

                            @Override
                            public void onNext(String s) {
                                Log.e(TAG,"responseFrame="+s);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG,"",e);
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

        ((SeekBar)findViewById(R.id.green)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                                Log.e(TAG,"responseFrame="+s);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG,"",e);
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

        ((SeekBar)findViewById(R.id.blue)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                                Log.e(TAG,"responseFrame="+s);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG,"",e);
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

        ((SeekBar)findViewById(R.id.luminosity)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                                Log.e(TAG,"responseFrame="+s);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG,"",e);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                for (int permissionResult: grantResults) {
                    if (permissionResult == PackageManager.PERMISSION_DENIED)
                        return;
                }
                permissionsGranted();
        }
    }
}
