package com.example.watchlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SchedulePagerAdapter extends RecyclerView.Adapter<SchedulePagerAdapter.ViewHolder> {
    private List<String[][]> schedules;
    private List<String> posts;
    private int startHour, startMinute;
    private List<String[][]> balancedSchedules;
    private Context context;
    private int durationMinutes;
    private int numSoldiers;

    public SchedulePagerAdapter(Context context, List<String[][]> schedules, List<String> posts, int startHour, int startMinute, List<String[][]> balancedSchedules, int durationMinutes, int numSoldiers) {
        this.context = context;
        this.schedules = schedules;
        this.posts = posts;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.balancedSchedules = balancedSchedules;
        this.durationMinutes = durationMinutes;
        this.numSoldiers = numSoldiers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[][] schedule = schedules.get(position);
        boolean isBalancedAlgorithm = position >= schedules.size() - balancedSchedules.size();
        float currentTimeSlotDuration = isBalancedAlgorithm ? 60 : (float) durationMinutes / numSoldiers;
        holder.bind(schedule, posts, startHour, startMinute, currentTimeSlotDuration);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private GridLayout gridLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            gridLayout = itemView.findViewById(R.id.gridLayoutSchedule);
        }

        public void bind(String[][] schedule, List<String> posts, int startHour, int startMinute, float timeSlotDuration) {
            gridLayout.removeAllViews();
            gridLayout.setColumnCount(posts.size() + 1);

            // Add header row
            for (String post : posts) {
                gridLayout.addView(createTextView(post));
            }
            gridLayout.addView(createTextView("Time"));

            // Add schedule data
            for (int i = 0; i < schedule[0].length; i++) {
                for (int j = 0; j < schedule.length; j++) {
                    gridLayout.addView(createTextView(schedule[j][i]));
                }
                int[] currentTime = addMinutes(startHour, startMinute, (int) (i * timeSlotDuration));
                int hour = currentTime[0];
                int minute = currentTime[1];
                minute=roundUpToNearest5(minute);
                gridLayout.addView(createTextView(String.format("%02d:%02d", hour, minute)));
            }
        }
        private int roundUpToNearest5(int minute) {
            return (int) (Math.ceil(minute / 5.0) * 5) % 60;
        }


        private TextView createTextView(String text) {
            TextView textView = new TextView(itemView.getContext());
            textView.setText(text);
            textView.setPadding(8, 8, 8, 8);
            return textView;
        }

        private int[] addMinutes(int hour, int minute, int minutesToAdd) {
            int totalMinutes = hour * 60 + minute + minutesToAdd;
            int newHour = (totalMinutes / 60) % 24;
            int newMinute = (totalMinutes % 60);
            return new int[]{newHour, newMinute};
        }
    }
}
