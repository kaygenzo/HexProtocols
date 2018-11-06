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
import com.telen.sdk.demo.R;
import com.telen.sdk.common.utils.ColorUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class CardViewLightRibbon extends CardView {

    private static final String TAG = CardViewLightRibbon.class.getSimpleName();

    private LightRibbon lightRibbon;

    private TextView information;
    private Button connect;
    private Button on;
    private Button off;
    private SeekBar redSlide;
    private SeekBar greenSlide;
    private SeekBar blueSlide;
    private SeekBar luminositySlide;
    private Button colorPicker;

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
        connect = findViewById(R.id.connect);
        colorPicker = findViewById(R.id.color_picker);

        lightRibbon = new LightRibbon(getContext());

        connect.setOnClickListener(view -> {
            lightRibbon.connect(null, false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        });

        on.setOnClickListener(view -> {
            lightRibbon.lightOn()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {

                    }, throwable -> {
                        Log.d(TAG, "", throwable);
                    });
        });

        off.setOnClickListener(view -> {
            lightRibbon.lightOff()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
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
                        lightRibbon.changeColor(rgb[0], rgb[1], rgb[2])
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe();
                    })
                    .build()
                    .show();
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
                lightRibbon.changeColor(red, green, blue)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
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
                lightRibbon.changeColor(red, green, blue)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
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
                lightRibbon.changeColor(red, green, blue)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
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
