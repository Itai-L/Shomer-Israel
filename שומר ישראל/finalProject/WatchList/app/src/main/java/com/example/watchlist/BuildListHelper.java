package com.example.watchlist;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuildListHelper {
    private static final String TAG = "BuildListHelper";
    private String teamName;
    private String listName;

    public BuildListHelper(String teamName, String listName) {
        this.teamName = teamName;
        this.listName = listName;
    }

    public interface FetchCallback {
        void onFetchComplete(List<String> posts, List<String> soldiers, int[] times, int[] dayTimes, int durationMinutes, int numPosts, int numSoldiers, Map<String, Integer> dayTimeSoldiers, Map<String, Integer> nightTimeSoldiers);
    }

    public void fetchWatchList(FetchCallback callback) {
        Call<Map<String, Object>> call = RetrofitClient.getApi().getWatchList(teamName, listName);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    int[] times = parseTime((String) data.get("startHour"));
                    int[] dayTimes = parseDayTimes((String) data.get("dayStartHour"), (String) data.get("dayEndHour"));
                    int durationMinutes = ((Number) data.get("duration")).intValue() * 60;
                    int numPosts = ((Number) data.get("numPosts")).intValue();
                    int numSoldiers = ((Number) data.get("numSoldiers")).intValue();
                    List<String> soldiers = (List<String>) data.get("selectedSoldiers");
                    List<String> posts = new ArrayList<>();
                    Map<String, Integer> dayTimeSoldiers = new HashMap<>();
                    Map<String, Integer> nightTimeSoldiers = new HashMap<>();
                    for (int i = 1; i <= numPosts; i++) {
                        String postName = (String) data.get("post" + i + "Name");
                        int dayTime = ((Number) data.get("post" + i + "DayTime")).intValue();
                        int nightTime = ((Number) data.get("post" + i + "NightTime")).intValue();
                        posts.add(postName);
                        dayTimeSoldiers.put(postName, dayTime);
                        nightTimeSoldiers.put(postName, nightTime);
                    }
                    callback.onFetchComplete(posts, soldiers, times, dayTimes, durationMinutes, numPosts, numSoldiers, dayTimeSoldiers, nightTimeSoldiers);
                } else {
                    Log.e(TAG, "Error fetching document. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error fetching document.", t);
            }
        });
    }

    public static void saveScheduleToFirestore(String teamName, String listName, String[][] schedule, String algorithm, List<String> posts, int startHour, int startMinute, int durationMinutes, int numSoldiers) {
        List<Map<String, String>> scheduleList = new ArrayList<>();
        int timeSlotDuration = algorithm.equals("Balanced Algorithm Schedule") ? 60 : durationMinutes / numSoldiers;
        for (int i = 0; i < schedule[0].length; i++) {
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < schedule.length; j++) {
                String postName = posts.get(j);
                row.put(postName, schedule[j][i]);
            }
            int[] currentTime = addMinutes(startHour, startMinute, i * timeSlotDuration);
            int hour = currentTime[0];
            int minute = roundUpToNearest5(currentTime[1]);
            row.put("Time", String.format("%02d:%02d", hour, minute));
            scheduleList.add(row);
        }

        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("schedule", scheduleList);
        scheduleData.put("timestamp", System.currentTimeMillis());
        scheduleData.put("algorithm", algorithm);

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


    public static void deleteList(String teamName, String listName) {
        Call<Void> call = RetrofitClient.getApi().deleteList(teamName, listName);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Document successfully deleted!");
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

    public interface ParallelCallback {
        void onParallelComplete(List<String[][]> schedules, List<String[][]> balancedSchedules);
    }

    public static void runInParallel(List<String> soldiers, int startHour, int startMinute, int numSoldiers, int durationMinutes, List<String> posts, int numPosts, Map<String, Integer> dayTimeSoldiers, Map<String, Integer> nightTimeSoldiers, int dayStartHour, int dayStartMinute, int dayEndHour, int dayEndMinute, ParallelCallback callback) {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<String[][]> task1 = () -> distributeSoldiersCurrentAlgorithm(soldiers, startHour, startMinute, numSoldiers, durationMinutes, posts, numPosts, dayTimeSoldiers, nightTimeSoldiers, dayStartHour, dayStartMinute, dayEndHour, dayEndMinute);
        Callable<String[][]> task2 = () -> distributeSoldiersBalancedAlgorithm(soldiers, startHour, startMinute, numSoldiers, durationMinutes, posts, numPosts, dayTimeSoldiers, nightTimeSoldiers, dayStartHour, dayStartMinute, dayEndHour, dayEndMinute);

        try {
            Future<String[][]> future1 = executor.submit(task1);
            Future<String[][]> future2 = executor.submit(task2);

            String[][] schedule1 = future1.get();
            String[][] schedule2 = future2.get();

            List<String[][]> schedules = new ArrayList<>();
            List<String[][]> balancedSchedules = new ArrayList<>();
            schedules.add(schedule1);
            balancedSchedules.add(schedule2);

            callback.onParallelComplete(schedules, balancedSchedules);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private static String[][] distributeSoldiersCurrentAlgorithm(List<String> soldiers, int startHour, int startMinute, int numSoldiers, int durationMinutes, List<String> posts, int numPosts, Map<String, Integer> dayTimeSoldiers, Map<String, Integer> nightTimeSoldiers, int dayStartHour, int dayStartMinute, int dayEndHour, int dayEndMinute) {
        Queue<String> queue1 = new LinkedList<>(soldiers);
        Queue<String> queue2 = new LinkedList<>();

        float timeSlotDuration = (float) durationMinutes / numSoldiers;
        if (timeSlotDuration <= 0) {
            Log.e(TAG, "Time slot duration is invalid. Check duration and numSoldiers values.");
            return null;
        }

        int numTimeSlots = (int) Math.ceil(durationMinutes / timeSlotDuration); // Duration in minutes
        String[][] schedule = new String[numPosts][numTimeSlots];

        for (int i = 0; i < numTimeSlots; i++) {
            int[] currentTime = addMinutes(startHour, startMinute, (int) (i * timeSlotDuration));
            int currentHour = currentTime[0];
            int currentMinute = roundUpToNearest5(currentTime[1]);
            for (int j = 0; j < numPosts; j++) {
                String postName = posts.get(j);
                int soldiersNeeded = getSoldiersNeeded(postName, currentHour, currentMinute, dayTimeSoldiers, nightTimeSoldiers, dayStartHour, dayStartMinute, dayEndHour, dayEndMinute);
                StringBuilder assignedSoldiers = new StringBuilder();

                for (int k = 0; k < soldiersNeeded; k++) {
                    if (queue1.isEmpty()) {
                        Queue<String> temp = queue1;
                        queue1 = queue2;
                        queue2 = temp;
                    }
                    if (!queue1.isEmpty()) {
                        String soldier = queue1.poll();
                        assignedSoldiers.append(soldier).append(", ");
                        queue2.offer(soldier);
                    }
                }

                schedule[j][i] = assignedSoldiers.length() > 0 ? assignedSoldiers.substring(0, assignedSoldiers.length() - 2) : "";
            }
        }

        return schedule;
    }

    private static String[][] distributeSoldiersBalancedAlgorithm(List<String> soldiers, int startHour, int startMinute, int numSoldiers, int durationMinutes, List<String> posts, int numPosts, Map<String, Integer> dayTimeSoldiers, Map<String, Integer> nightTimeSoldiers, int dayStartHour, int dayStartMinute, int dayEndHour, int dayEndMinute) {
        Queue<String> soldierQueue = new LinkedList<>(soldiers);

        int timeSlotDuration = 60; // 1 hour in minutes

        int numTimeSlots = durationMinutes / timeSlotDuration;
        String[][] schedule = new String[numPosts][numTimeSlots];

        for (int i = 0; i < numTimeSlots; i++) {
            int[] currentTime = addMinutes(startHour, startMinute, i * timeSlotDuration);
            int currentHour = currentTime[0];
            int currentMinute = currentTime[1];
            for (int j = 0; j < numPosts; j++) {
                String postName = posts.get(j);
                int soldiersNeeded = getSoldiersNeeded(postName, currentHour, currentMinute, dayTimeSoldiers, nightTimeSoldiers, dayStartHour, dayStartMinute, dayEndHour, dayEndMinute);
                StringBuilder assignedSoldiers = new StringBuilder();

                for (int k = 0; k < soldiersNeeded; k++) {
                    if (!soldierQueue.isEmpty()) {
                        String soldier = soldierQueue.poll();
                        assignedSoldiers.append(soldier).append(", ");
                        soldierQueue.offer(soldier);
                    }
                }

                schedule[j][i] = assignedSoldiers.length() > 0 ? assignedSoldiers.substring(0, assignedSoldiers.length() - 2) : "";
            }
        }

        return schedule;
    }

    private static int getSoldiersNeeded(String postName, int currentHour, int currentMinute, Map<String, Integer> dayTimeSoldiers, Map<String, Integer> nightTimeSoldiers, int dayStartHour, int dayStartMinute, int dayEndHour, int dayEndMinute) {
        boolean isDay = (currentHour > dayStartHour || (currentHour == dayStartHour && currentMinute >= dayStartMinute)) &&
                (currentHour < dayEndHour || (currentHour == dayEndHour && currentMinute <= dayEndMinute));
        return isDay ? dayTimeSoldiers.get(postName) : nightTimeSoldiers.get(postName);
    }

    private static int[] parseTime(String time) {
        String[] parts = time.split(":");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private static int[] parseDayTimes(String dayStart, String dayEnd) {
        int[] dayTimes = new int[4];
        String[] dayStartParts = dayStart.split(":");
        String[] dayEndParts = dayEnd.split(":");
        dayTimes[0] = Integer.parseInt(dayStartParts[0]);
        dayTimes[1] = Integer.parseInt(dayStartParts[1]);
        dayTimes[2] = Integer.parseInt(dayEndParts[0]);
        dayTimes[3] = Integer.parseInt(dayEndParts[1]);
        return dayTimes;
    }

    private static int[] addMinutes(int hour, int minute, int minutesToAdd) {
        int totalMinutes = hour * 60 + minute + minutesToAdd;
        int newHour = (totalMinutes / 60) % 24;
        int newMinute = (totalMinutes % 60);
        return new int[]{newHour, newMinute};
    }

    private static int roundUpToNearest5(int minute) {
        return (int) (Math.ceil(minute / 5.0) * 5) % 60;
    }
}
