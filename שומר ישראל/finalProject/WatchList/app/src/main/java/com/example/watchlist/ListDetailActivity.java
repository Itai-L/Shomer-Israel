package com.example.watchlist;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListDetailActivity extends AppCompatActivity {
    private static final String TAG = "ListDetailActivity";
    private String teamName;
    private String listName;
    private GridLayout gridLayoutListDetails;
    private Button editButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        teamName = getIntent().getStringExtra("TEAM_NAME");
        listName = getIntent().getStringExtra("LIST_NAME");

        // Log the received teamName and listName
        Log.d(TAG, "Received TEAM_NAME: " + teamName);
        Log.d(TAG, "Received LIST_NAME: " + listName);

        gridLayoutListDetails = findViewById(R.id.gridLayoutListDetails);
        editButton = findViewById(R.id.editButton);

        loadListDetails();

        editButton.setOnClickListener(v -> {
            // Implement the edit functionality here
            Log.d(TAG, "Edit button clicked for list: " + listName);
        });
    }

    private void loadListDetails() {
        Call<Map<String, Object>> call = RetrofitClient.getApi().getWatchList(teamName, listName);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "DocumentSnapshot data: " + response.body());

                    Map<String, Object> details = response.body();
                    if (details != null) {
                        displayDetails(details);
                    } else {
                        Log.e(TAG, "No details found for list: " + listName);
                        gridLayoutListDetails.addView(createTextView("No details available"));
                    }
                } else {
                    Log.e(TAG, "Error getting list details: " + response.message());
                    gridLayoutListDetails.addView(createTextView("Error loading details"));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error getting list details: ", t);
                gridLayoutListDetails.addView(createTextView("Error loading details"));
            }
        });
    }

    private void displayDetails(Map<String, Object> details) {
        gridLayoutListDetails.removeAllViews();
        gridLayoutListDetails.setColumnCount(2); // Two columns: one for keys and one for values

        for (Map.Entry<String, Object> entry : details.entrySet()) {
            gridLayoutListDetails.addView(createTextView(entry.getKey()));
            gridLayoutListDetails.addView(createTextView(entry.getValue().toString()));
        }
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        return textView;
    }
}
