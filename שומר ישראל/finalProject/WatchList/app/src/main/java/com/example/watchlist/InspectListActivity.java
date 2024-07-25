package com.example.watchlist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InspectListActivity extends AppCompatActivity {
    private static final String TAG = "InspectListActivity";
    private static final int REQUEST_POST_NOTIFICATIONS_PERMISSION = 1001;
    private String teamName;
    private String listName;
    private GridLayout scheduleLayout;
    private List<String> posts = new ArrayList<>();
    private List<Map<String, String>> scheduleList;
    private Button btnSave, btnCancel, btnMonitor, btnStopMonitor;
    private boolean isEditing = false;
    private LinearLayout buttonLayout;
    private boolean monitoring = false; // New boolean to manage monitoring state

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspect_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        teamName = getIntent().getStringExtra("TEAM_NAME");
        listName = getIntent().getStringExtra("LIST_NAME");

        if (teamName == null || listName == null) {
            Log.e(TAG, "Team name or list name is null");
            finish(); // Close the activity if data is not passed correctly
            return;
        }

        scheduleLayout = findViewById(R.id.scheduleLayout);
        btnMonitor = findViewById(R.id.btnMonitor);
        btnMonitor.setOnClickListener(v -> checkNotificationPermission());

        btnStopMonitor = findViewById(R.id.btnStopMonitor); // Initialize stop monitoring button
        btnStopMonitor.setOnClickListener(v -> stopMonitoring());

        fetchWatchList();
    }

    private void checkNotificationPermission() {
        Log.d(TAG, "Checking notification permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS_PERMISSION);
            } else {
                startMonitoring();
            }
        } else {
            startMonitoring();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMonitoring();
            } else {
                Toast.makeText(this, "Permission denied to post notifications", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Permission denied to post notifications");
            }
        }
    }

    private void startMonitoring() {
        Log.d(TAG, "Starting monitoring");

        if (monitoring) {
            stopMonitoring(); // Stop the current monitoring before starting a new one
        }

        monitoring = true; // Set monitoring to true

        // Pass the schedule list to the MonitorService
        Intent serviceIntent = new Intent(this, MonitorService.class);
        serviceIntent.putExtra("scheduleList", (ArrayList<Map<String, String>>) scheduleList);
        startService(serviceIntent);

        // Set up the alarm to trigger the TimeCheckReceiver every 1 minute
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, TimeCheckReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long interval = 60 * 1000; // 1 minute in milliseconds
        long startTime = System.currentTimeMillis();
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTime, interval, pendingIntent);

        Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show();
    }

    private void stopMonitoring() {
        Log.d(TAG, "Stopping monitoring");

        monitoring = false; // Set monitoring to false

        // Stop the MonitorService
        Intent serviceIntent = new Intent(this, MonitorService.class);
        stopService(serviceIntent);

        // Cancel the alarm
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, TimeCheckReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);

        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_inspect_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete) {
            confirmDeleteList();
            return true;
        } else if (id == R.id.action_edit) {
            showEditMenu(findViewById(R.id.action_edit));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteList() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this list?")
                .setPositiveButton("Yes", (dialog, which) -> deleteList())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteList() {
        Call<Void> call = RetrofitClient.getApi().deleteList(teamName, listName);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Document successfully deleted!");
                    Toast.makeText(InspectListActivity.this, "List deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.w(TAG, "Error deleting document. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error deleting document.", t);
            }
        });
    }

    private void showEditMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_edit_options, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_manual_edit) {
                enterManualEditMode();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void enterManualEditMode() {
        isEditing = true;
        scheduleLayout.removeAllViews(); // Clear existing views

        // Add the toolbar again
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add Save and Cancel buttons
        btnSave = new Button(this);
        btnSave.setText("Save");
        btnSave.setOnClickListener(v -> saveChanges());

        btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setOnClickListener(v -> exitEditMode());

        buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.addView(btnSave);
        buttonLayout.addView(btnCancel);
        buttonLayout.setId(View.generateViewId());

        LinearLayout mainLayout = findViewById(R.id.mainLayout);
        mainLayout.addView(buttonLayout);

        displayScheduleEditable(scheduleList);
    }

    private void saveChanges() {
        scheduleList.clear();
        int rowCount = scheduleLayout.getChildCount() / (posts.size() + 1); // Number of rows
        int columnCount = scheduleLayout.getColumnCount();

        for (int i = 1; i < rowCount; i++) { // Start at 1 to skip the header row
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < columnCount; j++) {
                int index = i * columnCount + j;
                if (j == 0) {
                    // Time column (editable)
                    EditText timeEditText = (EditText) scheduleLayout.getChildAt(index);
                    row.put("Time", timeEditText.getText().toString());
                } else {
                    // Post columns (editable)
                    EditText postEditText = (EditText) scheduleLayout.getChildAt(index);
                    row.put(posts.get(j - 1), postEditText.getText().toString());
                }
            }
            scheduleList.add(row);
        }

        saveScheduleToFirestore();
        exitEditMode();
    }

    private void exitEditMode() {
        isEditing = false;
        displaySchedule(scheduleList); // Refresh to display read-only mode

        // Remove Save and Cancel buttons
        if (buttonLayout != null) {
            LinearLayout mainLayout = findViewById(R.id.mainLayout);
            mainLayout.removeView(buttonLayout);
            buttonLayout = null;
        }
    }

    private void fetchWatchList() {
        Call<Map<String, Object>> call = RetrofitClient.getApi().getWatchList(teamName, listName);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    scheduleList = (List<Map<String, String>>) data.get("schedule");
                    if (scheduleList != null && !scheduleList.isEmpty()) {
                        Map<String, String> firstRow = scheduleList.get(0);
                        for (String key : firstRow.keySet()) {
                            if (!key.equals("Time")) {
                                posts.add(key);
                            }
                        }
                        displaySchedule(scheduleList);
                    } else {
                        Log.e(TAG, "Schedule list is empty or null");
                    }
                } else {
                    Log.e(TAG, "Error getting document. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error getting document.", t);
            }
        });
    }

    private void displaySchedule(List<Map<String, String>> scheduleList) {
        scheduleLayout.removeAllViews();
        scheduleLayout.setColumnCount(posts.size() + 1);

        // Add header row
        TextView timeHeader = new TextView(this);
        timeHeader.setText("Time");
        timeHeader.setPadding(8, 8, 8, 8);
        scheduleLayout.addView(timeHeader);

        for (String post : posts) {
            TextView postHeader = new TextView(this);
            postHeader.setText(post);
            postHeader.setPadding(8, 8, 8, 8);
            scheduleLayout.addView(postHeader);
        }

        // Add schedule data
        for (Map<String, String> row : scheduleList) {
            TextView timeTextView = new TextView(this);
            timeTextView.setText(row.get("Time"));
            timeTextView.setPadding(8, 8, 8, 8);
            scheduleLayout.addView(timeTextView);

            for (String post : posts) {
                TextView postTextView = new TextView(this);
                postTextView.setText(row.get(post));
                postTextView.setPadding(8, 8, 8, 8);
                scheduleLayout.addView(postTextView);
            }
        }
    }

    private void displayScheduleEditable(List<Map<String, String>> scheduleList) {
        scheduleLayout.removeAllViews();
        scheduleLayout.setColumnCount(posts.size() + 1);

        // Add header row
        TextView timeHeader = new TextView(this);
        timeHeader.setText("Time");
        timeHeader.setPadding(8, 8, 8, 8);
        scheduleLayout.addView(timeHeader);

        for (String post : posts) {
            TextView postHeader = new TextView(this);
            postHeader.setText(post);
            postHeader.setPadding(8, 8, 8, 8);
            scheduleLayout.addView(postHeader);
        }

        // Add schedule data
        for (Map<String, String> row : scheduleList) {
            EditText timeEditText = new EditText(this);
            timeEditText.setText(row.get("Time"));
            timeEditText.setPadding(8, 8, 8, 8);
            scheduleLayout.addView(timeEditText);

            for (String post : posts) {
                EditText postEditText = new EditText(this);
                postEditText.setText(row.get(post));
                postEditText.setPadding(8, 8, 8, 8);
                scheduleLayout.addView(postEditText);
            }
        }
    }

    private void saveScheduleToFirestore() {
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("schedule", scheduleList);

        Call<Void> call = RetrofitClient.getApi().saveSchedule(teamName, listName, scheduleData);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Schedule successfully saved!");
                } else {
                    Log.w(TAG, "Error saving schedule. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error saving schedule.", t);
            }
        });
    }
}
