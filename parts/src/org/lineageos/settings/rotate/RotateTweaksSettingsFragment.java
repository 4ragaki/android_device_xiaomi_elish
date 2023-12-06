/*
 * Copyright (C) 2018 The LineageOS Project
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

package org.lineageos.settings.rotate;

import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;

import org.lineageos.settings.R;

import java.util.Arrays;

public class RotateTweaksSettingsFragment
        extends PreferenceFragment implements OnPreferenceChangeListener {
    private static final String KEY_ROTATE_TWEAKS = "rotate_policy";

    private static final String SETTING_ROTATE_POLICY = "aragaki.rotate.policy";
    private static final String ROTATE_POLICY_DEFAULT = "0";
    private static final String ROTATE_POLICY_LOCK = "1";
    private static final String ROTATE_POLICY_RESTORE = "2";
    private static final String VALID_OPTIONS[] = new String[]{
            ROTATE_POLICY_DEFAULT,
            ROTATE_POLICY_LOCK,
            ROTATE_POLICY_RESTORE
    };

    private ContentResolver mCR;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.rotate_tweaks);

        mCR = getActivity().getContentResolver();

        ListPreference rotateTweaks = (ListPreference)findPreference(KEY_ROTATE_TWEAKS);
        String setting = Settings.System.getString(mCR, SETTING_ROTATE_POLICY);
        boolean anyMatch = Arrays.stream(VALID_OPTIONS).anyMatch(op -> op.equals(setting));
        rotateTweaks.setValue(anyMatch ? setting : ROTATE_POLICY_DEFAULT);
        rotateTweaks.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return Settings.System.putString(mCR, SETTING_ROTATE_POLICY, (String) newValue);
    }
}
