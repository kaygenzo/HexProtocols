package com.telen.sdk.common.models;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProtocolConfiguration {

    public static DeviceConfiguration parse(Context context, String protocolPath) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(protocolPath);
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            Gson gson = new Gson();
            return gson.fromJson(reader, DeviceConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        finally {
            if(inputStream!=null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
