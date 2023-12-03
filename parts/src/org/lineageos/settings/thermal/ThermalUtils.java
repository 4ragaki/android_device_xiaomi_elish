/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class ThermalUtils {
    private static final String TAG = "ThermalUtils";

    private static final String FORCESTOP_CONTROL = "forcestop_control";
    private static final String FORCESTOP_PACKAGE_BILIBILI = "tv.danmaku.bili";
    private static final String FORCESTOP_PACKAGE_BILIBILI_HD = "tv.danmaku.bilibilihd";
    private static final String FORCESTOP_PACKAGE_BILIBILI_IN = "com.bilibili.app.in";
    private static Set<String> sStopSet = null;
    private String mCurrentPower = null;

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_BENCHMARK = 1;
    protected static final int STATE_BROWSER = 2;
    protected static final int STATE_CAMERA = 3;
    protected static final int STATE_DIALER = 4;
    protected static final int STATE_GAMING = 5;
    protected static final int STATE_STREAMING = 6;
    private static final String THERMAL_CONTROL = "thermal_control";
    private static final String THERMAL_STATE_DEFAULT = "0";
    private static final String THERMAL_STATE_BENCHMARK = "10";
    private static final String THERMAL_STATE_BROWSER = "11";
    private static final String THERMAL_STATE_CAMERA = "12";
    private static final String THERMAL_STATE_DIALER = "8";
    private static final String THERMAL_STATE_GAMING = "9";
    private static final String THERMAL_STATE_STREAMING = "14";

    private static final String THERMAL_BENCHMARK = "thermal.benchmark=";
    private static final String THERMAL_BROWSER = "thermal.browser=";
    private static final String THERMAL_CAMERA = "thermal.camera=";
    private static final String THERMAL_DIALER = "thermal.dialer=";
    private static final String THERMAL_GAMING = "thermal.gaming=";
    private static final String THERMAL_STREAMING = "thermal.streaming=";

    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private SharedPreferences mSharedPrefs;

    ThermalUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (sStopSet == null) {
            Set<String> stringSet = mSharedPrefs.getStringSet(FORCESTOP_CONTROL, null);
            if (stringSet == null) sStopSet = new HashSet<>();
            else sStopSet = new HashSet<>(stringSet);
        }
    }


    void initialStopSet(Context context) {
        Set<String> stringSet = mSharedPrefs.getStringSet(FORCESTOP_CONTROL, null);
        if (stringSet == null) {
            PackageManager pm = context.getPackageManager();

//          those suckers keep an unnecessary connection when screen is off
            checkFSPackage(pm, FORCESTOP_PACKAGE_BILIBILI);
            checkFSPackage(pm, FORCESTOP_PACKAGE_BILIBILI_HD);
            checkFSPackage(pm, FORCESTOP_PACKAGE_BILIBILI_IN);
        }
    }

    private void checkFSPackage(PackageManager pm, String pkg) {
        try {
            if (pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)) != null){
                writeForceStopPackage(pkg,true);
            }
        } catch (PackageManager.NameNotFoundException e) { e.printStackTrace(); }
    }

    public static void startService(Context context) {
        if (FileUtils.fileExists(THERMAL_SCONFIG)) {
            context.startServiceAsUser(new Intent(context, ThermalService.class),
                    UserHandle.CURRENT);
        }
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(THERMAL_CONTROL, profiles).apply();
    }

    private String getValue() {
        String value = mSharedPrefs.getString(THERMAL_CONTROL, null);

        if (value != null) {
             String[] modes = value.split(":");
             if (modes.length < 5) value = null;
         }

        if (value == null || value.isEmpty()) {
            value = THERMAL_BENCHMARK + ":" + THERMAL_BROWSER + ":" + THERMAL_CAMERA + ":" +
                    THERMAL_DIALER + ":" + THERMAL_GAMING + ":" + THERMAL_STREAMING;
            writeValue(value);
        }
        return value;
    }

    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(":");
        String finalString;

        switch (mode) {
            case STATE_BENCHMARK:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_BROWSER:
                modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_CAMERA:
                modes[2] = modes[2] + packageName + ",";
                break;
            case STATE_DIALER:
                modes[3] = modes[3] + packageName + ",";
                break;
            case STATE_GAMING:
                modes[4] = modes[4] + packageName + ",";
                break;
            case STATE_STREAMING:
                modes[5] = modes[5] + packageName + ",";
                break;
        }

        finalString = modes[0] + ":" + modes[1] + ":" + modes[2] + ":" + modes[3] + ":" +
                modes[4] + ":" + modes[5];

        writeValue(finalString);
    }

    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;
        if (modes[0].contains(packageName + ",")) {
            state = STATE_BENCHMARK;
        } else if (modes[1].contains(packageName + ",")) {
            state = STATE_BROWSER;
        } else if (modes[2].contains(packageName + ",")) {
            state = STATE_CAMERA;
        } else if (modes[3].contains(packageName + ",")) {
            state = STATE_DIALER;
        } else if (modes[4].contains(packageName + ",")) {
            state = STATE_GAMING;
        } else if (modes[5].contains(packageName + ",")) {
            state = STATE_STREAMING;
        }

        return state;
    }

    protected void setDefaultThermalProfile() {
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_DEFAULT);
    }

    protected void setThermalProfile(String packageName) {
        String value = getValue();
        String modes[];
        String state = THERMAL_STATE_DEFAULT;

        if (value != null) {
            modes = value.split(":");

            if (modes[0].contains(packageName + ",")) {
                state = THERMAL_STATE_BENCHMARK;
            } else if (modes[1].contains(packageName + ",")) {
                state = THERMAL_STATE_BROWSER;
            } else if (modes[2].contains(packageName + ",")) {
                state = THERMAL_STATE_CAMERA;
            } else if (modes[3].contains(packageName + ",")) {
                state = THERMAL_STATE_DIALER;
            } else if (modes[4].contains(packageName + ",")) {
                state = THERMAL_STATE_GAMING;
            } else if (modes[5].contains(packageName + ",")) {
                state = THERMAL_STATE_STREAMING;
            }
        }
        FileUtils.writeLine(THERMAL_SCONFIG, state);
    }

    public void writeForceStopPackage(String packageName, boolean enabled) {
        if (enabled) sStopSet.add(packageName); else sStopSet.remove(packageName);
        mSharedPrefs.edit().putStringSet(FORCESTOP_CONTROL, sStopSet).commit();
    }

    public boolean getForceStopStateForPackage(String packageName) {
        return sStopSet != null && sStopSet.contains(packageName);
    }

    public void onSleepChange(Intent intent, ActivityManager am, MediaSessionManager msm) {
        mCurrentPower = intent.getAction();
        Log.i(TAG,"onSleepChange " + mCurrentPower);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            List<MediaController> sessions = msm.getActiveSessions(null);
            for (String pkg : sStopSet) {
                boolean toBeStopped = true;

                for (MediaController controller : sessions) {
                    int state = controller.getPlaybackState().getState();
                    if (pkg.equals(controller.getPackageName())
                            && state != PlaybackState.STATE_PAUSED && state != PlaybackState.STATE_STOPPED) {
                        toBeStopped = false;

                        MediaController.Callback callback = new MediaController.Callback() {
                            @Override
                            public void onPlaybackStateChanged(PlaybackState state) {
                                super.onPlaybackStateChanged(state);

                                if (mCurrentPower.equals(Intent.ACTION_SCREEN_ON)){
                                    controller.unregisterCallback(this);
                                    Log.i(TAG, "screen is on, ignore stopping: " + pkg);
                                    return;
                                }

                                int playState = state.getState();
                                if (playState == PlaybackState.STATE_PAUSED || playState == PlaybackState.STATE_STOPPED) {
                                    controller.unregisterCallback(this);
                                    am.forceStopPackage(pkg);
                                    Log.i(TAG, pkg + " was stopped. PlaybackState: " + playState);
                                }
                            }
                        };
                        controller.registerCallback(callback);
                        Log.i(TAG, pkg + " is in an active media session.");
                        break;
                    }
                }

                if (toBeStopped) {
                    am.forceStopPackage(pkg);
                    Log.i(TAG, pkg + " was stopped.");
                }
            }
        }
    }
}
