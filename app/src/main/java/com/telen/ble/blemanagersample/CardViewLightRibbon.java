package com.telen.ble.blemanagersample;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.telen.ble.blemanagersample.devices.ribbon.LightRibbon;
import com.telen.ble.blemanagersample.pending.MagicHueService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class CardViewLightRibbon extends CardView {

    private static final String TAG = CardViewLightRibbon.class.getSimpleName();

    private LightRibbon lightRibbon;

    private TextView information;
    private Button on;
    private Button off;
    private SeekBar redSlide;
    private SeekBar greenSlide;
    private SeekBar blueSlide;
    private SeekBar luminositySlide;

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


        lightRibbon = new LightRibbon(MagicHueService.class);

        on.setOnClickListener(view -> {
            lightRibbon.lightOn()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        });

        off.setOnClickListener(view -> {
            lightRibbon.lightOff()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
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
