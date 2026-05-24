package com.example.yt_transcriptor_app;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileInputStream;
import java.io.FileOutputStream;

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

    private static final String CACHE_FILE_NAME = "transcript_cache.json";
    private String currentVideoId = null;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private android.widget.TextView tvError;
    private SubtitleAdapter adapter;
    private List<Subtitle> subtitleList = new ArrayList<>();

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

        // Try to load cached transcript instead of making an API call
        if (!loadTranscriptCache()) {
            // No cache — show placeholder, don't waste an API call
            if (tvError != null) {
                tvError.setText("Enter a YouTube link to load transcripts.");
                tvError.setVisibility(View.VISIBLE);
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

                    // Cache the successful response for next startup
                    saveTranscriptCache(responseStr);

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

    private void saveTranscriptCache(String jsonResponse) {
        try {
            FileOutputStream fos = openFileOutput(CACHE_FILE_NAME, MODE_PRIVATE);
            fos.write(jsonResponse.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean loadTranscriptCache() {
        try {
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


}