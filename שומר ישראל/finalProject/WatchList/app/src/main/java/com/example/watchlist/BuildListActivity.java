package com.example.watchlist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildListActivity extends AppCompatActivity {
    private static final String TAG = "BuildList";
    private String teamName;
    private String listName;
    private List<String> posts;
    private List<String> soldiers;
    private int startHour;
    private int startMinute;
    private int dayStartHour;
    private int dayStartMinute;
    private int dayEndHour;
    private int dayEndMinute;
    private int durationMinutes;
    private int numPosts;
    private int numSoldiers;
    private ViewPager2 viewPager;
    private Button btnChoose;
    private List<String[][]> schedules;
    private List<String[][]> balancedSchedules;
    private SchedulePagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        teamName = getIntent().getStringExtra("TEAM_NAME");
        listName = getIntent().getStringExtra("LIST_NAME");

        viewPager = findViewById(R.id.viewPager);
        btnChoose = findViewById(R.id.btnChoose);

        fetchWatchList();

        btnChoose.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < schedules.size()) {
                String[][] selectedSchedule = schedules.get(currentItem);
                BuildListHelper.saveScheduleToFirestore(teamName, listName, selectedSchedule, "Current Algorithm Schedule", posts, startHour, startMinute, durationMinutes, numSoldiers);
            } else {
                String[][] selectedSchedule = balancedSchedules.get(currentItem - schedules.size());
                BuildListHelper.saveScheduleToFirestore(teamName, listName, selectedSchedule, "Balanced Algorithm Schedule", posts, startHour, startMinute, durationMinutes, numSoldiers);
            }

            new Handler().postDelayed(() -> {
                Intent intent = new Intent(BuildListActivity.this, InspectListActivity.class);
                intent.putExtra("TEAM_NAME", teamName);
                intent.putExtra("LIST_NAME", listName);
                startActivity(intent);
            }, 1500); // 1.5 seconds delay
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_build_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add) {
            return true;
        } else if (id == R.id.action_delete) {
            BuildListHelper.deleteList(teamName, listName);
            Intent intent = new Intent(BuildListActivity.this, WatchListsActivity.class);
            finish();
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchWatchList() {
        BuildListHelper helper = new BuildListHelper(teamName, listName);
        helper.fetchWatchList((posts, soldiers, times, dayTimes, durationMinutes, numPosts, numSoldiers, dayTimeSoldiers, nightTimeSoldiers) -> {
            this.posts = posts;
            this.soldiers = soldiers;
            this.startHour = times[0];
            this.startMinute = times[1];
            this.dayStartHour = dayTimes[0];
            this.dayStartMinute = dayTimes[1];
            this.dayEndHour = dayTimes[2];
            this.dayEndMinute = dayTimes[3];
            this.durationMinutes = durationMinutes;
            this.numPosts = numPosts;
            this.numSoldiers = numSoldiers;

            runInParallel(dayTimeSoldiers, nightTimeSoldiers);
        });
    }

    private void runInParallel(Map<String, Integer> dayTimeSoldiers, Map<String, Integer> nightTimeSoldiers) {
        BuildListHelper.runInParallel(soldiers, startHour, startMinute, numSoldiers, durationMinutes, posts, numPosts, dayTimeSoldiers, nightTimeSoldiers, dayStartHour, dayStartMinute, dayEndHour, dayEndMinute, (schedules, balancedSchedules) -> {
            this.schedules = schedules;
            this.balancedSchedules = balancedSchedules;
            displayScheduleChoices();
        });
    }

    private void displayScheduleChoices() {
        List<String[][]> allSchedules = new ArrayList<>(schedules);
        allSchedules.addAll(balancedSchedules);
        adapter = new SchedulePagerAdapter(this, allSchedules, posts, startHour, startMinute, balancedSchedules, durationMinutes, numSoldiers);
        viewPager.setAdapter(adapter);
    }
}
