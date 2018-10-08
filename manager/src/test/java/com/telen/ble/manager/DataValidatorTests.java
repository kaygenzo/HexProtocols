package com.telen.ble.manager;

import com.telen.ble.manager.model.Payload;
import com.telen.ble.manager.exceptions.InvalidPayloadLengthException;
import com.telen.ble.manager.exceptions.InvalidPayloadValueException;
import com.telen.ble.manager.model.PayloadType;
import com.telen.ble.manager.validator.DataValidator;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class DataValidatorTests {

    private DataValidator dataValidator;

    @BeforeClass
    public static void setupClass() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerCallable -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @AfterClass
    public static void tearDownClass() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Before
    public void setup() {
        dataValidator = new DataValidator();
    }

    @Test
    public void shouldValidateValidPayloads() {

    }

    @Test
    public void shouldNotValidHexStringWithInvalidLength() {
        List<Payload> payloads = new ArrayList<>();

        Payload payload = new Payload();
        payload.setName("SUBROUTINE");
        payload.setType(PayloadType.HEX_STRING.name());
        payload.setStart(0);
        payload.setEnd(3);
        payloads.add(payload);

        Map<String, Object> data = new HashMap<>();
        data.put("SUBROUTINE", "2100000000");

        TestObserver observer = new TestObserver();
        dataValidator.validateData(payloads, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(InvalidPayloadLengthException.class);
    }

    @Test
    public void shouldNotValidIntegerValueWithOutOfBoundValue() {
        List<Payload> payloads = new ArrayList<>();

        Payload payload = new Payload();
        payload.setName("TEST");
        payload.setType(PayloadType.INTEGER.name());
        payload.setStart(0);
        payload.setEnd(0);
        payload.setMin(0);
        payload.setMax(10);
        payloads.add(payload);

        Map<String, Object> data = new HashMap<>();
        data.put("TEST", 11);

        TestObserver observer = new TestObserver();
        dataValidator.validateData(payloads, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(InvalidPayloadValueException.class);
    }

    @Test
    public void shouldValidIntegerValueWithLimitBoundValue() {
        List<Payload> payloads = new ArrayList<>();

        Payload payload = new Payload();
        payload.setName("TEST");
        payload.setType(PayloadType.INTEGER.name());
        payload.setStart(0);
        payload.setEnd(0);
        payload.setMin(4);
        payload.setMax(4);
        payloads.add(payload);

        Map<String, Object> data = new HashMap<>();
        data.put("TEST", 4);

        TestObserver observer = new TestObserver();
        dataValidator.validateData(payloads, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
    }

    @Test
    public void shouldNotValidLongValueWithOutOfBoundValue() {
        List<Payload> payloads = new ArrayList<>();

        Payload payload = new Payload();
        payload.setName("TEST");
        payload.setType(PayloadType.LONG.name());
        payload.setStart(0);
        payload.setEnd(0);
        payload.setMin(0);
        payload.setMax(10);
        payloads.add(payload);

        Map<String, Object> data = new HashMap<>();
        data.put("TEST", 11L);

        TestObserver observer = new TestObserver();
        dataValidator.validateData(payloads, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(InvalidPayloadValueException.class);
    }

    @Test
    public void shouldNotValidLongValueWithLimitBoundValue() {
        List<Payload> payloads = new ArrayList<>();

        Payload payload = new Payload();
        payload.setName("TEST");
        payload.setType(PayloadType.LONG.name());
        payload.setStart(0);
        payload.setEnd(0);
        payload.setMin(4);
        payload.setMax(4);
        payloads.add(payload);

        Map<String, Object> data = new HashMap<>();
        data.put("TEST", 4L);

        TestObserver observer = new TestObserver();
        dataValidator.validateData(payloads, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
    }

    @Test
    public void shouldNotValidHexValueWithOutOfBoundsValue() {
        List<Payload> payloads = new ArrayList<>();
        Payload payload = new Payload();
        payload.setName("PAYLOAD_1");
        payload.setType(PayloadType.HEX_STRING.name());
        payload.setStart(0);
        payload.setEnd(1);
        payload.setValue("3F3E");
        payloads.add(payload);

        payload = new Payload();
        payload.setName("PAYLOAD_2");
        payload.setType(PayloadType.INTEGER.name());
        payload.setStart(4);
        payload.setEnd(5);
        payload.setMin(0);
        payload.setMax(3);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("PAYLOAD_3");
        payload.setType(PayloadType.LONG.name());
        payload.setStart(6);
        payload.setEnd(7);
        payload.setMin(0);
        payload.setMax(3);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("PAYLOAD_4");
        payload.setType(PayloadType.HEX.name());
        payload.setStart(8);
        payload.setEnd(11);
        payload.setMin(0);
        payload.setMax("0xFFFFFF");
        payloads.add(payload);

        Map<String, Object> data = new HashMap<>();
        data.put("PAYLOAD_2", 1);
        data.put("PAYLOAD_3", 2);
        data.put("PAYLOAD_4", 0xFFFFFFFFL);

        TestObserver<String> observer = new TestObserver<>();
        dataValidator.validateData(payloads, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(InvalidPayloadValueException.class);
    }
}
