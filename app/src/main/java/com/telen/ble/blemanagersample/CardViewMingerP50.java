package com.telen.ble.blemanagersample;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.telen.ble.blemanagersample.devices.minger.Minger_P50;
import com.telen.ble.sdk.model.Device;

import io.reactivex.CompletableObserver;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class CardViewMingerP50 extends CardView {

    private static final String TAG = CardViewMingerP50.class.getSimpleName();

    private Minger_P50 minger_p50;

    private TextView information;
    private Button connect;
    private Button scan;
    private Button bond;
    private Button disconnect;
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

        disconnect.setEnabled(false);
        connect.setEnabled(false);
        bond.setEnabled(false);

        minger_p50 = new Minger_P50(context);

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
                            information.setText(device.toString());
                            disconnect.setEnabled(true);
                            connect.setEnabled(true);
                            bond.setEnabled(true);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "", e);
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
                        Log.e(TAG,"",e);
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
                        Log.e(TAG,"",e);
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
                        Log.e(TAG,"",e);
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
}
