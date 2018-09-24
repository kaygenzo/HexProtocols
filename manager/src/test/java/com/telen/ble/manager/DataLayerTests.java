package com.telen.ble.manager;

import android.util.Log;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.telen.ble.manager.model.Command;
import com.telen.ble.manager.model.Device;
import com.telen.ble.manager.model.Payload;
import com.telen.ble.manager.model.Request;
import com.telen.ble.manager.model.Response;
import com.telen.ble.manager.exceptions.CommandTimeoutException;
import com.telen.ble.manager.interfaces.HardwareLayerInterface;
import com.telen.ble.manager.validator.DataValidator;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UUID.class, Log.class})
public class BleDataLayerTests {

    private BleDataLayer datalayer;
    @Mock HardwareLayerInterface hardwareLayer;
    @Mock DataValidator dataValidator;
    @Mock RxBleDevice rxBleDevice;
    @Mock RxBleConnection rxBleConnection;
    @Mock ScanResult scanResult;

    private List<Payload> payloads;
    private Device expectedDevice;
    private Command command;

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
        MockitoAnnotations.initMocks(this);
        datalayer = new BleDataLayer(hardwareLayer, dataValidator);
        PowerMockito.mockStatic(Log.class);

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

        expectedDevice = new Device("mydevice", "macaddress");
        command = new Command();
        command.setIdentifier("TEST_COMMAND");
        Request request = new Request();
        request.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        command.setRequest(request);
        request.setPayloads(payloads);
    }

    @Test
    public void shouldBuildCommandFromPayloads() {


        Map<String, Object> data = new HashMap<>();
        data.put("SUBROUTINE", 4294967295L);
        data.put("RED", 125);
        data.put("GREEN", 255);
        data.put("BLUE", 125);

        TestObserver<String> observer = new TestObserver<>();
        datalayer.buildHexaCommand(payloads, data)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        assertEquals(observer.values().get(0),"ffffffff7dff7d0bcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void shouldConnectSuccessfully() {
        String deviceName = "mydevice";
        when(hardwareLayer.scan(deviceName)).thenReturn(Observable.just(scanResult));
        when(scanResult.getBleDevice()).thenReturn(rxBleDevice);
        when(rxBleDevice.getMacAddress()).thenReturn("macaddress");
        when(hardwareLayer.connect(rxBleDevice)).thenReturn(Single.just(rxBleConnection));
        TestObserver observer = new TestObserver();
        datalayer.connect(deviceName).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.onComplete();
        observer.assertValueCount(1);

        Device expectedDevice = new Device(deviceName, "macaddress");

        assertEquals(observer.values().get(0), expectedDevice);
    }

    @Test
    public void shouldNotConnectIfNoDeviceFound() {
        String deviceName = "mydevice";
        when(hardwareLayer.scan(deviceName)).thenReturn(Observable.just(scanResult));
        when(scanResult.getBleDevice()).thenReturn(rxBleDevice);
        when(rxBleDevice.getMacAddress()).thenReturn("macaddress");
        when(hardwareLayer.connect(rxBleDevice)).thenReturn(Single.just(rxBleConnection));
        TestObserver observer = new TestObserver();
        datalayer.connect(deviceName).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.onComplete();
        observer.assertValueCount(1);

        Device expectedDevice = new Device(deviceName, "macaddress");

        assertEquals(observer.values().get(0), expectedDevice);
    }

    @Test
    public void shouldSendCommandSuccessfullyWithoutWaitForResponses() {
        TestObserver<String> observer = new TestObserver<>();
        sendCommand(-1, false).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
    }

    @Test
    public void shouldSendCommandSuccessfullyAndWaitForResponses() {
        TestObserver<String> observer = new TestObserver<>();
        sendCommand(-1, true).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(3);
        assertArrayEquals(observer.values().toArray(), new String[]{
                "01000000000000000000000000000000000000",
                "02000000000000000000000000000000000000",
                "03000000000000000000000000000000000000"
        });
    }

    @Test
    public void shouldNotSendCommandWhenTimeoutTriggered() {
        TestObserver<String> observer = new TestObserver<>();
        sendCommand(100, false).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(CommandTimeoutException.class);

    }

    private Observable<String> sendCommand(long timeout, boolean listenResponse) {
        Map<String, Object> data = new HashMap<>();
        data.put("SUBROUTINE", 5L);
        data.put("RED", 0);
        data.put("GREEN", 0);
        data.put("BLUE", 0);

        if(listenResponse) {
            List<Payload> responsePayloads = new ArrayList<>();

            Payload payload = new Payload();
            payload.setName("RESPONSE");
            payload.setType("INTEGER");
            payload.setStart(0);
            payload.setEnd(0);
            responsePayloads.add(payload);

            Response response = new Response();
            response.setCharacteristic("00008888-0000-1000-8000-00805f9b34fb");
            response.setPayloads(responsePayloads);
            command.setResponse(response);
            when(dataValidator.validateData(any(List.class), any(String.class))).thenReturn(Completable.complete());
        }

        datalayer.getRxConnections().put(expectedDevice, rxBleConnection);
        UUID requestUuid = UUID.fromString(command.getRequest().getCharacteristic());
        UUID responseUuid = UUID.fromString("00008888-0000-1000-8000-00805f9b34fb");
        PowerMockito.mockStatic(UUID.class);
        when(UUID.fromString(command.getRequest().getCharacteristic())).thenReturn(requestUuid);
        when(dataValidator.validateData(payloads,data)).thenReturn(Completable.complete());
        when(hardwareLayer.sendCommand(any(RxBleConnection.class), any(UUID.class), any(String.class))).thenReturn(Single.just("05000000000000000000000000000000000000"));
        when(hardwareLayer.listenResponses(rxBleConnection, responseUuid)).thenReturn(Observable.fromArray(
                "01000000000000000000000000000000000000",
                "02000000000000000000000000000000000000",
                "03000000000000000000000000000000000000"
        ));

        datalayer.setTimeout(timeout);
        return datalayer.sendCommand(expectedDevice, command, data);
    }

    @Test
    public void shouldCleanMemoryAfterMultiConnection() {
        String deviceName = "mydevice";
        when(hardwareLayer.scan(deviceName)).thenReturn(Observable.just(scanResult));
        when(scanResult.getBleDevice()).thenReturn(rxBleDevice);
        when(rxBleDevice.getMacAddress()).thenReturn("macaddress");
        when(hardwareLayer.connect(rxBleDevice)).thenReturn(Single.just(rxBleConnection));
        TestObserver observer = new TestObserver();
        datalayer.connect(deviceName)
                .flatMap(device -> datalayer.connect(deviceName))
                .flatMap(device -> datalayer.connect(deviceName))
                .flatMap(device -> datalayer.connect(deviceName))
                .flatMap(device -> datalayer.connect(deviceName))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.onComplete();
        assertEquals(datalayer.getDevices().size(), 1);
        assertEquals(datalayer.getRxConnections().size(), 1);
        assertEquals(datalayer.getDevicesDisposable().size(), 1);
    }

    @Test
    public void shouldLaunchCommandSuccessfullyStartingByHex() {
        payloads.clear();
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
        datalayer.buildHexaCommand(payloads, data)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        assertEquals("01fe0000538310000000FF005002020000000000".toLowerCase(), observer.values().get(0).toLowerCase());
    }
}
