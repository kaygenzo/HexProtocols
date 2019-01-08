package com.telen.sdk.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.mock.MockContext;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleScanResult;
import com.polidea.rxandroidble2.Timeout;
import com.telen.sdk.ble.layers.impl.BleHardwareConnectionLayer;
import com.telen.sdk.ble.models.ResponseType;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.Response;
import com.telen.sdk.common.utils.BytesUtils;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UUID.class, Log.class})
public class HardwareLayerTests extends BleMockObject {

    @Mock BleHardwareConnectionLayer hardwareConnectionLayer;
    @Mock RxBleClient mockRxBleClient;
    @Mock BluetoothAdapter mockBluetoothAdapter;

    @Mock RxBleDevice mockrxBleDevice;
    @Mock RxBleConnection mockBleConnection;
    @Mock BluetoothDevice mockBluetoothDevice;
    @Mock Intent intent;
    @Mock RxBleScanResult rxMockScanResult;

    private MockedContext mockContext;

    private class MockedContext extends MockContext {

        private BroadcastReceiver receiver;

        public BroadcastReceiver getReceiver() {
            return receiver;
        }

        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
            this.receiver = receiver;
            return new Intent();
        }

        @Override
        public void unregisterReceiver(BroadcastReceiver receiver) {
            this.receiver = null;
        }
    }

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
        mockContext = new MockedContext();
        hardwareConnectionLayer = new BleHardwareConnectionLayer(mockRxBleClient, mockBluetoothAdapter, mockContext);
        expectedDevice = new Device("mydevice", "mac@");
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void shouldNotConnectIfDeviceIsNull() {
        hardwareConnectionLayer.connect(null, false).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(IllegalArgumentException.class);
    }

    @Test
    public void shouldCleanMemoryAfterMultiConnection_connect() {
        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        hardwareConnectionLayer.connect(expectedDevice, false)
                .andThen(hardwareConnectionLayer.connect(expectedDevice, false))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, false))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, false))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, false))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals(hardwareConnectionLayer.getBleDevices().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesConnection().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().get(expectedDevice).size(), 1);
    }

    @Test
    public void shouldCleanMemoryAfterMultiConnection_alreadyBonded() {
        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        when(mockBluetoothDevice.getAddress()).thenReturn(expectedDevice.getMacAddress());
        Set<BluetoothDevice> bondedDevices = new HashSet<>();
        bondedDevices.add(mockBluetoothDevice);
        when(mockBluetoothAdapter.getBondedDevices()).thenReturn(bondedDevices);
        hardwareConnectionLayer.connect(expectedDevice, true)
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals(hardwareConnectionLayer.getBleDevices().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesConnection().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().get(expectedDevice).size(), 1);
    }

    @Test
    public void shouldCleanMemoryAfterMultiConnection_bond_success() {
        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        when(mockBluetoothAdapter.getBondedDevices()).thenReturn(new HashSet<>());
        when(mockrxBleDevice.getBluetoothDevice()).thenReturn(mockBluetoothDevice);

        when(intent.getIntExtra(any(String.class), any(Integer.class))).thenReturn(BluetoothDevice.BOND_BONDED);

        doAnswer(invocation -> {
            mockContext.getReceiver().onReceive(mockContext, intent);
            return null;
        }).when(mockBluetoothDevice).createBond();
        hardwareConnectionLayer.connect(expectedDevice, true)
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals(hardwareConnectionLayer.getBleDevices().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesConnection().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().get(expectedDevice).size(), 1);
    }

    @Test
    public void shouldCleanMemoryAfterMultiConnection_bond_failed() {
        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        when(mockBluetoothAdapter.getBondedDevices()).thenReturn(new HashSet<>());
        when(mockrxBleDevice.getBluetoothDevice()).thenReturn(mockBluetoothDevice);

        when(intent.getIntExtra(any(String.class), any(Integer.class))).thenReturn(BluetoothDevice.BOND_NONE);

        doAnswer(invocation -> {
            mockContext.getReceiver().onReceive(mockContext, intent);
            return null;
        }).when(mockBluetoothDevice).createBond();
        hardwareConnectionLayer.connect(expectedDevice, true)
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .andThen(hardwareConnectionLayer.connect(expectedDevice, true))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(Exception.class);
        Assert.assertEquals(1, hardwareConnectionLayer.getBleDevices().size());
        Assert.assertEquals(0, hardwareConnectionLayer.getDevicesConnection().size());
        Assert.assertEquals(1, hardwareConnectionLayer.getDevicesDisposable().size());
        Assert.assertEquals(0, hardwareConnectionLayer.getDevicesDisposable().get(expectedDevice).size());
    }

    @Test
    public void shouldConnectAndDisconnectManyTimes() {

        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));

        hardwareConnectionLayer.connect(expectedDevice, false)
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals(1, hardwareConnectionLayer.getBleDevices().size());
        Assert.assertEquals(0, hardwareConnectionLayer.getDevicesConnection().size());
        Assert.assertEquals(0, hardwareConnectionLayer.getDevicesDisposable().size());
    }

    @Test
    public void shouldOnlyDisconnectManyTimes() {
        hardwareConnectionLayer.disconnect(expectedDevice)
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals(0, hardwareConnectionLayer.getBleDevices().size());
        Assert.assertEquals(0, hardwareConnectionLayer.getDevicesConnection().size());
        Assert.assertEquals(0, hardwareConnectionLayer.getDevicesDisposable().size());
    }

    @Test
    public void assertCanDisconnectAfterIfErrorOccurredWhenListening() {
        Response response = new Response();
        response.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        response.setService("00008888-0000-1000-8000-00805f9b34fb");

        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));

        hardwareConnectionLayer.connect(expectedDevice, false)
                .andThen(hardwareConnectionLayer.listenResponses(expectedDevice, response))
                .timeout(200, TimeUnit.MILLISECONDS)
                .subscribe( next -> {}, throwable -> {
                    hardwareConnectionLayer.disconnect(expectedDevice)
                            .subscribe(observer);
                    observer.awaitTerminalEvent();
                    observer.assertComplete();
                }, () -> {
                    Assert.fail("Not suppose to complete if error thrown");
                });
    }

    @Test
    public void assertCanDisconnect() {
        Response response = new Response();
        response.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        response.setService("00008888-0000-1000-8000-00805f9b34fb");

        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));

        hardwareConnectionLayer.connect(expectedDevice, false)
                .andThen(hardwareConnectionLayer.disconnect(expectedDevice))
                .andThen(hardwareConnectionLayer.listenResponses(expectedDevice, response))
                .timeout(200, TimeUnit.MILLISECONDS)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(TimeoutException.class);
    }

    @Test
    public void shouldEmmitErrorIfConnectionNotEstablished() {
        Response response = new Response();
        response.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        response.setService("00008888-0000-1000-8000-00805f9b34fb");

        hardwareConnectionLayer.listenResponses(expectedDevice, response)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(Exception.class);
    }

    @Test
    public void shouldEmmitResponseFrameWhenListenNotifications() {
        Response response = new Response();
        response.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        response.setService("00008888-0000-1000-8000-00805f9b34fb");
        response.setType(ResponseType.notification.name());

        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        byte[][] bytesArray = new byte[][] {
                BytesUtils.hexStringToByteArray("01"),
                BytesUtils.hexStringToByteArray("02"),
                BytesUtils.hexStringToByteArray("03")
        };
        when(mockBleConnection.setupNotification(any(UUID.class))).thenReturn(Observable.just(Observable.fromArray(bytesArray)));

        hardwareConnectionLayer.connect(expectedDevice, false)
                .andThen(hardwareConnectionLayer.listenResponses(expectedDevice, response))
                .subscribe(observer);

        observer.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        observer.assertValueCount(3);
        observer.assertValues("01","02","03");
    }

    @Test
    public void shouldEmmitResponseFrameWithNotificationWhenResponseTypeNotFound() {
        Response response = new Response();
        response.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        response.setService("00008888-0000-1000-8000-00805f9b34fb");

        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        when(mockBleConnection.setupNotification(any(UUID.class))).thenReturn(Observable.just(Observable.fromArray(new byte[][]{})));

        hardwareConnectionLayer.connect(expectedDevice, false)
                .andThen(hardwareConnectionLayer.listenResponses(expectedDevice, response))
                .subscribe(observer);

        observer.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        verify(mockBleConnection, times(1)).setupNotification(any(UUID.class));
        verify(mockBleConnection, never()).setupIndication(any(UUID.class));
    }

    @Test
    public void shouldEmmitResponseFrameWithNotificationWhenResponseTypeNotValid() {
        Response response = new Response();
        response.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        response.setService("00008888-0000-1000-8000-00805f9b34fb");
        response.setType("blabla");

        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        when(mockBleConnection.setupNotification(any(UUID.class))).thenReturn(Observable.just(Observable.fromArray(new byte[][]{})));

        hardwareConnectionLayer.connect(expectedDevice, false)
                .andThen(hardwareConnectionLayer.listenResponses(expectedDevice, response))
                .subscribe(observer);

        observer.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        verify(mockBleConnection, times(1)).setupNotification(any(UUID.class));
        verify(mockBleConnection, never()).setupIndication(any(UUID.class));
    }

    @Test
    public void shouldEmmitResponseFrameWhenListenIndications() {
        Response response = new Response();
        response.setCharacteristic("00007777-0000-1000-8000-00805f9b34fb");
        response.setService("00008888-0000-1000-8000-00805f9b34fb");
        response.setType(ResponseType.indication.name());

        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        byte[][] bytesArray = new byte[][] {
                BytesUtils.hexStringToByteArray("01"),
                BytesUtils.hexStringToByteArray("02"),
                BytesUtils.hexStringToByteArray("03")
        };
        when(mockBleConnection.setupIndication(any(UUID.class))).thenReturn(Observable.just(Observable.fromArray(bytesArray)));

        hardwareConnectionLayer.connect(expectedDevice, false)
                .andThen(hardwareConnectionLayer.listenResponses(expectedDevice, response))
                .subscribe(observer);

        observer.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        observer.assertValueCount(3);
        observer.assertValues("01","02","03");
    }

    @Test
    public void shouldNotSendCommandIfNoExistingConnection() {
        hardwareConnectionLayer.sendCommand(expectedDevice, command.getRequest(), "ffff")
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(Exception.class);
    }

    @Test
    public void shouldSendCommandFromString() {
        String commandString = "0a0f";
        String responseString = "aaaa";
        hardwareConnectionLayer.getDevicesConnection().put(expectedDevice, mockBleConnection);
        when(mockBleConnection.writeCharacteristic(any(UUID.class), any(byte[].class))).thenReturn(Single.just(BytesUtils.hexStringToByteArray(responseString)));

        hardwareConnectionLayer.sendCommand(expectedDevice, command.getRequest(), commandString)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValue(responseString);
    }

    @Test
    public void shouldSendCommandFromByteArray() {
        byte[] commandArray = BytesUtils.hexStringToByteArray("0a0f");
        String responseString = "aaaa";
        hardwareConnectionLayer.getDevicesConnection().put(expectedDevice, mockBleConnection);
        when(mockBleConnection.writeCharacteristic(any(UUID.class), any(byte[].class))).thenReturn(Single.just(BytesUtils.hexStringToByteArray(responseString)));

        hardwareConnectionLayer.sendCommand(expectedDevice, command.getRequest(), commandArray)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValue(responseString);
    }

    @Test
    public void shouldEmmitAnErrorIfNotReceiveWriteConfirmation() {
        byte[] commandArray = BytesUtils.hexStringToByteArray("0a0f");
        String responseString = "aaaa";
        hardwareConnectionLayer.getDevicesConnection().put(expectedDevice, mockBleConnection);
        when(mockBleConnection.writeCharacteristic(any(UUID.class), any(byte[].class))).thenReturn(Single.error(new Exception("Something bad happened")));

        hardwareConnectionLayer.sendCommand(expectedDevice, command.getRequest(), commandArray)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(Exception.class);
    }

    @Test
    public void shouldScanDeviceSuccessfully() {

        when(mockRxBleClient.scanBleDevices()).thenReturn(Observable.just(rxMockScanResult));
        when(rxMockScanResult.getBleDevice()).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getName()).thenReturn(expectedDevice.getName());
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());

        hardwareConnectionLayer.scan(expectedDevice.getName())
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValue(expectedDevice);
        Assert.assertTrue(hardwareConnectionLayer.getScanDisposable().isDisposed());
        Assert.assertEquals(1, hardwareConnectionLayer.getBleDevices().size());
        Assert.assertEquals(mockrxBleDevice, hardwareConnectionLayer.getBleDevices().get(expectedDevice));
    }

    @Test
    public void shouldRaiseAnErrorIfNoDeviceFound() {

        when(mockRxBleClient.scanBleDevices()).thenReturn(Observable.empty());

        hardwareConnectionLayer.scan(expectedDevice.getName())
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(NoSuchElementException.class);
        Assert.assertTrue(hardwareConnectionLayer.getScanDisposable().isDisposed());
        Assert.assertEquals(0, hardwareConnectionLayer.getBleDevices().size());
    }

    @Test
    public void shouldNotSucceedScanIfNoDeviceFound() {

        when(mockRxBleClient.scanBleDevices()).thenReturn(Observable.just(rxMockScanResult));
        when(rxMockScanResult.getBleDevice()).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getName()).thenReturn("blala");
        when(mockrxBleDevice.getMacAddress()).thenReturn(expectedDevice.getMacAddress());

        hardwareConnectionLayer.scan(expectedDevice.getName())
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(NoSuchElementException.class);
        Assert.assertTrue(hardwareConnectionLayer.getScanDisposable().isDisposed());
        Assert.assertEquals(0, hardwareConnectionLayer.getBleDevices().size());
    }

    @Test
    public void shouldReturnNotConnectedIfDeviceNull() {
        hardwareConnectionLayer.isConnected(null)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValue(false);
    }

    @Test
    public void shouldReturnNotConnectedIfBleDeviceIsNotFound() {
        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(null);
        hardwareConnectionLayer.isConnected(expectedDevice)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValue(false);
    }

    @Test
    public void shouldReturnNotConnectedIfBleDeviceInMapButNotConnected() {
        hardwareConnectionLayer.getBleDevices().put(expectedDevice, mockrxBleDevice);
        when(mockrxBleDevice.getConnectionState()).thenReturn(RxBleConnection.RxBleConnectionState.CONNECTING);
        hardwareConnectionLayer.isConnected(expectedDevice)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValue(false);
    }

    @Test
    public void shouldReturnNotConnectedIfBleDeviceNotInMapButInBleClientNotConnected() {
        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getConnectionState()).thenReturn(RxBleConnection.RxBleConnectionState.CONNECTING);
        hardwareConnectionLayer.isConnected(expectedDevice)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValue(false);
    }

    @Test
    public void shouldReturnConnectedIfBleDeviceFromAnywhereIsConnected() {
        when(mockRxBleClient.getBleDevice(expectedDevice.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getConnectionState()).thenReturn(RxBleConnection.RxBleConnectionState.CONNECTED);
        hardwareConnectionLayer.isConnected(expectedDevice)
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        observer.assertValue(true);
    }
}
