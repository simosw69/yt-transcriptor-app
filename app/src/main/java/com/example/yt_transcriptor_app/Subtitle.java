package com.example.yt_transcriptor_app;

public class Subtitle {
    private String subtitle;
    private double start;
    private double dur;

    public Subtitle(String subtitle, double start, double dur) {
        this.subtitle = subtitle;
        this.start = start;
        this.dur = dur;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public double getStart() {
        return start;
    }

    public double getDur() {
        return dur;
    }
}
