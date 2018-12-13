package com.telen.sdk.common.builder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.telen.sdk.common.models.Payload;
import com.telen.sdk.common.models.PayloadType;
import com.telen.sdk.common.utils.BytesUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;

public class CommandBuilder {

    private static final String TAG = CommandBuilder.class.getSimpleName();

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Single<String> dataCommandBuilder(@NonNull List<Payload> payloads, @Nullable Map<String, Object> data, int length) {
        return Single.create(emitter -> {
            String[] commandArray = new String[length > 0 ? length: payloads.size()];
            Arrays.fill(commandArray, "00");

            for (int index = 0; index < payloads.size() ; index++) {
                Payload payload = payloads.get(index);
                String identifier = payload.getName();
                Object obj = null;
                if(data != null)
                    obj = data.get(identifier);
                if(obj==null)
                    obj = payload.getValue();
                int start = payload.getStart();
                int end = payload.getEnd();
                StringBuilder commandBuilder = new StringBuilder();

                String payloadTypeString = payload.getType();
                try {
                    PayloadType type = PayloadType.valueOf(payloadTypeString);
                    switch (type) {
                        case HEX_STRING:
                            //keep value like this
                            String hex = (String) obj;
                            commandBuilder.append(hex);
                            break;
                        case INTEGER:
                            //convert integer to hex value
                            Integer integer;
                            if (obj instanceof Integer) {
                                integer = (Integer) obj;
                            } else {
                                integer = Integer.parseInt(obj.toString());
                            }
                            commandBuilder.append(Integer.toHexString(integer));
                            break;
                        case LONG:
                            //convert integer to hex value
                            Long longValue = Long.parseLong(obj.toString());
                            commandBuilder.append(Long.toHexString(longValue));
                            break;
                        case HEX:
                            try {
                                Long hexValue = Long.decode(obj.toString());
                                commandBuilder.append(Long.toHexString(hexValue));
                            }
                            catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            break;
                        case ASCII:
                            commandArray[index] = (String)obj;
                            continue;
                        case STRING:
                            String value = (String)obj;
                            value = BytesUtils.byteArrayToHex(value.getBytes());
                            commandBuilder.append(value);
                            break;
                        default:
                            Log.d(TAG, "Not managed type "+type+" yet");
                    }
                    //if it's an odd string length, let's add 0 at start to be able to build 2-digits packets
                    if (commandBuilder.length() % 2 == 1)
                        commandBuilder.insert(0, '0');
                    //let's cut the hex value into array of 2-digits
                    //String[] splittedValue = hexBuilder.toString().split("(?<=\\G.{2})");
                    String[] splittedValue = BytesUtils.splitStringByLength(commandBuilder.toString(), 2);
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
