package com.telen.ble.manager;

import android.util.Log;

import com.telen.ble.manager.builder.HexBuilder;
import com.telen.ble.manager.layers.impl.DataLayerImpl;
import com.telen.ble.manager.model.Command;
import com.telen.ble.manager.model.Device;
import com.telen.ble.manager.model.Payload;
import com.telen.ble.manager.model.Request;
import com.telen.ble.manager.model.Response;
import com.telen.ble.manager.exceptions.CommandTimeoutException;
import com.telen.ble.manager.layers.HardwareLayerInterface;
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
public class DataLayerTests {

    private DataLayerImpl datalayer;
    @Mock HardwareLayerInterface hardwareLayer;
    @Mock DataValidator dataValidator;
    @Mock
    HexBuilder mockHexBuilder;

    private List<Payload> payloads;
    private Device expectedDevice;
    private Command command;
    private TestObserver observer = new TestObserver();

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
        datalayer = new DataLayerImpl(hardwareLayer, dataValidator, mockHexBuilder);
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

        expectedDevice = new Device("mydevice", "@mac");
        command = new Command();
        command.setIdentifier("TEST_COMMAND");
        Request request = new Request();
        request.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        command.setRequest(request);
        request.setPayloads(payloads);
    }

    @Test
    public void shouldConnectSuccessfully() {
        boolean createBonding = false;
        when(hardwareLayer.connect(expectedDevice, createBonding)).thenReturn(Completable.complete());
        datalayer.connect(expectedDevice, createBonding).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(1);

        assertEquals(expectedDevice, observer.values().get(0));
    }

    @Test
    public void shouldBondSuccessfully() {
        boolean createBonding = true;
        when(hardwareLayer.connect(expectedDevice, createBonding)).thenReturn(Completable.complete());
        datalayer.connect(expectedDevice, createBonding).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(1);

        assertEquals(expectedDevice, observer.values().get(0));
    }

    @Test
    public void shouldScanSuccessfully() {
        when(hardwareLayer.scanOld(expectedDevice.getName())).thenReturn(Single.just(expectedDevice));
        when(hardwareLayer.scan(expectedDevice.getName())).thenReturn(Single.just(expectedDevice));
        datalayer.scan(expectedDevice.getName()).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(1);

        assertEquals(expectedDevice, observer.values().get(0));
    }

    @Test
    public void shouldDisconnectSuccessfully() {
        when(hardwareLayer.disconnect(expectedDevice)).thenReturn(Completable.complete());
        datalayer.disconnect(expectedDevice).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
    }

    @Test
    public void shouldGetPositiveBondedStatus() {
        when(hardwareLayer.isBonded(expectedDevice.getMacAddress())).thenReturn(Single.just(Boolean.TRUE));
        datalayer.isBonded(expectedDevice).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(1);
        assertTrue((Boolean)observer.values().get(0));
    }

    @Test
    public void shouldGetNegativeBondedStatus() {
        when(hardwareLayer.isBonded(expectedDevice.getMacAddress())).thenReturn(Single.just(Boolean.FALSE));
        datalayer.isBonded(expectedDevice).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(1);
        assertFalse((Boolean)observer.values().get(0));
    }

    @Test
    public void shouldNotSendCommandWithoutDataValidated() {
        Map<String, Object> data = new HashMap<>();
        Exception e = new Exception("Fake exception");
        when(dataValidator.validateData(payloads, data)).thenReturn(Completable.error(e));
        when(mockHexBuilder.buildHexaCommand(payloads, data)).thenReturn(Single.just("hexString"));
        datalayer.sendCommand(expectedDevice, command, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(e);
    }

    @Test
    public void shouldNotSendCommandWithHexBuildingFailed() {
        Map<String, Object> data = new HashMap<>();
        Exception e = new Exception("Fake exception");
        when(dataValidator.validateData(payloads, data)).thenReturn(Completable.complete());
        when(mockHexBuilder.buildHexaCommand(payloads, data)).thenReturn(Single.error(e));
        datalayer.sendCommand(expectedDevice, command, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(e);
    }

    @Test
    public void shouldNotSendCommandWhenErrorTriggeredInHWLayer() {
        Map<String, Object> data = new HashMap<>();
        datalayer.setRequestTimeout(-1);
        when(dataValidator.validateData(payloads, data)).thenReturn(Completable.complete());
        when(mockHexBuilder.buildHexaCommand(payloads, data)).thenReturn(Single.just("hexString"));
        UUID uuid = UUID.fromString("00007777-0000-1000-8000-00805f9b34fb");
        Exception e = new Exception("Fake exception");
        when(hardwareLayer.sendCommand(expectedDevice, uuid, "hexString")).thenReturn(Single.error(e));
        datalayer.sendCommand(expectedDevice, command, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(e);
    }

    @Test
    public void shouldSendCommandAndNotWaitForResponse() {
        sendCommand(-1, false, false).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
    }

    @Test
    public void shouldSendCommandAndWaitForResponse_WithoutEndOfFrame_completeOnTimeout_true() {
        //TODO make better test here to test timeout
        sendCommand(-1, 1, true, false, true).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
    }

    @Test
    public void shouldSendCommandAndWaitForResponse_WithoutEndOfFrame_completeOnTimeout_false() {
        //TODO make better test here to test timeout
        sendCommand(-1, 1, true, false, false).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(CommandTimeoutException.class);
    }

    @Test
    public void shouldSendCommandAndWaitForResponse_WithEndOfFrame() {
        sendCommand(-1, -1, true, true, false).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertResult(
                "01000000000000000000000000000000000000",
                "02000000000000000000000000000000000000",
                "03000000000000000000000000000000000000",
                "ffffffff");
    }

    @Test
    public void shouldNotSendCommandWhenTimeoutTriggered() {
        sendCommand(100, false, false).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(CommandTimeoutException.class);
    }

    private Observable<String> sendCommand(long timeout, boolean listenResponse, boolean endOfFrame) {
        return sendCommand(timeout, -1, listenResponse, endOfFrame, false);
    }

    private Observable<String> sendCommand(long requestTimeout, long responseTimeout, boolean listenResponse, boolean endOfFrame, boolean completeOnTimeout) {
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
            if(endOfFrame)
                response.setEndFrame("ffffffff");
            response.setCharacteristic("00008888-0000-1000-8000-00805f9b34fb");
            response.setPayloads(responsePayloads);
            if(completeOnTimeout)
                response.setCompleteOnTimeout(completeOnTimeout);
            command.setResponse(response);
            datalayer.setResponseTimeout(responseTimeout);
            when(dataValidator.validateData(any(List.class), any(String.class))).thenReturn(Completable.complete());
        }


        UUID requestUuid = UUID.fromString(command.getRequest().getCharacteristic());
        UUID responseUuid = UUID.fromString("00008888-0000-1000-8000-00805f9b34fb");
        PowerMockito.mockStatic(UUID.class);
        when(UUID.fromString(command.getRequest().getCharacteristic())).thenReturn(requestUuid);

        when(dataValidator.validateData(payloads,data)).thenReturn(Completable.complete());
        when(mockHexBuilder.buildHexaCommand(payloads, data)).thenReturn(Single.just("hexString"));
        when(hardwareLayer.sendCommand(expectedDevice, requestUuid, "hexString")).thenReturn(Single.just("05000000000000000000000000000000000000"));
        when(hardwareLayer.listenResponses(expectedDevice, responseUuid)).thenReturn(Observable.create(emitter -> {
                    emitter.onNext("01000000000000000000000000000000000000");
                    emitter.onNext("02000000000000000000000000000000000000");
                    emitter.onNext("03000000000000000000000000000000000000");
                    if(endOfFrame){
                        emitter.onNext("ffffffff");
                    }
                }
        ));

        datalayer.setRequestTimeout(requestTimeout);
        return datalayer.sendCommand(expectedDevice, command, data);
    }
}
