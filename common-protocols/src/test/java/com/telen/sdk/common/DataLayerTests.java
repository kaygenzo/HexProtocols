package com.telen.sdk.common;

import android.util.Log;

import com.telen.sdk.common.builder.CommandBuilder;
import com.telen.sdk.common.layers.HardwareLayerInterface;
import com.telen.sdk.common.layers.impl.DataLayerImpl;
import com.telen.sdk.common.models.Frame;
import com.telen.sdk.common.models.Payload;
import com.telen.sdk.common.models.Request;
import com.telen.sdk.common.models.Response;
import com.telen.sdk.common.validator.DataValidator;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class DataLayerTests extends CommonMock {

    private DataLayerImpl<HardwareLayerInterface> datalayer;
    @Mock HardwareLayerInterface hardwareLayer;
    @Mock DataValidator dataValidator;
    @Mock CommandBuilder mockCommandBuilder;

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
        datalayer = new DataLayerImpl(hardwareLayer, dataValidator, mockCommandBuilder);
        PowerMockito.mockStatic(Log.class);

        Mockito.when(hardwareLayer.prepareBeforeSendingCommand(Mockito.any(Request.class))).thenReturn(Completable.complete());

        Response response = new Response();
        command.setResponse(response);
        Frame frame = new Frame();
        List<Frame> frames = new ArrayList<>();
        frames.add(frame);
        response.setFrames(frames);

        frame.setCommandIndex(0);
        frame.setCommandId(1);
    }

    @Test
    public void shouldConnectSuccessfully() {
        boolean createBonding = false;
        Mockito.when(hardwareLayer.connect(expectedDevice, createBonding)).thenReturn(Completable.complete());
        datalayer.connect(expectedDevice, createBonding).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(1);

        assertEquals(expectedDevice, observer.values().get(0));
    }

    @Test
    public void shouldNotConnectIfErrorOccurredInHardwareLayer() {
        boolean createBonding = false;
        Mockito.when(hardwareLayer.connect(expectedDevice, createBonding)).thenReturn(Completable.error(Exception::new));
        datalayer.connect(expectedDevice, createBonding).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(Exception.class);
    }

    @Test
    public void shouldBondSuccessfully() {
        boolean createBonding = true;
        Mockito.when(hardwareLayer.connect(expectedDevice, createBonding)).thenReturn(Completable.complete());
        datalayer.connect(expectedDevice, createBonding).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(1);

        assertEquals(expectedDevice, observer.values().get(0));
    }

    @Test
    public void shouldScanSuccessfully() {
        Mockito.when(hardwareLayer.scan(expectedDevice.getName())).thenReturn(Single.just(expectedDevice));
        datalayer.scan(expectedDevice.getName()).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValueCount(1);

        assertEquals(expectedDevice, observer.values().get(0));
    }

    @Test
    public void shouldScanFailedIfErrorTriggeredInHardwareLayer() {
        Mockito.when(hardwareLayer.scan(expectedDevice.getName())).thenReturn(Single.error(new Exception()));
        datalayer.scan(expectedDevice.getName()).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(Exception.class);
    }

    @Test
    public void shouldDisconnectSuccessfully() {
        Mockito.when(hardwareLayer.disconnect(expectedDevice)).thenReturn(Completable.complete());
        datalayer.disconnect(expectedDevice).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
    }

    @Test
    public void shouldListenResponsesSuccessfully() {
        Mockito.when(hardwareLayer.listenResponses(expectedDevice, command.getResponse())).thenReturn(Observable.fromArray("0102","0304","0506"));
        Mockito.when(dataValidator.validateData(command.getResponse().getFrames(), "0102")).thenReturn(Completable.complete());
        Mockito.when(dataValidator.validateData(command.getResponse().getFrames(), "0304")).thenReturn(Completable.complete());
        Mockito.when(dataValidator.validateData(command.getResponse().getFrames(), "0506")).thenReturn(Completable.complete());
        datalayer.observe(expectedDevice, command.getResponse())
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValues("0102","0304","0506");
    }

    @Test
    public void shouldNotListenResponsesIfAnErrorOccurredInHardwareLayer() {
        Mockito.when(hardwareLayer.listenResponses(expectedDevice, command.getResponse())).thenReturn(Observable.error(Exception::new));
        datalayer.observe(expectedDevice, command.getResponse())
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(Exception.class);
    }

