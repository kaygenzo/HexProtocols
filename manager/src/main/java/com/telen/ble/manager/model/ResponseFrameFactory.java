package com.telen.ble.manager.model;

import android.util.Log;

import com.telen.ble.manager.utils.BytesUtils;

import java.util.List;

import io.reactivex.Single;

public class ResponseFrameFactory {

    private static final String TAG = ResponseFrameFactory.class.getSimpleName();

    public <T extends ResponseFrame> Single<ResponseFrame> parse(Response response, String responseFrameString, Class<T> type) {
        return Single.create(emitter -> {
            Log.d(TAG,"Parse "+responseFrameString);
            List<Payload> responsePayloads = response.getPayloads();

            T returnResponseFrame;
            try {
                returnResponseFrame = type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                emitter.onError(e);<
                return;
            }

            for (Payload payload: responsePayloads) {
                String identifier = payload.getName();
                int start = payload.getStart();
                int end = payload.getEnd();

                StringBuilder payloadExtract = new StringBuilder();
                payloadExtract.append(responseFrameString, start*2, 2*(end+1));
                String payloadTypeString = payload.getType();

                Directions direction = Directions.LTR;
                try {
                    if(payload.getDirection()!=null)
                        direction = Directions.valueOf(payload.getDirection().toUpperCase());
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
