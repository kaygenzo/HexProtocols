package com.telen.sdk.demo.commonui.devices.ribbon;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.telen.sdk.common.models.DeviceType;
import com.telen.sdk.common.utils.ColorUtils;
import com.telen.sdk.demo.commonui.FirestoreManager;
import com.telen.sdk.demo.commonui.ILightActions;
import com.telen.sdk.demo.commonui.R;
import com.telen.sdk.demo.commonui.di.DaggerCommonUiWrapper;
import com.telen.sdk.socket.devices.SocketDevice;
import com.telen.sdk.socket.models.RequestType;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CardViewLightRibbon extends CardView implements ILightActions {

    private static final String TAG = CardViewLightRibbon.class.getSimpleName();

    private LightRibbon lightRibbon;

    private TextView information;
    private Button connect_tcp;
    private Button connect_udp;
    private Button on;
    private Button off;
    private SeekBar luminositySlide;
    private Button colorPicker;
    private Button scan;
    private Button disconnect;
    private Button process;

    @Inject FirestoreManager mFirestoreManager;
    final SocketDevice mDevice = new SocketDevice(LightRibbon.class.getSimpleName());

    private CompositeDisposable slideDisposable = new CompositeDisposable();
    private CompositeDisposable firestoreActionDisposable = new CompositeDisposable();

    private int red;
    private int green;
    private int blue;
    private int luminosity;

    public CardViewLightRibbon(Context context) {
        super(context);
        initView(context);
    }

    public CardViewLightRibbon(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public CardViewLightRibbon(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {

        DaggerCommonUiWrapper.getComponent(context).inject(this);
        mFirestoreManager.registerLightCallback(DeviceType.RIBBON, this);

        LayoutInflater.from(context).inflate(R.layout.cardview_light_ribbon,this);
        luminositySlide = findViewById(R.id.luminosity);
        information = findViewById(R.id.information);
        on = findViewById(R.id.light_on);
        off = findViewById(R.id.light_off);
        connect_tcp = findViewById(R.id.connect_tcp);
        connect_udp = findViewById(R.id.connect_udp);
        colorPicker = findViewById(R.id.color_picker);
        scan = findViewById(R.id.scan);
        disconnect = findViewById(R.id.disconnect);
        process = findViewById(R.id.process);

        lightRibbon = new LightRibbon(getContext());

        connect_tcp.setOnClickListener(view -> {
            lightRibbon.connect(mDevice, RequestType.tcp)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(device -> {
                        Log.d(TAG,"connected tcp");
                    }, throwable -> {
                        Log.e(TAG,"",throwable);
                    });
        });

        connect_udp.setOnClickListener(view -> {
            lightRibbon.connect(mDevice, RequestType.udp)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(device -> {
                        Log.d(TAG,"connected udp");
                    }, throwable -> {
                        Log.e(TAG,"",throwable);
                    });
        });

        disconnect.setOnClickListener(view -> {
            lightRibbon.disconnect(mDevice)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Log.d(TAG,"Disconnected from all sockets!");
                    }, throwable -> {
                        Log.e(TAG,"",throwable);
                    });
        });

        on.setOnClickListener(view -> {
            lightRibbon.lightOn(mDevice)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {

                    }, throwable -> {
                        Log.e(TAG, "", throwable);
                    });
        });

        off.setOnClickListener(view -> {
            lightRibbon.lightOff(mDevice)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {

                    }, throwable -> {
                        Log.e(TAG, "", throwable);
                    });
        });

        colorPicker.setOnClickListener(view -> {
            ColorPickerDialogBuilder
                    .with(context)
                    .setTitle("Choose color")
                    .initialColor(Color.rgb(red, green, blue))
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setOnColorSelectedListener(selectedColor -> {
                        int[] rgb = ColorUtils.getRGB(selectedColor);
                        lightRibbon.changeColor(mDevice, rgb[0], rgb[1], rgb[2])
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(() -> {

                                }, throwable -> {
                                    Log.e(TAG, "", throwable);
                                });
                    })
                    .build()
                    .show();
        });

        process.setOnClickListener(view -> {
            mDevice.setType(RequestType.tcp);
            Disposable disposable = lightRibbon.isConnected(mDevice)
                    .flatMapCompletable(connected -> {
                        if(connected) {
                            Log.d(TAG, "already connected!");
                            return Completable.complete();
                        }
                        else {
                            return lightRibbon.connect(mDevice, RequestType.udp)
                                    .flatMapCompletable(device -> {
                                        Log.d(TAG,"process:step 1");
                                        return Completable.complete();
                                    })
                                    .andThen(lightRibbon.scan())
                                    .flatMap(device -> {
                                        Log.d(TAG,"process:step 2");
                                        SocketDevice updatedDevice = (SocketDevice)device;
                                        mDevice.setAddress(updatedDevice.getAddress());
                                        Log.d(TAG,mDevice.toString());
                                        return lightRibbon.connect(mDevice, RequestType.tcp);
                                    })
                                    .flatMapCompletable(device -> Completable.complete())
                                    ;
                        }
                    }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Log.d(TAG,"Process finished!");
                    }, throwable -> {
                        Log.e(TAG,"", throwable);
                    });
        });

        scan.setOnClickListener(view -> {
            lightRibbon.scan()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(device -> {
                        SocketDevice socketDevice = (SocketDevice)device;
                        mDevice.setAddress(socketDevice.getAddress());
                        Log.d(TAG,mDevice.toString());
                    }, throwable -> {
                        Log.e(TAG,"", throwable);
                    });
        });

        luminositySlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int value = seekBar.getProgress();
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
    public void lightOn() {
        firestoreActionDisposable.clear();
        Disposable disposable = scanIfNecessary()
        .andThen(lightRibbon.lightOn(mDevice))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {

                }, throwable -> {
                    Log.e(TAG, "", throwable);
                });
        firestoreActionDisposable.add(disposable);
    }

    @Override
    public void lightOff() {
        firestoreActionDisposable.clear();
        Disposable disposable = scanIfNecessary()
                .andThen(lightRibbon.lightOff(mDevice))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {

                }, throwable -> {
                    Log.e(TAG, "", throwable);
                });
        firestoreActionDisposable.add(disposable);
    }

    @Override
    public void setColor(int color) {
        firestoreActionDisposable.clear();
        int[] rgb = ColorUtils.getRGB(color);
        Disposable disposable = scanIfNecessary()
                .andThen(lightRibbon.changeColor(mDevice, rgb[0], rgb[1], rgb[2]))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {

                }, throwable -> {
                    Log.e(TAG, "", throwable);
                });
        firestoreActionDisposable.add(disposable);
    }

    private Completable scanIfNecessary() {
        mDevice.setType(RequestType.tcp);
        return lightRibbon.isConnected(mDevice)
                .flatMapCompletable(connected -> {
                    if(connected) {
                        Log.d(TAG, "already connected!");
                        return Completable.complete();
                    }
                    else {
                        return lightRibbon.connect(mDevice, RequestType.udp)
                                .flatMapCompletable(device -> {
                                    Log.d(TAG,"process:step 1");
                                    return Completable.complete();
                                })
                                .andThen(lightRibbon.scan())
                                .flatMap(device -> {
                                    Log.d(TAG,"process:step 2");
                                    SocketDevice updatedDevice = (SocketDevice)device;
                                    mDevice.setAddress(updatedDevice.getAddress());
                                    Log.d(TAG,mDevice.toString());
                                    return lightRibbon.connect(mDevice, RequestType.tcp);
                                })
                                .flatMapCompletable(device -> Completable.complete())
                                ;
                    }
                }).subscribeOn(Schedulers.io());
    }
}
