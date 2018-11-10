package com.telen.sdk.demo.devices.ribbon;

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
import com.telen.sdk.common.models.RequestType;
import com.telen.sdk.demo.R;
import com.telen.sdk.common.utils.ColorUtils;
import com.telen.sdk.socket.devices.SocketDevice;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CardViewLightRibbon extends CardView {

    private static final String TAG = CardViewLightRibbon.class.getSimpleName();

    private LightRibbon lightRibbon;

    private TextView information;
    private Button connect_tcp;
    private Button connect_udp;
    private Button on;
    private Button off;
    private SeekBar redSlide;
    private SeekBar greenSlide;
    private SeekBar blueSlide;
    private SeekBar luminositySlide;
    private Button colorPicker;
    private Button scan;
    private Button disconnect;


    private CompositeDisposable slideDisposable = new CompositeDisposable();
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
        LayoutInflater.from(context).inflate(R.layout.cardview_light_ribbon,this);
        redSlide = findViewById(R.id.red);
        greenSlide = findViewById(R.id.green);
        blueSlide = findViewById(R.id.blue);
        luminositySlide = findViewById(R.id.luminosity);
        information = findViewById(R.id.information);
        on = findViewById(R.id.light_on);
        off = findViewById(R.id.light_off);
        connect_tcp = findViewById(R.id.connect_tcp);
        connect_udp = findViewById(R.id.connect_udp);
        colorPicker = findViewById(R.id.color_picker);
        scan = findViewById(R.id.scan);
        disconnect = findViewById(R.id.disconnect);

        lightRibbon = new LightRibbon(getContext());

        final SocketDevice mDevice = new SocketDevice(LightRibbon.class.getSimpleName());

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
                        Log.d(TAG, "", throwable);
                    });
        });

        off.setOnClickListener(view -> {
            lightRibbon.lightOff(mDevice)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {

                    }, throwable -> {
                        Log.d(TAG, "", throwable);
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
                                    Log.d(TAG, "", throwable);
                                });
                    })
                    .build()
                    .show();
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

        redSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                red = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                lightRibbon.changeColor(mDevice, red, green, blue)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {

                        }, throwable -> {
                            Log.d(TAG, "", throwable);
                        });
            }
        });

        greenSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                green = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                lightRibbon.changeColor(mDevice, red, green, blue)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {

                        }, throwable -> {
                            Log.d(TAG, "", throwable);
                        });
            }
        });

        blueSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                blue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                lightRibbon.changeColor(mDevice, red, green, blue)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {

                        }, throwable -> {
                            Log.d(TAG, "", throwable);
                        });
            }
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
}
