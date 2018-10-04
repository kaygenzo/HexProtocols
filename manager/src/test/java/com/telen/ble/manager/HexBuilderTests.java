package com.telen.ble.manager;

import com.telen.ble.manager.builder.HexBuilder;
import com.telen.ble.manager.model.Payload;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

@RunWith(MockitoJUnitRunner.class)
public class HexBuilderTests {

    @Mock
    HexBuilder hexBuilder;
    private TestObserver observer = new TestObserver();
    private List<Payload> payloads;

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
        hexBuilder = new HexBuilder();

        payloads = new ArrayList<>();

        Payload payload = new Payload();
        payload.setName("SUBROUTINE");
        payload.setType("LONG");
        payload.setStart(0);
        payload.setEnd(3);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("RED");
        payload.setType("INTEGER");
        payload.setStart(4);
        payload.setEnd(4);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("GREEN");
        payload.setType("INTEGER");
        payload.setStart(5);
        payload.setEnd(5);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("BLUE");
        payload.setType("INTEGER");
        payload.setStart(6);
        payload.setEnd(6);
        payloads.add(payload);

        payload = new Payload();
        payload.setName("SUFFIX");
        payload.setType("HEX");
        payload.setStart(7);
        payload.setEnd(19);
        payload.setValue("bcdefghijklmnopqrstuvwxyz");
        payloads.add(payload);
    }

    @Test
    public void shouldLaunchCommandSuccessfullyStartingByHex() {
        List<Payload> payloads = new ArrayList<>();
        Payload payload = new Payload();
        payload.setName("PREFIX");
        payload.setType("HEX");
        payload.setStart(0);
        payload.setEnd(7);
        payload.setValue("01fe000053831000");
        payloads.add(payload);

        payload = new Payload();
        payload.setName("GREEN");
        payload.setType("INTEGER");
        payload.setStart(8);
        payload.setEnd(8);
        payload.setValue("0");
        payloads.add(payload);

        payload = new Payload();
        payload.setName("BLUE");
        payload.setType("INTEGER");
        payload.setStart(9);
        payload.setEnd(9);
        payload.setValue("0");
        payloads.add(payload);

        payload = new Payload();
        payload.setName("RED");
        payload.setType("INTEGER");
        payload.setStart(10);
        payload.setEnd(10);
        payload.setValue("255");
        payloads.add(payload);

        payload = new Payload();
        payload.setName("UNKNOWN");
        payload.setType("HEX");
        payload.setStart(11);
        payload.setEnd(12);
        payload.setValue("0050");
        payloads.add(payload);

        payload = new Payload();
        payload.setName("LUMINOSITY_1");
        payload.setType("INTEGER");
        payload.setStart(13);
        payload.setEnd(13);
        payload.setValue("2");
        payloads.add(payload);

        payload = new Payload();
        payload.setName("LUMINOSITY_1");
        payload.setType("INTEGER");
        payload.setStart(14);
        payload.setEnd(14);
        payload.setValue("2");
        payloads.add(payload);

        payload = new Payload();
        payload.setName("SUFFIX");
        payload.setType("HEX");
        payload.setStart(15);
        payload.setEnd(15);
        payload.setValue("00");
        payloads.add(payload);

        Map<String, Object> data = new HashMap<>();
        data.put("RED", 255);
        data.put("GREEN", 0);
        data.put("BLUE", 0);

        TestObserver<String> observer = new TestObserver<>();
        hexBuilder.buildHexaCommand(payloads, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals("01fe0000538310000000FF005002020000000000".toLowerCase(), observer.values().get(0).toLowerCase());
    }

    @Test
    public void shouldBuildCommandFromPayloads() {
        Map<String, Object> data = new HashMap<>();
        data.put("SUBROUTINE", 4294967295L);
        data.put("RED", 125);
        data.put("GREEN", 255);
        data.put("BLUE", 125);

        hexBuilder.buildHexaCommand(payloads, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals(observer.values().get(0),"ffffffff7dff7d0bcdefghijklmnopqrstuvwxyz");
    }
}
