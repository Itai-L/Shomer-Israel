package com.example.watchlist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchListsActivity extends AppCompatActivity {
    private Menu menu;
    private String teamName;
    private static final String TAG = "WatchListsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_lists);
        teamName = getIntent().getStringExtra("TEAM_NAME");

        if (teamName == null) {
            Log.e(TAG, "TEAM_NAME is null");
            finish();
            return;
        } else {
            Log.d(TAG, "Received TEAM_NAME: " + teamName);
        }

        TextView textView = findViewById(R.id.textViewWatchLists);
        textView.setText("Watch Lists for Team: " + teamName);

        loadWatchLists();
    }

    private void loadWatchLists() {
        RetrofitClient.getApi().getWatchLists(teamName).enqueue(new Callback<List<WatchList>>() {
            @Override
            public void onResponse(Call<List<WatchList>> call, Response<List<WatchList>> response) {
                if (response.isSuccessful()) {
                    List<WatchList> watchLists = response.body();
                    if (watchLists != null && !watchLists.isEmpty()) {
                        LinearLayout layout = findViewById(R.id.watchListsLayout);
                        for (WatchList watchList : watchLists) {
                            String listName = watchList.getListName();
                            long timestamp = watchList.getTimestamp();

                            // Log the list name and timestamp
                            Log.d(TAG, "List Name: " + listName);
                            Log.d(TAG, "Timestamp: " + timestamp);

                            // Parse the timestamp to date
                            Date date = new Date(timestamp);
                            String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(date);
                            Log.d(TAG, "Formatted Date: " + formattedDate);

                            // Create button dynamically
                            Button button = new Button(WatchListsActivity.this);
                            button.setText(listName + "\n" + formattedDate);
                            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_paper_page, 0, 0, 0);
                            button.setOnClickListener(v -> {
                                Intent intent = new Intent(WatchListsActivity.this, InspectListActivity.class);
                                intent.putExtra("TEAM_NAME", teamName);
                                intent.putExtra("LIST_NAME", listName);
                                startActivity(intent);
                            });

                            layout.addView(button);
                        }
                    } else {
                        Log.d(TAG, "No lists found for team: " + teamName);
                    }
                } else {
                    Log.e(TAG, "Error getting lists: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<WatchList>> call, Throwable t) {
                Log.e(TAG, "Error getting lists: ", t);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.watch_list_menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add) {
            CreateNewList();
            return true;
        } else if (id == R.id.action_approve_delete) {
            // confirmDeleteSelectedMembers();
            return true;
        } else if (id == R.id.action_exit_selectable) {
            // exitSelectableMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void CreateNewList() {
        Intent intent = new Intent(this, CreateNewList.class);
        intent.putExtra("TEAM_NAME", teamName);
        startActivity(intent);
    }
}
