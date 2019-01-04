package com.telen.sdk.demo.commonui;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.telen.sdk.common.models.DeviceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {

    private static final String TAG = FirestoreManager.class.getSimpleName();

    private Map<DeviceType, List<ILightActions>> mListeners = new HashMap<>();
    private Map<DeviceType, ListenerRegistration> mFirebaseListeners = new HashMap<>();

    private static final Object mLock = new Object();

    public void registerLightCallback(DeviceType deviceType, ILightActions actionsCallback) {
        synchronized (mLock) {
            if(mListeners.get(deviceType)==null)
                mListeners.put(deviceType, new ArrayList<>());
            mListeners.get(deviceType).add(actionsCallback);
        }
    }

    public void unregisterLightCallback(DeviceType deviceType, ILightActions actionsCallback) {
        synchronized (mLock) {
            if(mListeners.get(deviceType)!=null)
                mListeners.get(deviceType).remove(actionsCallback);
        }
    }

    public void init() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();

        firestore.setFirestoreSettings(settings);
    }

    public void destroy() {
        if(mFirebaseListeners.get(DeviceType.LIGHTBULB)!=null)
            mFirebaseListeners.get(DeviceType.LIGHTBULB).remove();
        if(mFirebaseListeners.get(DeviceType.RIBBON)!=null)
            mFirebaseListeners.get(DeviceType.RIBBON).remove();
    }

    public void lightOn(DeviceType deviceType) {
        synchronized (mLock) {
            List<ILightActions> callbacks = mListeners.get(deviceType);
            if (callbacks != null) {
                for (int i = callbacks.size() - 1; i >= 0; i--) {
                    ILightActions actionsCallback = callbacks.get(i);
                    try {
                        actionsCallback.lightOn();
                    } catch (Exception e) {
                        callbacks.remove(i);
                    }
                }
            }
        }
    }

    public void lightOff(DeviceType deviceType) {
        synchronized (mLock) {
            List<ILightActions> callbacks = mListeners.get(deviceType);
            if (callbacks != null) {
                for (int i = callbacks.size() - 1; i >= 0; i--) {
                    ILightActions actionsCallback = callbacks.get(i);
                    try {
                        actionsCallback.lightOff();
                    } catch (Exception e) {
                        callbacks.remove(i);
                    }
                }
            }
        }
    }

    public void setColor(DeviceType deviceType, int color) {
        synchronized (mLock) {
            List<ILightActions> callbacks = mListeners.get(deviceType);
            if (callbacks != null) {
                for (int i = callbacks.size() - 1; i >= 0; i--) {
                    ILightActions actionsCallback = callbacks.get(i);
                    try {
                        actionsCallback.setColor(color);
                    } catch (Exception e) {
                        callbacks.remove(i);
                    }
                }
            }
        }
    }
}
