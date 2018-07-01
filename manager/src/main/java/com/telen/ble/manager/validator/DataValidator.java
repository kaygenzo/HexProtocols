package com.telen.ble.manager.validator;

import com.telen.ble.manager.data.Payload;
import com.telen.ble.manager.exceptions.InvalidPayloadLengthException;
import com.telen.ble.manager.exceptions.InvalidPayloadValueException;
import com.telen.ble.manager.exceptions.PayloadOutOfBoundsException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;

public class DataValidator {
    public Completable validateData(List<Payload> payloads, Map<String, Object> dataToValidate) {
        return Completable.create(emitter -> {

            Map<String, Payload> payloadsMap = new HashMap<>();
            for (Payload payload: payloads) {
                payloadsMap.put(payload.getName(), payload);
            }

            for (String identifier: dataToValidate.keySet()) {
                Object value = dataToValidate.get(identifier);
                Payload payload = payloadsMap.get(identifier);
                int bytesLength = payload.getEnd() - payload.getStart() + 1;
                switch (payload.getType()) {
                    case "HEX":
                        String obj = (String)value;
                        if(obj.length() > bytesLength*2)
                            emitter.onError(new InvalidPayloadLengthException("Invalid payload size: expect "+bytesLength+" bytes but string is length "+obj.length()));
                        break;
                    case "INTEGER":
                        Integer integer = (Integer)value;
                        if(integer>payload.getMax() || integer<payload.getMin())
                            emitter.onError(new InvalidPayloadValueException("Invalid payload value: expect integer between"+ payload.getMin()+" and" + payload.getMax()
                                    +" but value is "+integer));
                        break;
                    case "LONG":
                        Long longValue = (Long)value;
                        if(longValue>payload.getMax() || longValue<payload.getMin())
                            emitter.onError(new InvalidPayloadValueException("Invalid payload value: expect long between"+ payload.getMin()+" and" + payload.getMax()
                                    +" but value is "+longValue));
                        break;
                }
                emitter.onComplete();
            }
        });
    }

    public Completable validateData(List<Payload> payloads, String hexString) {
        return Completable.create(emitter -> {

            for (Payload payload: payloads) {
                int start = payload.getStart()*2;
                int end = (payload.getEnd()+1)*2;

                if(start > hexString.length() || end > hexString.length())
                    emitter.onError(new PayloadOutOfBoundsException("start: "+start+" end: "+end+" but hexString length is "+hexString.length()));

                String subHexString = hexString.substring(start, end);

//                switch (payload.getType()) {
//                    case "HEX":
//                        //TODO ?
//                        break;
//                    case "INTEGER":
//                        Integer value = Integer.parseInt(subHexString, 16);
//                    case "LONG":
//                    default:
//                }
                emitter.onComplete();
            }
        });
    }
}
