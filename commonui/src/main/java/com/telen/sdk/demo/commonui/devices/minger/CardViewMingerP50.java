package com.telen.sdk.demo.commonui.devices.minger;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.telen.sdk.common.models.Device;
import com.telen.sdk.demo.commonui.R;
import com.telen.sdk.demo.commonui.di.DaggerCommonUiWrapper;

import javax.inject.Inject;

public class CardViewMingerP50 extends CardView {

    private static final String TAG = CardViewMingerP50.class.getSimpleName();

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

    @Inject MingerManager mMingerManager;

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
        DaggerCommonUiWrapper.getComponent(context).inject(this);

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

        mMingerManager.init();

        int[] colorArray = mMingerManager.getColorConfiguration();
        redSlide.setProgress(colorArray[MingerManager.RED_INDEX]);
        greenSlide.setProgress(colorArray[MingerManager.GREEN_INDEX]);
        blueSlide.setProgress(colorArray[MingerManager.BLUE_INDEX]);
        luminositySlide.setProgress(colorArray[MingerManager.LUMINOSITY_INDEX]);

        if(mMingerManager.getDevice()!=null && !TextUtils.isEmpty(mMingerManager.getDevice().getMacAddress())) {
            deviceReady(mMingerManager.getDevice());
        }

        scan.setOnClickListener(view -> {
            information.setText("");
            mMingerManager.scan(this::deviceReady);
        });
        save.setOnClickListener(view -> mMingerManager.save());
        connect.setOnClickListener(view -> mMingerManager.connect());
        bond.setOnClickListener(view -> mMingerManager.bond());
        disconnect.setOnClickListener(view -> mMingerManager.disconnect());

        redSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int value = seekBar.getProgress();
                mMingerManager.updateRed(value);
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
                mMingerManager.updateGreen(value);
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
                mMingerManager.updateBlue(value);
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
                mMingerManager.updateLuminosity(value);
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

}
