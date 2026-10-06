/*
 * SPDX-FileCopyrightText: 2025-2026 AxionOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.statusbar;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import org.json.JSONArray;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

import java.util.HashSet;
import java.util.Set;

public class DynamicBarSettings extends SettingsPreferenceFragment {

    private static final String SETTINGS_KEY_EVENTS = "ax_dynamic_bar_events";
    private static final String KEY_BATTERY_CHIP_MODE =
            "ax_dynamic_bar_keyguard_battery_chip_mode";

    private static class EventToggle {
        final String typeId;
        final int titleRes;
        final int summaryRes;

        EventToggle(String typeId, int titleRes, int summaryRes) {
            this.typeId = typeId;
            this.titleRes = titleRes;
            this.summaryRes = summaryRes;
        }
    }

    private static final EventToggle[] EVENT_TOGGLES = new EventToggle[] {
        new EventToggle("audio_recording", R.string.dynamic_bar_event_audio_recording, R.string.dynamic_bar_event_audio_recording_summary),
        new EventToggle("media", R.string.dynamic_bar_event_media, R.string.dynamic_bar_event_media_summary),
        new EventToggle("now_playing", R.string.dynamic_bar_event_now_playing, R.string.dynamic_bar_event_now_playing_summary),
        new EventToggle("timer", R.string.dynamic_bar_event_timer, R.string.dynamic_bar_event_timer_summary),
        new EventToggle("stopwatch", R.string.dynamic_bar_event_stopwatch, R.string.dynamic_bar_event_stopwatch_summary),
        new EventToggle("alarm", R.string.dynamic_bar_event_alarm, R.string.dynamic_bar_event_alarm_summary),
        new EventToggle("charging", R.string.dynamic_bar_event_charging, R.string.dynamic_bar_event_charging_summary),
        new EventToggle("bluetooth", R.string.dynamic_bar_event_bluetooth, R.string.dynamic_bar_event_bluetooth_summary),
        new EventToggle("hotspot", R.string.dynamic_bar_event_hotspot, R.string.dynamic_bar_event_hotspot_summary),
        new EventToggle("ringer_mode", R.string.dynamic_bar_event_ringer, R.string.dynamic_bar_event_ringer_summary),
        new EventToggle("vpn", R.string.dynamic_bar_event_vpn, R.string.dynamic_bar_event_vpn_summary),
        new EventToggle("clipboard", R.string.dynamic_bar_event_clipboard, R.string.dynamic_bar_event_clipboard_summary),
        new EventToggle("torch", R.string.dynamic_bar_event_torch, R.string.dynamic_bar_event_torch_summary),
        new EventToggle("promoted_ongoing", R.string.dynamic_bar_event_ongoing, R.string.dynamic_bar_event_ongoing_summary),
        new EventToggle("sports", R.string.dynamic_bar_event_sports, R.string.dynamic_bar_event_sports_summary),
        new EventToggle("app_switch", R.string.dynamic_bar_event_app_switch, R.string.dynamic_bar_event_app_switch_summary),
        new EventToggle("biometric_unlock", R.string.dynamic_bar_event_biometric, R.string.dynamic_bar_event_biometric_summary),
    };

    private Set<String> mDisabledEvents = new HashSet<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dynamic_bar_settings);

        loadDisabledEvents();
        setupEventToggles();
        setupBatteryChipMode();
    }

    private void setupBatteryChipMode() {
        ListPreference pref = findPreference(KEY_BATTERY_CHIP_MODE);
        if (pref == null) return;
        // Stored in Settings.Secure (not LineageSettings) because SystemUI reads it from there.
        int current = Settings.Secure.getIntForUser(requireContext().getContentResolver(),
                KEY_BATTERY_CHIP_MODE, 1, UserHandle.USER_CURRENT);
        pref.setValue(String.valueOf(current));
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.Secure.putIntForUser(requireContext().getContentResolver(),
                    KEY_BATTERY_CHIP_MODE, Integer.parseInt((String) newValue),
                    UserHandle.USER_CURRENT);
            return true;
        });
    }

    private void loadDisabledEvents() {
        mDisabledEvents.clear();
        String json = Settings.Secure.getStringForUser(
                requireContext().getContentResolver(), SETTINGS_KEY_EVENTS, UserHandle.USER_CURRENT);
        if (json != null && !json.isBlank()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    mDisabledEvents.add(arr.getString(i));
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveDisabledEvents() {
        JSONArray arr = new JSONArray();
        for (String event : mDisabledEvents) {
            arr.put(event);
        }
        Settings.Secure.putStringForUser(
                requireContext().getContentResolver(),
                SETTINGS_KEY_EVENTS,
                mDisabledEvents.isEmpty() ? "" : arr.toString(),
                UserHandle.USER_CURRENT);
    }

    private void setupEventToggles() {
        PreferenceCategory category = findPreference("dynamic_bar_events_category");
        if (category == null) return;

        Context context = requireContext();
        for (EventToggle toggle : EVENT_TOGGLES) {
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
            pref.setKey("event_" + toggle.typeId);
            pref.setTitle(toggle.titleRes);
            pref.setSummary(toggle.summaryRes);
            pref.setChecked(!mDisabledEvents.contains(toggle.typeId));
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    mDisabledEvents.remove(toggle.typeId);
                } else {
                    mDisabledEvents.add(toggle.typeId);
                }
                saveDisabledEvents();
                return true;
            });
            category.addPreference(pref);
        }
    }
}
