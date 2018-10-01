package com.telen.ble.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.mock.MockContext;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.Timeout;
import com.telen.ble.manager.layers.impl.BleHardwareConnectionLayer;
import com.telen.ble.manager.model.Device;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HardwareLayerTests {

    @Mock BleHardwareConnectionLayer hardwareConnectionLayer;
    @Mock RxBleClient mockRxBleClient;
    @Mock BluetoothAdapter mockBluetoothAdapter;

    @Mock RxBleDevice mockrxBleDevice;
    @Mock RxBleConnection mockBleConnection;
    @Mock BluetoothDevice mockBluetoothDevice;

    private MockContext mockContext;
    private Device device;
    private TestObserver observer = new TestObserver();

    private class MockedContext extends MockContext {
        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
            return super.registerReceiver(receiver, filter);
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
        hardwareConnectionLayer = new BleHardwareConnectionLayer(mockRxBleClient, mockBluetoothAdapter, mockContext);
        device = new Device("mydevice", "mac@");
    }

    @Test
    public void shouldNotConnectIfDeviceIsNull() {
        hardwareConnectionLayer.connect(null, false).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertError(IllegalArgumentException.class);
    }

    @Test
    public void shouldNot() {

    }

    @Test
    public void should() {

    }

    @Test
    public void shouldCleanMemoryAfterMultiConnection_connect() {
        when(mockRxBleClient.getBleDevice(device.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(device.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        hardwareConnectionLayer.connect(device, false)
                .andThen(hardwareConnectionLayer.connect(device, false))
                .andThen(hardwareConnectionLayer.connect(device, false))
                .andThen(hardwareConnectionLayer.connect(device, false))
                .andThen(hardwareConnectionLayer.connect(device, false))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals(hardwareConnectionLayer.getBleDevices().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesConnection().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().size(), 1);
    }

    @Test
    public void shouldCleanMemoryAfterMultiConnection_alreadyBonded() {
        when(mockRxBleClient.getBleDevice(device.getMacAddress())).thenReturn(mockrxBleDevice);
        when(mockrxBleDevice.getMacAddress()).thenReturn(device.getMacAddress());
        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
        when(mockBluetoothDevice.getAddress()).thenReturn(device.getMacAddress());
        Set<BluetoothDevice> bondedDevices = new HashSet<>();
        bondedDevices.add(mockBluetoothDevice);
        when(mockBluetoothAdapter.getBondedDevices()).thenReturn(bondedDevices);
        hardwareConnectionLayer.connect(device, true)
                .andThen(hardwareConnectionLayer.connect(device, true))
                .andThen(hardwareConnectionLayer.connect(device, true))
                .andThen(hardwareConnectionLayer.connect(device, true))
                .andThen(hardwareConnectionLayer.connect(device, true))
                .subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete();
        Assert.assertEquals(hardwareConnectionLayer.getBleDevices().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesConnection().size(), 1);
        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().size(), 1);
    }

//    @Test
//    public void shouldCleanMemoryAfterMultiConnection_bond() {
//        when(mockRxBleClient.getBleDevice(device.getMacAddress())).thenReturn(mockrxBleDevice);
//        when(mockrxBleDevice.getMacAddress()).thenReturn(device.getMacAddress());
//        when(mockrxBleDevice.observeConnectionStateChanges()).thenReturn(Observable.empty());
//        when(mockrxBleDevice.establishConnection(any(Boolean.class), any(Timeout.class))).thenReturn(Observable.just(mockBleConnection));
//        doNothing().when(mockrxBleDevice.getBluetoothDevice());
//
//        when(mockContext.registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class)))
//        hardwareConnectionLayer.connect(device, true)
//                .andThen(hardwareConnectionLayer.connect(device, true))
//                .andThen(hardwareConnectionLayer.connect(device, true))
//                .andThen(hardwareConnectionLayer.connect(device, true))
//                .andThen(hardwareConnectionLayer.connect(device, true))
//                .subscribe(observer);
//        observer.awaitTerminalEvent();
//        observer.assertComplete();
//        Assert.assertEquals(hardwareConnectionLayer.getBleDevices().size(), 1);
//        Assert.assertEquals(hardwareConnectionLayer.getDevicesConnection().size(), 1);
//        Assert.assertEquals(hardwareConnectionLayer.getDevicesDisposable().size(), 1);
//    }
}
