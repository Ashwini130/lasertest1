package com.example.lasertest1;

/**
 * Created by Ashwini on 17-02-2018.
 */

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class PreferenceSettingActivity extends PreferenceActivity {

    public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.setpref);

            ((Preference) findPreference("border")).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Intent intent = new Intent(PreferenceSettingActivity.this, border.class);

                    Log.v("value","STring"+newValue);
                    boolean isChecked = (Boolean) newValue;
                    if (!isChecked) {
                        stopService(intent);

                    } else {
                        startService(intent);
                    }
                    return true;
                }
            });

        }

    }

