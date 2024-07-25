package com.example.watchlist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.watchlist.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AppSettings";
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String NOTIFICATIONS_KEY = "notifications_enabled";
    private static final String TEAMS_KEY = "teams";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        setAppTheme(isDarkMode);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.fab.setOnClickListener(view -> showAddTeamDialog());

        if (isConnected()) {
            loadTeamsFromApi(binding.buttonContainer);
        } else {
            loadLocalData(binding.buttonContainer);
        }

        // Check notification settings
        boolean notificationsEnabled = sharedPreferences.getBoolean(NOTIFICATIONS_KEY, true);
        if (notificationsEnabled) {
            // Initialize notification logic here if needed
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void setAppTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void loadTeamsFromApi(LinearLayout buttonContainer) {
        RetrofitClient.getApi().getTeams().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Set<String> teamNames = new HashSet<>(response.body());
                    for (String teamName : teamNames) {
                        createButton(buttonContainer, teamName);
                    }
                    saveTeamsToLocal(teamNames);
                } else {
                    Log.w("API", "Error getting teams");
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Log.e("API", "Error: " + t.getMessage());
            }
        });
    }

    private void saveTeamsToLocal(Set<String> teamNames) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(TEAMS_KEY, teamNames);
        editor.apply();
    }

    private void loadLocalData(LinearLayout buttonContainer) {
        Set<String> teamNames = sharedPreferences.getStringSet(TEAMS_KEY, new HashSet<>());
        for (String teamName : teamNames) {
            createButton(buttonContainer, teamName);
        }
    }

    private void createButton(LinearLayout buttonContainer, String collectionName) {
        Button collectionButton = new Button(this);
        collectionButton.setText(collectionName);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 20, 16, 16);
        collectionButton.setLayoutParams(params);
        collectionButton.setOnClickListener(v -> openTeamDetailActivity(collectionName));
        buttonContainer.addView(collectionButton);
    }

    private void showAddTeamDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_team, null);
        builder.setView(dialogView);

        EditText editTextTeamName = dialogView.findViewById(R.id.editTextTeamName);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonAdd = dialogView.findViewById(R.id.buttonAdd);

        AlertDialog dialog = builder.create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonAdd.setOnClickListener(v -> {
            String teamName = editTextTeamName.getText().toString().trim();
            if (!teamName.isEmpty()) {
                addTeamToApi(teamName);
                dialog.dismiss();
            } else {
                Snackbar.make(v, "Team name cannot be empty", Snackbar.LENGTH_LONG).show();
            }
        });

        dialog.show();
    }

    private void addTeamToApi(String teamName) {
        RetrofitClient.getApi().addTeam(new Team(teamName)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    createButton(binding.buttonContainer, teamName);
                    Log.d("API", "Team added successfully");
                } else {
                    Log.w("API", "Error adding team");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("API", "Error: " + t.getMessage());
            }
        });
    }

    private void openTeamDetailActivity(String teamName) {
        Intent intent = new Intent(this, TeamDetailActivity.class);
        intent.putExtra("TEAM_NAME", teamName);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            showAboutDialog();
            return true;

        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_exit) {
            showExitDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void showAboutDialog() {
        String appName = getString(R.string.app_name);
        String appVersion = "1.0";
        String osInfo = "OS Version: " + Build.VERSION.RELEASE;
        String deviceInfo = "Device: " + Build.MODEL;
        String authors = "Authors: Itai Levin";
        String submissionDate = "Submission Date: " + "21/07/24";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About")
                .setMessage(appName + "\n" +
                        "Version: " + appVersion + "\n" +
                        osInfo + "\n" +
                        deviceInfo + "\n" +
                        authors + "\n" +
                        submissionDate)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
