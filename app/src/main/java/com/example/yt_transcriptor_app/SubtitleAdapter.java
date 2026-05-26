package com.example.yt_transcriptor_app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SubtitleAdapter extends RecyclerView.Adapter<SubtitleAdapter.ViewHolder> {

    private List<Subtitle> subtitles;
    private int activeIndex = -1;

    public SubtitleAdapter(List<Subtitle> subtitles) {
        this.subtitles = subtitles;
    }

    public void updateData(List<Subtitle> subtitles) {
        this.subtitles = subtitles;
        this.activeIndex = -1; // Reset active index
        notifyDataSetChanged();
    }

    public void setActiveIndex(int index) {
        if (this.activeIndex == index) return;
        int oldIndex = this.activeIndex;
        this.activeIndex = index;
        
        if (subtitles != null) {
            if (oldIndex >= 0 && oldIndex < subtitles.size()) {
                notifyItemChanged(oldIndex);
            }
            if (index >= 0 && index < subtitles.size()) {
                notifyItemChanged(index);
            }
        }
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subtitle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subtitle subtitle = subtitles.get(position);
        holder.tvSubtitle.setText(subtitle.getSubtitle());
        
        if (position == activeIndex) {
            // Highlighted active subtitle: Full opacity, bright white, larger size
            holder.tvSubtitle.setTextColor(Color.WHITE);
            holder.tvSubtitle.setTextSize(22);
            holder.tvSubtitle.setAlpha(1.0f);
        } else {
            // Inactive subtitles: Dimmed, smaller size
            holder.tvSubtitle.setTextColor(Color.parseColor("#B3FFFFFF")); // 70% opacity white
            holder.tvSubtitle.setTextSize(18);
            holder.tvSubtitle.setAlpha(0.4f);
        }
    }

    @Override
    public int getItemCount() {
        return subtitles != null ? subtitles.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubtitle;

        public ViewHolder(View itemView) {
            super(itemView);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }
}
