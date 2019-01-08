package com.telen.sdk.common.models;

import android.util.Log;

import com.telen.sdk.common.utils.BytesUtils;

import java.util.List;
import java.util.Locale;

import io.reactivex.Single;

public class ResponseFrameFactory {

    private static final String TAG = ResponseFrameFactory.class.getSimpleName();

    public <T extends ResponseFrame> Single<ResponseFrame> parse(List<Payload> payloads, String responseFrameString, Class<T> type) {
        return Single.create(emitter -> {
            Log.d(TAG,"Parse "+responseFrameString);

            T returnResponseFrame;
            try {
                returnResponseFrame = type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                emitter.onError(e);
                return;
            }

            for (Payload payload: payloads) {
                String identifier = payload.getName();
                int start = payload.getStart();
                int end = payload.getEnd();

                StringBuilder payloadExtract = new StringBuilder();
                payloadExtract.append(responseFrameString, start*2, 2*(end+1));
                String payloadTypeString = payload.getType();

                Directions direction = Directions.LTR;
                try {
                    if(payload.getDirection()!=null)
                        direction = Directions.valueOf(payload.getDirection().toUpperCase(Locale.US));
                }
                catch (IllegalArgumentException e) {
                    Log.e(TAG,"",e);
                }

                String extractHex = payloadExtract.toString();
                if(direction==Directions.RTL)
                    extractHex = BytesUtils.reverseBytes(extractHex);

                Log.d(TAG,identifier+"->"+extractHex);

                try {
                    PayloadType payloadType = PayloadType.valueOf(payloadTypeString);
                    switch (payloadType) {
                        case INTEGER:
                            returnResponseFrame.setValue(payload, Integer.parseInt(extractHex, 16));
                            break;
                        case LONG:
                        case HEX:
                            returnResponseFrame.setValue(payload, Long.parseLong(extractHex, 16));
                            break;
                        case STRING:
                            returnResponseFrame.setValue(payload, BytesUtils.hexStringToAscii(extractHex));
                            break;
                        case HEX_STRING:
                            returnResponseFrame.setValue(payload, extractHex);
                            break;
                    }
                }
                catch (IllegalArgumentException e) {
                    emitter.onError(e);
                }
            }

            emitter.onSuccess(returnResponseFrame);
        });
    }
}
