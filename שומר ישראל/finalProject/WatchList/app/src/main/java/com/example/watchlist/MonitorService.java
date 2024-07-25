package com.example.watchlist;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class MonitorService extends Service {

    private static final String TAG = "MonitorService";
    private Queue<String> timeQueue;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private Handler handler;
    private Runnable timeCheckRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationUtil.createNotificationChannel(this);
        Log.d(TAG, "MonitorService created");
        timeQueue = new ConcurrentLinkedQueue<>();
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MonitorService started");
        if (intent != null && intent.hasExtra("scheduleList")) {
            timeQueue.clear(); // Clear the queue before loading new times
            loadSchedule(intent);
            logScheduledTimes(); // Log all scheduled times
        }
        startCheckingSchedule();
        return START_STICKY;
    }

    private void loadSchedule(Intent intent) {
        ArrayList<Map<String, String>> scheduleList = (ArrayList<Map<String, String>>) intent.getSerializableExtra("scheduleList");
        if (scheduleList != null) {
            for (Map<String, String> schedule : scheduleList) {
                String timeStr = schedule.get("Time");
                if (timeStr != null) {
                    try {
                        Date time = timeFormat.parse(timeStr);
                        if (time != null) {
                            // Deduct 10 minutes
                            Date notificationTime = new Date(time.getTime() - TimeUnit.MINUTES.toMillis(10));
                            timeQueue.add(timeFormat.format(notificationTime));
                            Log.d(TAG, "Scheduled notification for: " + timeFormat.format(notificationTime));
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing time: " + e.getMessage());
                    }
                }
            }
            Log.d(TAG, "Schedule loaded with " + scheduleList.size() + " items");
        } else {
            Log.e(TAG, "Schedule list is null");
        }
    }

    private void logScheduledTimes() {
        if (!timeQueue.isEmpty()) {
            Log.d(TAG, "Scheduled notification times:");
            for (String time : timeQueue) {
                Log.d(TAG, time);
            }
        } else {
            Log.d(TAG, "No scheduled times found.");
        }
    }

    private void startCheckingSchedule() {
        timeCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkSchedule();
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(1)); // Check every minute
            }
        };
        handler.post(timeCheckRunnable);
    }

    private void checkSchedule() {
        String currentTime = timeFormat.format(new Date());
        Log.d(TAG, "Current time: " + currentTime);

        if (!timeQueue.isEmpty()) {
            String nextScheduledTime = timeQueue.peek();
            if (currentTime.equals(nextScheduledTime)) {
                Log.d(TAG, "Triggering notification for time: " + nextScheduledTime);
                sendNotification(this, "Next Shift", "Next shift starts in 10 minutes.");
                timeQueue.poll(); // Remove the triggered time
            }
        }

        // Log the next scheduled time after processing the current time
        if (!timeQueue.isEmpty()) {
            Log.d(TAG, "Next scheduled time: " + timeQueue.peek());
        } else {
            Log.d(TAG, "No more scheduled times.");
        }
    }

    private void sendNotification(Context context, String title, String message) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission for posting notifications not granted.");
            return;
        }
        Log.d(TAG, "Sending notification: " + title + " - " + message);
        NotificationUtil.sendNotification(context, title, message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timeCheckRunnable);
        Log.d(TAG, "MonitorService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
