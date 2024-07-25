package com.example.watchlist;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNewList extends AppCompatActivity {
    private static final String TAG = "CreateNewList";
    private String teamName;

    private EditText editTextListName;
    private TextView textViewPosts;
    private LinearLayout postsContainer;
    private Button buttonChooseSoldiers;
    private TextView textViewNumSoldiers;
    private TextView textViewStartHour;
    private TextView textViewDayStartHour;
    private TextView textViewDayEndHour;
    private TextView textViewNightTime;
    private EditText editTextDuration;

    private int numPosts = 0;
    private int numSoldiers = 0;
    private int duration = 1;

    private Map<String, String> membersMap = new HashMap<>();
    private List<String> selectedSoldiers = new ArrayList<>();
    private Map<String, Object> listData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_list);

        teamName = getIntent().getStringExtra("TEAM_NAME");

        if (teamName == null) {
            Log.e(TAG, "TEAM_NAME is null");
            finish(); // Optionally, you can finish the activity if teamName is null
            return;
        }

        editTextListName = findViewById(R.id.editTextListName);
        textViewPosts = findViewById(R.id.textViewPosts);
        postsContainer = findViewById(R.id.postsContainer);
        buttonChooseSoldiers = findViewById(R.id.buttonChooseSoldiers);
        textViewNumSoldiers = findViewById(R.id.textViewNumSoldiers);
        textViewStartHour = findViewById(R.id.textViewStartHour);
        textViewDayStartHour = findViewById(R.id.textViewDayStartHour);
        textViewDayEndHour = findViewById(R.id.textViewDayEndHour);
        textViewNightTime = findViewById(R.id.textViewNightTime);
        editTextDuration = findViewById(R.id.editTextDuration);

        findViewById(R.id.buttonDecreasePosts).setOnClickListener(v -> updatePosts(-1));
        findViewById(R.id.buttonIncreasePosts).setOnClickListener(v -> updatePosts(1));

        buttonChooseSoldiers.setOnClickListener(v -> showChooseSoldiersDialog());

        findViewById(R.id.buttonApprove).setOnClickListener(v -> approveList());
        findViewById(R.id.buttonCancel).setOnClickListener(v -> finish());

        textViewDayStartHour.setOnClickListener(v -> showTimePickerDialog(textViewDayStartHour));
        textViewDayEndHour.setOnClickListener(v -> showTimePickerDialog(textViewDayEndHour));
        textViewStartHour.setOnClickListener(v -> showTimePickerDialog(textViewStartHour));

        fetchMembersFromApi();

        // Setup integer filter for duration input
        setupIntegerInputFilter(editTextDuration, 1, 24);
    }

    private void fetchMembersFromApi() {
        RetrofitClient.getApi().getMembers(teamName).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    membersMap = response.body();
                } else {
                    Log.d(TAG, "No such document");
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.w(TAG, "Error getting document", t);
            }
        });
    }

    private void updatePosts(int change) {
        numPosts = Math.max(0, numPosts + change);
        textViewPosts.setText(String.valueOf(numPosts));
        updatePostsFields();
    }

    private void updatePostsFields() {
        postsContainer.removeAllViews();
        for (int i = 0; i < numPosts; i++) {
            LinearLayout postLayout = new LinearLayout(this);
            postLayout.setOrientation(LinearLayout.VERTICAL);

            // Post Name
            EditText editTextPostName = new EditText(this);
            editTextPostName.setHint("Post Name");
            postLayout.addView(editTextPostName);

            // Day Time People Count
            LinearLayout dayTimeLayout = new LinearLayout(this);
            dayTimeLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView dayTimeLabel = new TextView(this);
            dayTimeLabel.setText("Day Time: ");
            dayTimeLayout.addView(dayTimeLabel);

            Button decreaseDayTime = new Button(this);
            decreaseDayTime.setText("-");
            decreaseDayTime.setOnClickListener(v -> updatePeopleInPost(postLayout, -1, "day"));
            dayTimeLayout.addView(decreaseDayTime);

            TextView textViewDayTime = new TextView(this);
            textViewDayTime.setText("1");
            dayTimeLayout.addView(textViewDayTime);

            Button increaseDayTime = new Button(this);
            increaseDayTime.setText("+");
            increaseDayTime.setOnClickListener(v -> updatePeopleInPost(postLayout, 1, "day"));
            dayTimeLayout.addView(increaseDayTime);

            postLayout.addView(dayTimeLayout);

            // Night Time People Count
            LinearLayout nightTimeLayout = new LinearLayout(this);
            nightTimeLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView nightTimeLabel = new TextView(this);
            nightTimeLabel.setText("Night Time: ");
            nightTimeLayout.addView(nightTimeLabel);

            Button decreaseNightTime = new Button(this);
            decreaseNightTime.setText("-");
            decreaseNightTime.setOnClickListener(v -> updatePeopleInPost(postLayout, -1, "night"));
            nightTimeLayout.addView(decreaseNightTime);

            TextView textViewNightTime = new TextView(this);
            textViewNightTime.setText("1");
            nightTimeLayout.addView(textViewNightTime);

            Button increaseNightTime = new Button(this);
            increaseNightTime.setText("+");
            increaseNightTime.setOnClickListener(v -> updatePeopleInPost(postLayout, 1, "night"));
            nightTimeLayout.addView(increaseNightTime);

            postLayout.addView(nightTimeLayout);

            postsContainer.addView(postLayout);
        }
    }

    private void updatePeopleInPost(LinearLayout postLayout, int change, String type) {
        for (int i = 0; i < postLayout.getChildCount(); i++) {
            View child = postLayout.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout childLayout = (LinearLayout) child;
                if (type.equals("day") && childLayout.getChildAt(0) instanceof TextView && ((TextView) childLayout.getChildAt(0)).getText().toString().contains("Day Time")) {
                    TextView peopleCountView = (TextView) childLayout.getChildAt(2); // The TextView for people count is at index 2
                    int count = Integer.parseInt(peopleCountView.getText().toString());
                    count = Math.max(1, count + change); // Minimum value is 1
                    peopleCountView.setText(String.valueOf(count));
                    break;
                } else if (type.equals("night") && childLayout.getChildAt(0) instanceof TextView && ((TextView) childLayout.getChildAt(0)).getText().toString().contains("Night Time")) {
                    TextView peopleCountView = (TextView) childLayout.getChildAt(2); // The TextView for people count is at index 2
                    int count = Integer.parseInt(peopleCountView.getText().toString());
                    count = Math.max(1, count + change); // Minimum value is 1
                    peopleCountView.setText(String.valueOf(count));
                    break;
                }
            }
        }
    }

    private void setupIntegerInputFilter(EditText editText, int min, int max) {
        InputFilter integerFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                try {
                    int input = Integer.parseInt(dest.toString() + source.toString());
                    if (isInRange(min, max, input)) {
                        return null;
                    }
                } catch (NumberFormatException nfe) {
                }
                return "";
            }

            private boolean isInRange(int min, int max, int input) {
                return max > min ? input >= min && input <= max : input >= max && input <= min;
            }
        };

        editText.setFilters(new InputFilter[]{integerFilter});
    }

    private void showTimePickerDialog(TextView timeTextView) {
        int hour = 0;
        int minute = 0;

        String time = timeTextView.getText().toString();
        if (!time.isEmpty()) {
            String[] parts = time.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String timeString = String.format("%02d:%02d", hourOfDay, minuteOfHour);
            timeTextView.setText(timeString);
            if (timeTextView == textViewDayStartHour || timeTextView == textViewDayEndHour) {
                updateNightTime();
            }
        }, hour, minute, true);

        timePickerDialog.show();
    }

    private void updateNightTime() {
        try {
            String dayStart = textViewDayStartHour.getText().toString();
            String dayEnd = textViewDayEndHour.getText().toString();

            int dayStartHour = Integer.parseInt(dayStart.split(":")[0]);
            int dayEndHour = Integer.parseInt(dayEnd.split(":")[0]);

            String nightTime = (dayEndHour + 1) % 24 + ":00 to " + (dayStartHour - 1 + 24) % 24 + ":00";
            textViewNightTime.setText("Night Time: " + nightTime);
        } catch (NumberFormatException e) {
            textViewNightTime.setText("Night Time: 0:00 to 0:00");
        }
    }

    private void showChooseSoldiersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Soldiers");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(layout);

        CheckBox selectAllCheckBox = new CheckBox(this);
        selectAllCheckBox.setText("Choose All");
        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (int i = 1; i < layout.getChildCount(); i++) { // Skip the first child which is the "Choose All" checkbox
                CheckBox checkBox = (CheckBox) layout.getChildAt(i);
                checkBox.setChecked(isChecked);
            }
        });
        layout.addView(selectAllCheckBox);

        for (String memberName : membersMap.keySet()) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(memberName);
            layout.addView(checkBox);
        }

        builder.setView(scrollView);
        builder.setPositiveButton("Approve", (dialog, which) -> {
            selectedSoldiers.clear();
            for (int i = 1; i < layout.getChildCount(); i++) { // Skip the first child which is the "Choose All" checkbox
                CheckBox checkBox = (CheckBox) layout.getChildAt(i);
                if (checkBox.isChecked()) {
                    String soldierName = checkBox.getText().toString();
                    selectedSoldiers.add(soldierName);
                }
            }
            textViewNumSoldiers.setText("Number of Soldiers: " + selectedSoldiers.size());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void approveList() {
        String listName = editTextListName.getText().toString();
        listData.put("listName", listName);
        listData.put("numPosts", numPosts);
        listData.put("numSoldiers", selectedSoldiers.size());
        listData.put("duration", Integer.parseInt(editTextDuration.getText().toString()));
        listData.put("startHour", textViewStartHour.getText().toString());
        listData.put("dayStartHour", textViewDayStartHour.getText().toString());
        listData.put("dayEndHour", textViewDayEndHour.getText().toString());
        listData.put("selectedSoldiers", selectedSoldiers);

        for (int i = 0; i < numPosts; i++) {
            LinearLayout postLayout = (LinearLayout) postsContainer.getChildAt(i);

            EditText editTextPostName = (EditText) postLayout.getChildAt(0);
            String postName = editTextPostName.getText().toString();

            TextView textViewDayTime = (TextView) ((LinearLayout) postLayout.getChildAt(1)).getChildAt(2);
            TextView textViewNightTime = (TextView) ((LinearLayout) postLayout.getChildAt(2)).getChildAt(2);

            listData.put("post" + (i + 1) + "Name", postName);
            listData.put("post" + (i + 1) + "DayTime", Integer.parseInt(textViewDayTime.getText().toString()));
            listData.put("post" + (i + 1) + "NightTime", Integer.parseInt(textViewNightTime.getText().toString()));
        }

        RetrofitClient.getApi().addList(teamName, new ListData(listData)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "List successfully created!");
                    // Start the BuildList activity with the team name and list name
                    Intent intent = new Intent(CreateNewList.this, BuildListActivity.class);
                    intent.putExtra("TEAM_NAME", teamName);
                    intent.putExtra("LIST_NAME", listName);
                    startActivity(intent);
                    finish(); // Optionally finish this activity if you don't want to go back to it
                } else {
                    Log.w(TAG, "Error creating list");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.w(TAG, "Error creating list", t);
            }
        });
    }
}
