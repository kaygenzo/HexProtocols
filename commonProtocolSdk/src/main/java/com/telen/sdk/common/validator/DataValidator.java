package com.telen.sdk.common.validator;

import android.util.Log;

import com.telen.sdk.common.exceptions.InvalidPayloadLengthException;
import com.telen.sdk.common.exceptions.InvalidPayloadValueException;
import com.telen.sdk.common.models.Directions;
import com.telen.sdk.common.models.Frame;
import com.telen.sdk.common.models.Payload;
import com.telen.sdk.common.models.PayloadType;
import com.telen.sdk.common.utils.BytesUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Completable;

public class DataValidator {

    private static final String TAG = DataValidator.class.getSimpleName();

    public Completable validateData(List<Payload> payloads, Map<String, Object> dataToValidate) {
        return Completable.create(emitter -> {

            if(dataToValidate==null) {
                //nothing to validate
                emitter.onComplete();
                return;
            }

            Map<String, Payload> payloadsMap = new HashMap<>();
            for (Payload payload: payloads) {
                payloadsMap.put(payload.getName(), payload);
            }

            for (String identifier: dataToValidate.keySet()) {
                Object value = dataToValidate.get(identifier);
                Payload payload = payloadsMap.get(identifier);
                int bytesLength = payload.getEnd() - payload.getStart() + 1;

                String payloadTypeString = payload.getType();
                try {
                    PayloadType type = PayloadType.valueOf(payloadTypeString);

                    switch (type) {
                        case STRING:
                            String obj = (String) value;
                            if(obj.length() != bytesLength)
                                emitter.onError(new InvalidPayloadLengthException("Invalid payload size for "+payload.getName()+" : expect " + bytesLength + " bytes but string is length " + obj.length()));
                            break;
                        case HEX_STRING:
                            obj = (String) value;
                            if (obj.length() > bytesLength * 2)
                                emitter.onError(new InvalidPayloadLengthException("Invalid payload size for "+payload.getName()+" : expect " + bytesLength + " bytes but string is length " + obj.length()));
                            break;
                        case INTEGER:
                            Integer integer = Integer.parseInt(value.toString());
                            if (integer > Integer.parseInt(payload.getMax()) || integer < Integer.parseInt(payload.getMin())) {
                                emitter.onError(new InvalidPayloadValueException("Invalid payload value for "+payload.getName()+" : expect integer between " + payload.getMin() + " and " + payload.getMax()
                                        + " but value is " + integer));
                                return;
                            }
                            break;
                        case LONG:
                            Long longValue = Long.parseLong(value.toString());
                            if(payload.getMax()!=null && payload.getMin()!=null) {
                                if (longValue > Long.parseLong(payload.getMax()) || longValue < Long.parseLong(payload.getMin())) {
                                    emitter.onError(new InvalidPayloadValueException("Invalid payload value for " + payload.getName() + " : expect long between " + payload.getMin() + " and " + payload.getMax()
                                            + " but value is " + longValue));
                                    return;
                                }
                            }
                            else
                                Log.w(TAG,"No min value or max value found, payload validated without check");
                            break;
                        case HEX:
                            longValue = Long.parseLong(value.toString());
                            Long hexMin = Long.decode(payload.getMin());
                            Long hexMax = Long.decode(payload.getMax());
                            if (longValue > hexMax || longValue < hexMin) {
                                emitter.onError(new InvalidPayloadValueException("Invalid payload value for "+payload.getName()+" : expect hex between " + payload.getMin() + " and " + payload.getMax()
                                        + " but value is " + longValue));
                                return;
                            }
                            break;
                        default:
                            Log.d(TAG, "Not managed type "+type+" yet");
                    }
                }
                catch (IllegalArgumentException e) {
                    emitter.onError(e);
                }
            }
            emitter.onComplete();
        });
    }

