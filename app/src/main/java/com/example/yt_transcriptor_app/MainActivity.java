package com.example.yt_transcriptor_app;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

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

    private String currentVideoId = "FWCxJPnsZi4";
    private YouTubePlayer activeYouTubePlayer;  
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private android.widget.TextView tvError;
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
        tvError = findViewById(R.id.tvError);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SubtitleAdapter(subtitleList);
        recyclerView.setAdapter(adapter);

        youTubePlayerView = findViewById(R.id.youtube_player_view);
        if (youTubePlayerView != null) {
            getLifecycle().addObserver(youTubePlayerView);
            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    activeYouTubePlayer = youTubePlayer;
                    youTubePlayer.loadVideo(currentVideoId, 0);
                }

                @Override
                public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                    updateActiveSubtitle(second);
                }

                @Override
                public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError error) {
                    showError("YouTube Player Error: " + error.name() + " (Video may be restricted or unavailable).");
                }
            });
        }

        EditText etVideoLink = findViewById(R.id.etVideoLink);
        View btnLoad = findViewById(R.id.btnLoad);
        if (btnLoad != null && etVideoLink != null) {
            btnLoad.setOnClickListener(v -> {
                String input = etVideoLink.getText().toString().trim();
                if (!input.isEmpty()) {
                    String extractedId = extractVideoId(input);
                    if (extractedId != null) {
                        currentVideoId = extractedId;
                        if (activeYouTubePlayer != null) {
                            activeYouTubePlayer.loadVideo(currentVideoId, 0);
                        }
                        fetchTranscripts();
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid YouTube link or ID", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a link or ID", Toast.LENGTH_SHORT).show();
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
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            if (tvError != null) tvError.setVisibility(View.GONE);
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL("https://youtube-transcriptor.p.rapidapi.com/transcript?video_id=" + currentVideoId + "&lang=it");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-rapidapi-host", "youtube-transcriptor.p.rapidapi.com");
                conn.setRequestProperty("x-rapidapi-key", BuildConfig.RAPIDAPI_KEY);
                conn.setRequestProperty("Content-Type", "application/json");

                int status = conn.getResponseCode();
                
                if (status == 403 || status == 401) {
                    showError("API Error: Expired key or invalid subscription.");
                    return;
                } else if (status == 429) {
                    showError("API Rate Limit Exceeded (HTTP 429): Too many requests. Please try again later.");
                    return;
                } else if (status == 404) {
                    showError("Video not found or transcripts unavailable for this video.");
                    return;
                } else if (status < 200 || status >= 300) {
                    showError("Server error (HTTP " + status + "). Please try again.");
                    return;
                }

                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                reader.close();

                String responseStr = response.toString();
                
                // Se la risposta è un JSON object (es. {"message": "..."} o {"error": "..."}), estraiamo il messaggio d'errore.
                if (responseStr.trim().startsWith("{")) {
                    org.json.JSONObject errorObj = new org.json.JSONObject(responseStr);
                    String apiMessage = errorObj.optString("message", null);
                    if (apiMessage == null || apiMessage.isEmpty()) {
                        apiMessage = errorObj.optString("error", null);
                    }
                    if (apiMessage == null || apiMessage.isEmpty()) {
                        apiMessage = errorObj.optString("detail", null);
                    }
                    if (apiMessage == null || apiMessage.isEmpty()) {
                        apiMessage = responseStr; // Fallback: mostra tutto il JSON
                    }
                    showError("API Error: " + apiMessage);
                    return;
                }

                JSONArray jsonArray = new JSONArray(responseStr);
                if (jsonArray.length() > 0) {
                    org.json.JSONObject videoObj = jsonArray.getJSONObject(0);
                    org.json.JSONArray transArray = videoObj.getJSONArray("transcription");
                    
                    List<Subtitle> parsedSubtitles = new ArrayList<>();
                    for (int i = 0; i < transArray.length(); i++) {
                        org.json.JSONObject subObj = transArray.getJSONObject(i);
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
                    showError("No transcripts found for this video.");
                }

            } catch (java.io.IOException e) {
                e.printStackTrace();
                showError("Network error: check your internet connection.");
            } catch (org.json.JSONException e) {
                e.printStackTrace();
                showError("Parsing error: invalid transcript data format.");
            } catch (Exception e) {
                e.printStackTrace();
                showError("An unexpected error occurred.");
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            subtitleList.clear();
            adapter.notifyDataSetChanged();
            if (tvError != null) {
                tvError.setText(message);
                tvError.setVisibility(View.VISIBLE);
            }
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private String extractVideoId(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        input = input.trim();
        
        // If it's already a valid 11-char ID
        if (input.matches("^[a-zA-Z0-9_-]{11}$")) {
            return input;
        }
        
        // Regex pattern to extract video ID from YouTube URLs
        String regex = "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com\\/(?:watch\\?(?:.*&)?v=|embed\\/|v\\/|shorts\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }
}