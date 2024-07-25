package com.example.watchlist;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamDetailActivity extends AppCompatActivity {

    private String teamName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_detail);

        // Get the team name from the intent
        teamName = getIntent().getStringExtra("TEAM_NAME");

        // Display the team name
        TextView textViewTeamName = findViewById(R.id.textViewTeamName);
        textViewTeamName.setText(teamName);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set Up Buttons
        Button buttonManageTeam = findViewById(R.id.buttonManageTeam);
        Button buttonWatchLists = findViewById(R.id.buttonWatchLists);

        buttonManageTeam.setOnClickListener(v -> {
            // Handle Manage Team button click
            Intent intent = new Intent(TeamDetailActivity.this, ManageTeamActivity.class);
            intent.putExtra("TEAM_NAME", teamName);
            startActivity(intent);
        });

        buttonWatchLists.setOnClickListener(v -> {
            // Handle Watch Lists button click
            Intent intent = new Intent(TeamDetailActivity.this, WatchListsActivity.class);
            intent.putExtra("TEAM_NAME", teamName);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.team_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this team?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteTeam())
                    .setNegativeButton("No", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteTeam() {
        Call<Void> call = RetrofitClient.getApi().deleteTeam(teamName);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TeamDetailActivity.this, "Team deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(TeamDetailActivity.this, "Error deleting team", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TeamDetailActivity.this, "Error deleting team", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
