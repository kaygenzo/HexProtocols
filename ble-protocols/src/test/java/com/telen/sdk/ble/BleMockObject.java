package com.telen.sdk.ble;

import com.telen.sdk.common.models.Command;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.Payload;
import com.telen.sdk.common.models.PayloadType;
import com.telen.sdk.common.models.Request;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.observers.TestObserver;

public class BleMockObject {
    protected List<Payload> payloads;
    protected Command command;
    protected Device expectedDevice;

    protected TestObserver observer = new TestObserver();

    public BleMockObject() {
        payloads = new ArrayList<>();

        Payload payload = new Payload();
        payload.setName("SUBROUTINE");
        payload.setType(PayloadType.LONG.name());
        payload.setStart(0);
        payload.setEnd(3);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("RED");
        payload.setType(PayloadType.INTEGER.name());
        payload.setStart(4);
        payload.setEnd(4);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("GREEN");
        payload.setType(PayloadType.INTEGER.name());
        payload.setStart(5);
        payload.setEnd(5);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("BLUE");
        payload.setType(PayloadType.INTEGER.name());
        payload.setStart(6);
        payload.setEnd(6);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("SUFFIX");
        payload.setType(PayloadType.HEX_STRING.name());
        payload.setStart(7);
        payload.setEnd(19);
        payload.setValue("bcdefghijklmnopqrstuvwxyz");
        payloads.add(payload);

        expectedDevice = new Device("mydevice", "mac@");
        command = new Command();
        command.setIdentifier("TEST_COMMAND");
        Request request = new Request();
        request.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        request.setLength(20);
        command.setRequest(request);
        request.setPayloads(payloads);
    }
}
