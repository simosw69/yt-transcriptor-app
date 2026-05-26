package com.example.yt_transcriptor_app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class MainActivity extends AppCompatActivity {

    private static final String CACHE_FILE_NAME = "transcript_cache.json";
    private String currentVideoId = null;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private android.widget.TextView tvError;
    private SubtitleAdapter adapter;
    private List<Subtitle> subtitleList = new ArrayList<>();

    private YouTubePlayerView youtubePlayerView;
    private YouTubePlayer youtubePlayer = null;
    private boolean isPlayerReady = false;
    private float lastPlaybackTime = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View rootContainer = findViewById(R.id.rootContainer);
        if (rootContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
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

        youtubePlayerView = findViewById(R.id.youtubePlayerView);
        getLifecycle().addObserver(youtubePlayerView);
        youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer player) {
                youtubePlayer = player;
                isPlayerReady = true;
                if (currentVideoId != null) {
                    youtubePlayer.cueVideo(currentVideoId, lastPlaybackTime);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        youtubePlayer.pause();
                    }
                }
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer player, float second) {
                lastPlaybackTime = second;
                syncSubtitles(second);
            }
        });

        updateLayoutForOrientation(getResources().getConfiguration().orientation);

        EditText etVideoLink = findViewById(R.id.etVideoLink);
        View btnLoad = findViewById(R.id.btnLoad);
        if (btnLoad != null && etVideoLink != null) {
            btnLoad.setOnClickListener(v -> {
                String input = etVideoLink.getText().toString().trim();
                if (!input.isEmpty()) {
                    String extractedId = extractVideoId(input);
                    if (extractedId != null) {
                        currentVideoId = extractedId;
                        fetchTranscripts();
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid YouTube link or ID", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a link or ID", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (!loadTranscriptCache()) {
            if (tvError != null) {
                tvError.setText("Enter a YouTube link to load transcripts.");
                tvError.setVisibility(View.VISIBLE);
            }
        }
    }

    private void syncSubtitles(float currentTime) {
        if (subtitleList == null || subtitleList.isEmpty()) return;

        int activeIndex = -1;
        for (int i = 0; i < subtitleList.size(); i++) {
            Subtitle sub = subtitleList.get(i);
            if (currentTime >= sub.getStart()) {
                activeIndex = i;
            } else {
                break;
            }
        }

        if (activeIndex != -1 && activeIndex != adapter.getActiveIndex()) {
            final int targetIndex = activeIndex;
            runOnUiThread(() -> {
                adapter.setActiveIndex(targetIndex);
                smoothScrollToPositionCentered(recyclerView, targetIndex);
            });
        }
    }

    private void smoothScrollToPositionCentered(RecyclerView recyclerView, int position) {
        if (recyclerView == null || recyclerView.getLayoutManager() == null) return;
        androidx.recyclerview.widget.LinearSmoothScroller smoothScroller = 
                new androidx.recyclerview.widget.LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return androidx.recyclerview.widget.LinearSmoothScroller.SNAP_TO_START;
            }

            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }
        };
        smoothScroller.setTargetPosition(position);
        recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayoutForOrientation(newConfig.orientation);
    }

    private void updateLayoutForOrientation(int orientation) {
        LinearLayout rootContainer = findViewById(R.id.rootContainer);
        View tvTitle = findViewById(R.id.tvTitle);
        View inputContainer = findViewById(R.id.inputContainer);
        View listContainer = findViewById(R.id.listContainer);

        if (rootContainer == null || youtubePlayerView == null || listContainer == null) return;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rootContainer.setOrientation(LinearLayout.HORIZONTAL);

            if (tvTitle != null) tvTitle.setVisibility(View.GONE);
            if (inputContainer != null) inputContainer.setVisibility(View.GONE);

            youtubePlayerView.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams playerParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.2f
            );
            playerParams.gravity = android.view.Gravity.CENTER_VERTICAL;
            youtubePlayerView.setLayoutParams(playerParams);

            LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1.0f
            );
            listContainer.setLayoutParams(listParams);

            if (youtubePlayer != null && currentVideoId != null) {
                youtubePlayer.play();
            }
        } else {
            rootContainer.setOrientation(LinearLayout.VERTICAL);

            if (tvTitle != null) tvTitle.setVisibility(View.VISIBLE);
            if (inputContainer != null) inputContainer.setVisibility(View.VISIBLE);

            youtubePlayerView.setVisibility(View.GONE);

            LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f
            );
            listContainer.setLayoutParams(listParams);

            if (youtubePlayer != null) {
                youtubePlayer.pause();
            }
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
                        apiMessage = responseStr;
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
                        String text = android.text.Html.fromHtml(subObj.getString("subtitle"), android.text.Html.FROM_HTML_MODE_LEGACY).toString();
                        double start = subObj.getDouble("start");
                        double dur = subObj.getDouble("dur");
                        parsedSubtitles.add(new Subtitle(text, start, dur));
                    }

                    saveTranscriptCache(responseStr);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        subtitleList.clear();
                        subtitleList.addAll(parsedSubtitles);
                        adapter.notifyDataSetChanged();

                        if (isPlayerReady && youtubePlayer != null && currentVideoId != null) {
                            youtubePlayer.loadVideo(currentVideoId, 0f);
                            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                                youtubePlayer.pause();
                            }
                        }
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

    private void saveTranscriptCache(String jsonResponse) {
        try {
            FileOutputStream fos = openFileOutput(CACHE_FILE_NAME, MODE_PRIVATE);
            fos.write(jsonResponse.getBytes());
            fos.close();

            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("last_video_id", currentVideoId)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean loadTranscriptCache() {
        try {
            currentVideoId = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("last_video_id", null);

            FileInputStream fis = openFileInput(CACHE_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            fis.close();

            String cachedJson = sb.toString();
            if (cachedJson.isEmpty()) return false;

            JSONArray jsonArray = new JSONArray(cachedJson);
            if (jsonArray.length() > 0) {
                JSONObject videoObj = jsonArray.getJSONObject(0);
                JSONArray transArray = videoObj.getJSONArray("transcription");

                List<Subtitle> parsedSubtitles = new ArrayList<>();
                for (int i = 0; i < transArray.length(); i++) {
                    JSONObject subObj = transArray.getJSONObject(i);
                    String text = android.text.Html.fromHtml(subObj.getString("subtitle"), android.text.Html.FROM_HTML_MODE_LEGACY).toString();
                    double start = subObj.getDouble("start");
                    double dur = subObj.getDouble("dur");
                    parsedSubtitles.add(new Subtitle(text, start, dur));
                }

                subtitleList.clear();
                subtitleList.addAll(parsedSubtitles);
                adapter.notifyDataSetChanged();

                if (isPlayerReady && youtubePlayer != null && currentVideoId != null) {
                    youtubePlayer.cueVideo(currentVideoId, 0f);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        youtubePlayer.pause();
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

        if (input.matches("^[a-zA-Z0-9_-]{11}$")) {
            return input;
        }

        String regex = "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com\\/(?:watch\\?(?:.*&)?v=|embed\\/|v\\/|shorts\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}