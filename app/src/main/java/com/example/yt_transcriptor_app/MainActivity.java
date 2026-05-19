package com.example.yt_transcriptor_app;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String VIDEO_ID = "8aGhZQkoFbQ";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SubtitleAdapter adapter;
    private List<Subtitle> subtitleList = new ArrayList<>();
    private YouTubePlayerView youTubePlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SubtitleAdapter(subtitleList);
        recyclerView.setAdapter(adapter);

        youTubePlayerView = findViewById(R.id.youtube_player_view);
        if (youTubePlayerView != null) {
            getLifecycle().addObserver(youTubePlayerView);
            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    youTubePlayer.loadVideo(VIDEO_ID, 0);
                }

                @Override
                public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                    updateActiveSubtitle(second);
                }
            });
        }

        fetchTranscripts();
    }

    private void updateActiveSubtitle(float currentSecond) {
        if (subtitleList.isEmpty()) return;

        int newActiveIndex = -1;
        for (int i = 0; i < subtitleList.size(); i++) {
            Subtitle s = subtitleList.get(i);
            if (currentSecond >= s.getStart() && currentSecond <= (s.getStart() + s.getDur())) {
                newActiveIndex = i;
                break;
            }
        }
        
        // Fallback to the closest past subtitle if we are in a gap
        if (newActiveIndex == -1) {
            for (int i = subtitleList.size() - 1; i >= 0; i--) {
                Subtitle s = subtitleList.get(i);
                if (currentSecond >= s.getStart()) {
                    newActiveIndex = i;
                    break;
                }
            }
        }

        if (newActiveIndex != -1) {
            adapter.setActiveIndex(newActiveIndex);
            // In a real app we might only auto-scroll if user hasn't touched the screen,
            // but for this task we smooth scroll to it automatically like Spotify lyrics
            recyclerView.smoothScrollToPosition(newActiveIndex);
        }
    }

    private void fetchTranscripts() {
        progressBar.setVisibility(View.VISIBLE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL("https://youtube-transcriptor.p.rapidapi.com/transcript?video_id=" + VIDEO_ID + "&lang=en");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-rapidapi-host", "youtube-transcriptor.p.rapidapi.com");
                conn.setRequestProperty("x-rapidapi-key", BuildConfig.RAPIDAPI_KEY);
                conn.setRequestProperty("Content-Type", "application/json");

                int status = conn.getResponseCode();
                InputStream inputStream = (status >= 200 && status < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject videoObj = jsonArray.getJSONObject(0);
                    JSONArray transArray = videoObj.getJSONArray("transcription");
                    
                    List<Subtitle> parsedSubtitles = new ArrayList<>();
                    for (int i = 0; i < transArray.length(); i++) {
                        JSONObject subObj = transArray.getJSONObject(i);
                        // Convert HTML entities like &#39; inside the subtitle
                        String text = android.text.Html.fromHtml(subObj.getString("subtitle"), android.text.Html.FROM_HTML_MODE_LEGACY).toString();
                        double start = subObj.getDouble("start");
                        double dur = subObj.getDouble("dur");
                        parsedSubtitles.add(new Subtitle(text, start, dur));
                    }

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        subtitleList.clear();
                        subtitleList.addAll(parsedSubtitles);
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }
}