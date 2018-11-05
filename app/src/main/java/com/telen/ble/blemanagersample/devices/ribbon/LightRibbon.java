package com.telen.ble.blemanagersample.devices.ribbon;

import android.os.Build;

import com.telen.ble.blemanagersample.BuildConfig;
import com.telen.ble.blemanagersample.pending.ChangeColorCMD;
import com.telen.ble.blemanagersample.pending.CommandResponse;
import com.telen.ble.blemanagersample.pending.DeviceCommand;
import com.telen.ble.blemanagersample.pending.DevicesCMD;
import com.telen.ble.blemanagersample.pending.LightOffCMD;
import com.telen.ble.blemanagersample.pending.LightOnCMD;
import com.telen.ble.blemanagersample.pending.MagicHueService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class LightRibbon {
    private static final String OS = "Android";
    private static final String APP_VERSION = "8.0.0";
    private static final String MAC_ADDRESS = "DC4F22C0D904";
    private MagicHueService service;

    public LightRibbon(Class<MagicHueService> serviceClass) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://wifi.magichue.net/WebMagicHome/api/")
                .client(new OkHttpClient.Builder().addInterceptor(interceptor).build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.service = retrofit.create(serviceClass);
    }

    public Single<CommandResponse> lightOn() {
        DevicesCMD devicesCMD = new DevicesCMD.Builder<>(LightOnCMD.class)
                .setMacAddress(MAC_ADDRESS)
                .build();

        List<DevicesCMD> listCmd = new ArrayList<>();
        listCmd.add(devicesCMD);

        DeviceCommand command = new DeviceCommand.Builder()
                .setAppSys(OS)
                .setAppVer(APP_VERSION)
                .setTimestamp(System.currentTimeMillis())
                .setDevicesCMDs(listCmd)
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("cookie",".ASPXAUTH=190F04ED2015978F33B0CF13DA2B161D8216A45A4E8E4AB650FFD0E3C9DB751CF6C21AC5854D63C8F8B895CD952C1923F281593956BD5D88004134974EE5D76A422A1AEE379B20621FD70B9AC540A34867884D6360C1A79ADCB783A4CECC05BC83C4248E506CEFAC9D4D3A921A73EA0218ECD75226494A2560639BCC71583B46");
        return service.lightOff(headers, "ZG001", command)
                .subscribeOn(Schedulers.io());
    }

    public Single<CommandResponse> lightOff() {
        DevicesCMD devicesCMD = new DevicesCMD.Builder<>(LightOffCMD.class)
                .setMacAddress(MAC_ADDRESS)
                .build();

        List<DevicesCMD> listCmd = new ArrayList<>();
        listCmd.add(devicesCMD);

        DeviceCommand command = new DeviceCommand.Builder()
                .setAppSys(OS)
                .setAppVer(APP_VERSION)
                .setTimestamp(System.currentTimeMillis())
                .setDevicesCMDs(listCmd)
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("cookie",".ASPXAUTH=190F04ED2015978F33B0CF13DA2B161D8216A45A4E8E4AB650FFD0E3C9DB751CF6C21AC5854D63C8F8B895CD952C1923F281593956BD5D88004134974EE5D76A422A1AEE379B20621FD70B9AC540A34867884D6360C1A79ADCB783A4CECC05BC83C4248E506CEFAC9D4D3A921A73EA0218ECD75226494A2560639BCC71583B46");
        return service.lightOff(headers, "ZG001", command)
                .subscribeOn(Schedulers.io());
    }

    public Single<CommandResponse> changeColor(int red, int green, int blue) {

        Map<String, Integer> data = new HashMap<>();
        data.put("RED", red);
        data.put("GREEN", green);
        data.put("BLUE", blue);

        DevicesCMD devicesCMD = new DevicesCMD.Builder<>(ChangeColorCMD.class)
                .setMacAddress(MAC_ADDRESS)
                .withData(data)
                .build();

        List<DevicesCMD> listCmd = new ArrayList<>();
        listCmd.add(devicesCMD);

        DeviceCommand command = new DeviceCommand.Builder()
                .setAppSys(OS)
                .setAppVer(APP_VERSION)
                .setTimestamp(System.currentTimeMillis())
                .setDevicesCMDs(listCmd)
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("cookie",".ASPXAUTH=190F04ED2015978F33B0CF13DA2B161D8216A45A4E8E4AB650FFD0E3C9DB751CF6C21AC5854D63C8F8B895CD952C1923F281593956BD5D88004134974EE5D76A422A1AEE379B20621FD70B9AC540A34867884D6360C1A79ADCB783A4CECC05BC83C4248E506CEFAC9D4D3A921A73EA0218ECD75226494A2560639BCC71583B46");
        return service.changeColor(headers, "ZG001", command)
                .subscribeOn(Schedulers.io());
    }
}
