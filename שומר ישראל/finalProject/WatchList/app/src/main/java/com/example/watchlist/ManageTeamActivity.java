package com.example.watchlist;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageTeamActivity extends AppCompatActivity implements MemberAdapter.MemberActionListener {

    private static final String TAG = "ManageTeamActivity";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int REQUEST_SELECT_CONTACT = 101;

    private String teamName;
    private List<View> memberViews = new ArrayList<>();
    private Map<String, String> membersMap = new HashMap<>();
    private MemberAdapter adapter;
    private List<Map.Entry<String, String>> memberList = new ArrayList<>();
    private boolean isDeleteMode = false;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_team);

        // Get the team name from the intent
        teamName = getIntent().getStringExtra("TEAM_NAME");
        Log.d(TAG, "Received team name: " + teamName);

        // Display the team name
        TextView textView = findViewById(R.id.textViewManageTeam);
        textView.setText("Manage Team: " + teamName);

        // Set up ListView and Adapter
        ListView listViewMembers = findViewById(R.id.listViewMembers);
        adapter = new MemberAdapter(this, memberList, this);
        listViewMembers.setAdapter(adapter);

        // Fetch existing members from API
        fetchMembersFromApi();

        // Hide floating icons on touch outside
        findViewById(R.id.manage_team_layout).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                adapter.hideFloatingMenu();
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.team_manage_menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            Log.d(TAG, "Add icon clicked");
            showOptionsDialog();
            return true;

        } else if (id == R.id.action_approve_delete) {
            Log.d(TAG, "Approve delete clicked");
            confirmDeleteSelectedMembers();
            return true;
        } else if (id == R.id.action_exit_selectable) {
            Log.d(TAG, "Exit selectable mode clicked");
            exitSelectableMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteSelectedMembers() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete the selected members?")
                .setPositiveButton("Yes", (dialog, which) -> deleteSelectedMembers())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void fetchMembersFromApi() {
        RetrofitClient.getApi().getMembers(teamName).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    membersMap = response.body();
                    displayMembers();
                } else {
                    Log.w(TAG, "Error getting members");
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }

    private void displayMembers() {
        memberList.clear();
        memberList.addAll(membersMap.entrySet());
        adapter.notifyDataSetChanged();
    }

    private void showOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Options");

        String[] options = {"Change Team Name", "Add Members"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showChangeTeamNameDialog();
            } else if (which == 1) {
                showAddMembersDialog();
            }
        });

        builder.show();
    }

    private void showChangeTeamNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Team Name");

        View view = getLayoutInflater().inflate(R.layout.dialog_change_team_name, null);
        EditText editTextNewTeamName = view.findViewById(R.id.editTextNewTeamName);
        builder.setView(view);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTeamName = editTextNewTeamName.getText().toString().trim();
            if (!newTeamName.isEmpty() && !newTeamName.equals(teamName)) {
                changeTeamName(newTeamName);
            } else {
                Toast.makeText(ManageTeamActivity.this, "Invalid or same team name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void changeTeamName(String newTeamName) {
        RetrofitClient.getApi().changeTeamName(teamName, newTeamName).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    teamName = newTeamName;
                    Toast.makeText(ManageTeamActivity.this, "Team name changed to " + teamName, Toast.LENGTH_SHORT).show();
                    restartApp();
                } else {
                    Log.w(TAG, "Error changing team name");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }

    private void restartApp() {
        Intent intent = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        finish();
        startActivity(intent);
    }

    private void showAddMembersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Members");

        String[] options = {"Manual", "From Contacts"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showManualAddMemberDialog();
            } else if (which == 1) {
                checkAndRequestContactsPermission();
            }
        });

        builder.show();
    }

    private void checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("This app requires access to your contacts to add members from your contact list.")
                        .setPositiveButton("Grant", (dialog, which) -> ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS))
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        } else {
            selectContact();
        }
    }

    private void showManualAddMemberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Members Manually");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(layout);

        addMemberField(layout);

        builder.setView(scrollView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            saveToLocalMap(layout);
            saveMembers();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.setNeutralButton("Add More", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> addMemberField(layout));
        });

        dialog.show();
    }

    private void addMemberField(LinearLayout layout) {
        View memberView = getLayoutInflater().inflate(R.layout.dialog_member_entry, null);
        layout.addView(memberView);
        memberViews.add(memberView);
    }

    private void saveToLocalMap(LinearLayout layout) {
        for (View memberView : memberViews) {
            EditText editTextName = memberView.findViewById(R.id.editTextName);
            EditText editTextPhone = memberView.findViewById(R.id.editTextPhone);

            String name = editTextName.getText().toString();
            String phone = editTextPhone.getText().toString();

            if (!name.isEmpty()) {
                phone = PhoneNumberUtils.formatPhoneNumber(phone); // Format the phone number if not empty
                membersMap.put(name, phone.isEmpty() ? "" : phone);
            }
        }
        displayMembers();
    }

    private void saveMembers() {
        RetrofitClient.getApi().updateMembers(teamName, membersMap).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Members successfully updated!");
                } else {
                    Log.w(TAG, "Error updating members");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }

    private void selectContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_SELECT_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_CONTACT && resultCode == RESULT_OK) {
            handleContactSelection(data);
        }
    }

    private void handleContactSelection(Intent data) {
        Cursor cursor = getContentResolver().query(data.getData(),
                null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            int hasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

            if (idIndex >= 0 && nameIndex >= 0 && hasPhoneNumberIndex >= 0) {
                String contactId = cursor.getString(idIndex);
                String name = cursor.getString(nameIndex);
                String hasPhoneNumber = cursor.getString(hasPhoneNumberIndex);

                if (hasPhoneNumber.equalsIgnoreCase("1")) {
                    Cursor phones = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null);

                    if (phones != null && phones.moveToFirst()) {
                        int phoneNumberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        if (phoneNumberIndex >= 0) {
                            String phoneNumber = phones.getString(phoneNumberIndex);
                            phoneNumber = PhoneNumberUtils.formatPhoneNumber(phoneNumber); // Format the phone number
                            membersMap.put(name, phoneNumber);
                        }
                        phones.close();
                    }
                }
            }
            cursor.close();
            saveMembers();
            displayMembers();
        }
    }

    @Override
    public void onEditMember(String memberName) {
        showEditMemberDialog(memberName);
    }

    @Override
    public void onDeleteMember(String memberName) {
        RetrofitClient.getApi().deleteMember(teamName, memberName).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    membersMap.remove(memberName);
                    displayMembers();
                    Log.d(TAG, "Member successfully deleted!");
                } else {
                    Log.w(TAG, "Error deleting member");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }

    private void showEditMemberDialog(String memberName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Member");

        View memberView = getLayoutInflater().inflate(R.layout.dialog_member_entry, null);
        EditText editTextName = memberView.findViewById(R.id.editTextName);
        EditText editTextPhone = memberView.findViewById(R.id.editTextPhone);

        editTextName.setText(memberName);
        editTextPhone.setText(membersMap.get(memberName));

        builder.setView(memberView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = editTextName.getText().toString();
            String newPhone = editTextPhone.getText().toString();

            if (!newName.isEmpty() && !newName.equals(memberName)) {
                membersMap.remove(memberName);
                membersMap.put(newName, newPhone.isEmpty() ? "" : PhoneNumberUtils.formatPhoneNumber(newPhone));
            } else {
                membersMap.put(memberName, newPhone.isEmpty() ? "" : PhoneNumberUtils.formatPhoneNumber(newPhone));
            }
            saveMembers();
            displayMembers();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void toggleDeleteMode() {
        isDeleteMode = !isDeleteMode;
        adapter.setSelectableMode(isDeleteMode);
        if (isDeleteMode) {
            showApproveDeleteMenuItem();
        } else {
            exitSelectableMode();
        }
    }

    private void showApproveDeleteMenuItem() {
        if (menu != null) {
            menu.findItem(R.id.action_approve_delete).setVisible(true);
            menu.findItem(R.id.action_exit_selectable).setVisible(true);
        }
    }

    private void hideApproveDeleteMenuItem() {
        if (menu != null) {
            menu.findItem(R.id.action_approve_delete).setVisible(false);
            menu.findItem(R.id.action_exit_selectable).setVisible(false);
        }
    }

    private void deleteSelectedMembers() {
        SparseBooleanArray selectedItems = adapter.getSelectedItems();
        if (selectedItems.size() == 0) {
            Log.d(TAG, "No items selected for deletion");
            return;
        }

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            int position = selectedItems.keyAt(i);
            String memberName = memberList.get(position).getKey();
            onDeleteMember(memberName);
        }

        displayMembers();
        exitSelectableMode();
    }

    private void exitSelectableMode() {
        isDeleteMode = false;
        adapter.setSelectableMode(false);
        hideApproveDeleteMenuItem();
        adapter.clearSelections();
    }

    @Override
    public void onSelectableModeChanged(boolean isSelectable) {
        if (isSelectable) {
            showApproveDeleteMenuItem();
        } else {
            hideApproveDeleteMenuItem();
        }
    }

    @Override
    public void onSelectionChanged(boolean hasSelection) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectContact();
            } else {
                Toast.makeText(this, "Permission to access contacts denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}




class PhoneNumberUtils {

    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber.startsWith("0")) {
            phoneNumber = "+972" + phoneNumber.substring(1);
        }
        return phoneNumber.replaceAll("[^\\d+]", ""); // Remove any non-digit characters
    }
}
