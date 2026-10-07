package com.example.shopping;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {
    private static final String PREF_NAME = "SearchHistory";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY = 10;

    private SharedPreferences prefs;

    public SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();

        List<String> history = getHistory();
        // Remove if already exists to move to top
        history.remove(query);
        history.add(0, query);

        // Limit size
        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }

        JSONArray jsonArray = new JSONArray(history);
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply();
    }

    public List<String> getHistory() {
        List<String> history = new ArrayList<>();
        String json = prefs.getString(KEY_HISTORY, null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    history.add(jsonArray.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return history;
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}