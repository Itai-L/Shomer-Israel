package com.example.watchlist;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchDarkMode;
    private Switch switchNotifications;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AppSettings";
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String NOTIFICATIONS_KEY = "notifications_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved preferences
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        boolean notificationsEnabled = sharedPreferences.getBoolean(NOTIFICATIONS_KEY, true);

        // Apply saved preferences
        switchDarkMode.setChecked(isDarkMode);
        switchNotifications.setChecked(notificationsEnabled);

        // Set listeners for changes
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setAppTheme(isChecked);
            savePreferences(DARK_MODE_KEY, isChecked);
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreferences(NOTIFICATIONS_KEY, isChecked);
        });
    }

    private void setAppTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void savePreferences(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
}
