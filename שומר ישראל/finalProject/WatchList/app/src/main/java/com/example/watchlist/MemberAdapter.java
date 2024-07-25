package com.example.watchlist;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class MemberAdapter extends ArrayAdapter<Map.Entry<String, String>> {

    private Context mContext;
    private List<Map.Entry<String, String>> memberList;
    private MemberActionListener memberActionListener;
    private boolean isSelectableMode = false;
    private SparseBooleanArray selectedItems = new SparseBooleanArray();
    private View currentLongPressedView;

    public MemberAdapter(@NonNull Context context, List<Map.Entry<String, String>> list, MemberActionListener listener) {
        super(context, 0, list);
        mContext = context;
        memberList = list;
        memberActionListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_member, parent, false);
        }

        Map.Entry<String, String> member = memberList.get(position);
        TextView textViewName = convertView.findViewById(R.id.textViewName);
        TextView textViewPhone = convertView.findViewById(R.id.textViewPhone);
        ImageView editIcon = convertView.findViewById(R.id.editIcon);
        ImageView deleteIcon = convertView.findViewById(R.id.deleteIcon);
        CheckBox checkBoxSelect = convertView.findViewById(R.id.checkBoxSelect);

        textViewName.setText(member.getKey());
        textViewPhone.setText(member.getValue());

        if (member.getValue() != null && !member.getValue().isEmpty()) {
            textViewPhone.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + member.getValue()));
                mContext.startActivity(intent);
            });
        } else {
            textViewPhone.setOnClickListener(null);
        }

        if (isSelectableMode) {
            checkBoxSelect.setVisibility(View.VISIBLE);
            checkBoxSelect.setChecked(selectedItems.get(position, false));
            editIcon.setVisibility(View.GONE);
            deleteIcon.setVisibility(View.GONE);
        } else {
            checkBoxSelect.setVisibility(View.GONE);
            editIcon.setVisibility(View.VISIBLE);
            deleteIcon.setVisibility(View.VISIBLE);
        }

        View finalConvertView = convertView;
        convertView.setOnClickListener(v -> {
            if (isSelectableMode) {
                toggleSelection(position);
            } else {
                if (currentLongPressedView != null && currentLongPressedView == finalConvertView) {
                    hideFloatingMenu();
                } else {
                    showFloatingMenu(finalConvertView, editIcon, deleteIcon);
                }
            }
        });

        convertView.setOnLongClickListener(v -> {
            if (!isSelectableMode) {
                enterSelectableMode();
                toggleSelection(position);
            }
            return true;
        });

        editIcon.setOnClickListener(v -> {
            hideFloatingMenu();
            memberActionListener.onEditMember(member.getKey());
        });

        deleteIcon.setOnClickListener(v -> {
            hideFloatingMenu();
            memberActionListener.onDeleteMember(member.getKey());
        });

        return convertView;
    }

    private void toggleSelection(int position) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        notifyDataSetChanged();
        memberActionListener.onSelectionChanged(selectedItems.size() > 0);
    }

    public void setSelectableMode(boolean selectableMode) {
        isSelectableMode = selectableMode;
        notifyDataSetChanged();
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public SparseBooleanArray getSelectedItems() {
        return selectedItems;
    }

    private void enterSelectableMode() {
        isSelectableMode = true;
        notifyDataSetChanged();
        memberActionListener.onSelectableModeChanged(true);
    }

    private void showFloatingMenu(View convertView, ImageView editIcon, ImageView deleteIcon) {
        if (currentLongPressedView != null) {
            hideFloatingMenu();
        }
        editIcon.setVisibility(View.VISIBLE);
        deleteIcon.setVisibility(View.VISIBLE);
        currentLongPressedView = convertView;
    }

    public void hideFloatingMenu() {
        if (currentLongPressedView != null) {
            ImageView editIcon = currentLongPressedView.findViewById(R.id.editIcon);
            ImageView deleteIcon = currentLongPressedView.findViewById(R.id.deleteIcon);
            editIcon.setVisibility(View.VISIBLE);
            deleteIcon.setVisibility(View.VISIBLE);
            currentLongPressedView = null;
        }
    }

    public interface MemberActionListener {
        void onEditMember(String memberName);
        void onDeleteMember(String memberName);
        void onSelectableModeChanged(boolean isSelectable);
        void onSelectionChanged(boolean hasSelection);
    }
}
