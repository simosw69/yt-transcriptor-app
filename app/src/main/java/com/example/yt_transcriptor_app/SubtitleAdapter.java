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
        notifyDataSetChanged();
    }

    public void setActiveIndex(int index) {
        if (this.activeIndex != index) {
            int oldIndex = this.activeIndex;
            this.activeIndex = index;
            if (oldIndex != -1) {
                notifyItemChanged(oldIndex);
            }
            if (this.activeIndex != -1) {
                notifyItemChanged(this.activeIndex);
            }
        }
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
            holder.tvSubtitle.setTextColor(Color.WHITE);
            holder.tvSubtitle.setTextSize(24);
        } else {
            holder.tvSubtitle.setTextColor(Color.parseColor("#888888"));
            holder.tvSubtitle.setTextSize(20);
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