    public Completable validateData(List<Frame> frames, String hexString) {
        return Completable.create(emitter -> {

            if(frames==null || frames.isEmpty()) {
                emitter.onComplete();
                return;
            }

            List<Payload> payloads = null;

//            for (Frame frame: frames) {
//                List<Payload> localPayloads = frame.getPayloads();
//                if(localPayloads!=null && !localPayloads.isEmpty() && frame.getCommandIndex() < localPayloads.size()) {
//                    //search for corresponding frame
//                    Payload payload = localPayloads.get(frame.getCommandIndex());
//
//                    if(payload.getValue()!=null && PayloadType.INTEGER.name().equals(payload.getType())) {
//                        int start = payload.getStart();
//                        int end = payload.getEnd();
//
//                        StringBuilder payloadExtract = new StringBuilder();
//                        payloadExtract.append(hexString, start * 2, 2 * (end + 1));
//                        int command = Integer.parseInt(payloadExtract.toString(), 16);
//                        if (command == Integer.parseInt(payload.getValue())) {
//                            payloads = localPayloads;
//                            break;
//                        }
//                    }
//                }
//            }

            for (Frame frame: frames) {
                if (frame.getCommandIndex() >= 0 && frame.getCommandId() >= 0) {
                    StringBuilder payloadExtract = new StringBuilder();
                    payloadExtract.append(hexString, frame.getCommandIndex() * 2, 2 * (frame.getCommandIndex() + 1));
                    int command = Integer.parseInt(payloadExtract.toString(), 16);
                    if (command == frame.getCommandId()) {
                        payloads = frame.getPayloads();
                        break;
                    }
                }
                else
                    Log.i(TAG,"commandIndex or commandId not configured in the protocol file.");
            }

            if(payloads==null || payloads.isEmpty()) {
                emitter.onComplete();
                return;
            }

            for (Payload payload: payloads) {
                int start = payload.getStart();
                int end = payload.getEnd();

                StringBuilder payloadExtract = new StringBuilder();
                payloadExtract.append(hexString, start*2, 2*(end+1));
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

                int bytesLength = end - start + 1;

//                Log.d(TAG,identifier+"->"+extractHex);

                try {
                    PayloadType payloadType = PayloadType.valueOf(payloadTypeString);
                    switch (payloadType) {
                        case STRING:
                            String stringValue = BytesUtils.hexStringToAscii(extractHex);
                            if (stringValue.length() != bytesLength)
                                emitter.onError(new InvalidPayloadLengthException("Invalid payload size: expect " + bytesLength + " characters but string is length " + stringValue.length()));

                            break;
                        case HEX_STRING:
                            String obj = extractHex;
                            if (obj.length() > bytesLength * 2)
                                emitter.onError(new InvalidPayloadLengthException("Invalid payload size: expect " + bytesLength + " bytes but string is length " + obj.length()));
                            break;
                        case INTEGER:
                            Integer integer = Integer.parseInt(extractHex, 16);
                            if (integer > Integer.parseInt(payload.getMax()) || integer < Integer.parseInt(payload.getMin())) {
                                emitter.onError(new InvalidPayloadValueException("Invalid payload value: expect integer between " + payload.getMin() + " and " + payload.getMax()
                                        + " but value is " + integer));
                                return;
                            }
                            break;
                        case LONG:
                            Long longValue = Long.parseLong(extractHex, 16);
                            if (longValue > Long.parseLong(payload.getMax()) || longValue < Long.parseLong(payload.getMin())) {
                                emitter.onError(new InvalidPayloadValueException("Invalid payload value: expect long between " + payload.getMin() + " and " + payload.getMax()
                                        + " but value is " + longValue));
                                return;
                            }
                            break;
                        case HEX:
                            longValue = Long.parseLong(extractHex, 16);
                            if(payload.getMin()!=null && payload.getMax()!=null) {
                                Long hexMin = Long.decode(payload.getMin());
                                Long hexMax = Long.decode(payload.getMax());
                                if (longValue > hexMax || longValue < hexMin) {
                                    emitter.onError(new InvalidPayloadValueException("Invalid payload value: expect hex between " + payload.getMin() + " and " + payload.getMax()
                                            + " but value is " + longValue));
                                    return;
                                }
                            }
                            else
                                Log.w(TAG,"No min value or max value found");
                            if(payload.getValue()!=null) {
                                long hexValue = Long.decode(payload.getValue());
                                if(hexValue!=longValue) {
                                    emitter.onError(new InvalidPayloadValueException("Invalid payload value: expect hex value equals to " + payload.getValue()
                                            + " but value is " + longValue));
                                    return;
                                }
                            }
                            break;
                        default:
                            Log.d(TAG, "Not managed type "+payloadType+" yet");
                    }
                }
                catch (IllegalArgumentException e) {
                    emitter.onError(e);
                }
            }
            emitter.onComplete();
        });
    }
}