//    @Test
//    public void shouldGetPositiveBondedStatus() {
//        Mockito.when(hardwareLayer.isBonded(expectedDevice.getMacAddress())).thenReturn(Single.just(Boolean.TRUE));
//        datalayer.isBonded(expectedDevice).subscribe(observer);
//        observer.awaitTerminalEvent();
//        observer.assertComplete();
//        observer.assertValueCount(1);
//        assertTrue((Boolean)observer.values().get(0));
//    }

//    @Test
//    public void shouldGetNegativeBondedStatus() {
//        Mockito.when(hardwareLayer.isBonded(expectedDevice.getMacAddress())).thenReturn(Single.just(Boolean.FALSE));
//        datalayer.isBonded(expectedDevice).subscribe(observer);
//        observer.awaitTerminalEvent();
//        observer.assertComplete();
//        observer.assertValueCount(1);
//        assertFalse((Boolean)observer.values().get(0));
//    }

    @Test
    public void shouldNotSendCommandWithoutDataValidated() {
        Map<String, Object> data = new HashMap<>();
        Exception e = new Exception("Fake exception");
        Mockito.when(dataValidator.validateData(payloads, data)).thenReturn(Completable.error(e));
        Mockito.when(mockCommandBuilder.dataCommandBuilder(payloads, data, command.getRequest().getLength())).thenReturn(Single.just("hexString"));
        datalayer.sendCommand(expectedDevice, command, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(e);
    }

    @Test
    public void shouldNotSendCommandWithHexBuildingFailed() {
        Map<String, Object> data = new HashMap<>();
        Exception e = new Exception("Fake exception");
        Mockito.when(dataValidator.validateData(payloads, data)).thenReturn(Completable.complete());
        Mockito.when(mockCommandBuilder.dataCommandBuilder(payloads, data, command.getRequest().getLength())).thenReturn(Single.error(e));
        datalayer.sendCommand(expectedDevice, command, data).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(e);
    }

    @Test
    public void shouldNotSendCommandWhenErrorTriggeredInHWLayer() {
        Map<String, Object> data = new HashMap<>();
        datalayer.setRequestTimeout(-1);
        datalayer.setResponseTimeout(-1);

        Mockito.when(dataValidator.validateData(payloads, data)).thenReturn(Completable.complete());
        Mockito.when(mockCommandBuilder.dataCommandBuilder(payloads, data, command.getRequest().getLength())).thenReturn(Single.just("hexString"));
        Mockito.when(hardwareLayer.listenResponses(expectedDevice, command.getResponse())).thenReturn(Observable.create(emitter -> {}));

        Exception e = new Exception("Fake exception");
        Mockito.when(hardwareLayer.sendCommand(expectedDevice, command.getRequest(), "hexString")).thenReturn(Single.error(e));
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
        observer.assertError(TimeoutException.class);
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
        observer.assertError(TimeoutException.class);
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
            List<Frame> frames = new ArrayList<>();
            Frame frame = new Frame();
            frames.add(frame);
            response.setFrames(frames);
            frame.setPayloads(responsePayloads);

            if(completeOnTimeout)
                response.setCompleteOnTimeout(completeOnTimeout);
            command.setResponse(response);
            datalayer.setResponseTimeout(responseTimeout);
            Mockito.when(dataValidator.validateData(ArgumentMatchers.any(List.class), ArgumentMatchers.any(String.class))).thenReturn(Completable.complete());
        }
        else
            command.setResponse(null);


        Mockito.when(dataValidator.validateData(payloads,data)).thenReturn(Completable.complete());
        Mockito.when(mockCommandBuilder.dataCommandBuilder(payloads, data, command.getRequest().getLength())).thenReturn(Single.just("hexString"));
        Mockito.when(hardwareLayer.sendCommand(expectedDevice, command.getRequest(), "hexString")).thenReturn(Single.just("05000000000000000000000000000000000000"));
        Mockito.when(hardwareLayer.listenResponses(expectedDevice, command.getResponse())).thenReturn(Observable.create(emitter -> {
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
