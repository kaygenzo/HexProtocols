package com.telen.ble.sdk.builder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.telen.ble.sdk.model.Payload;
import com.telen.ble.sdk.model.PayloadType;
import com.telen.ble.sdk.utils.BytesUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;

public class HexBuilder {

    private static final String TAG = HexBuilder.class.getSimpleName();

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Single<String> buildHexaCommand(@NonNull List<Payload> payloads, @Nullable Map<String, Object> data) {
        return Single.create(emitter -> {
            String[] commandArray = new String[20];
            Arrays.fill(commandArray, "00");

            for (Payload payload: payloads) {
                String identifier = payload.getName();
                Object obj = null;
                if(data != null)
                    obj = data.get(identifier);
                if(obj==null)
                    obj = payload.getValue();
                int start = payload.getStart();
                int end = payload.getEnd();
                StringBuilder hexBuilder = new StringBuilder();

                String payloadTypeString = payload.getType();
                try {
                    PayloadType type = PayloadType.valueOf(payloadTypeString);
                    switch (type) {
                        case HEX_STRING:
                            //keep value like this
                            String hex = (String) obj;
                            hexBuilder.append(hex);
                            break;
                        case INTEGER:
                            //convert integer to hex value
                            Integer integer;
                            if (obj instanceof Integer) {
                                integer = (Integer) obj;
                            } else {
                                integer = Integer.parseInt(obj.toString());
                            }
                            hexBuilder.append(Integer.toHexString(integer));
                            break;
                        case LONG:
                            //convert integer to hex value
                            Long longValue = Long.parseLong(obj.toString());
                            hexBuilder.append(Long.toHexString(longValue));
                            break;
                        case HEX:
                            Long hexValue = Long.decode(obj.toString());
                            hexBuilder.append(Long.toHexString(hexValue));
                            break;
                        default:
                            Log.d(TAG, "Not managed type "+type+" yet");
                    }
                    //if it's an odd string length, let's add 0 at start to be able to build 2-digits packets
                    if (hexBuilder.length() % 2 == 1)
                        hexBuilder.insert(0, '0');
                    //let's cut the hex value into array of 2-digits
                    //String[] splittedValue = hexBuilder.toString().split("(?<=\\G.{2})");
                    String[] splittedValue = BytesUtils.splitStringByLength(hexBuilder.toString(), 2);
                    for (int i = end, j = splittedValue.length - 1; i >= start && j >= 0; i--, j--) {
                        commandArray[i] = splittedValue[j];
                    }
                }
                catch (IllegalArgumentException e) {
                    Log.e(TAG,"Unknown payload type "+payloadTypeString+" for "+payload.getName(), e);
                }
            }

            StringBuilder finalCommand = new StringBuilder();
            for (String byteString: commandArray) {
                finalCommand.append(byteString);
            }
            emitter.onSuccess(finalCommand.toString());
        });
    }
}
