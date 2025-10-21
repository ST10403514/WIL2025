package com.example.kineticpulsemobileapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OfflineDataManager {
    private static final String PREFS_NAME = "offline_queue";
    private static final String KEY_QUEUE = "pending_movements";
    private SharedPreferences prefs;
    private Gson gson;

    public OfflineDataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveMovementOffline(JumpDataRequest request) {
        List<JumpDataRequest> queue = getPendingMovements();
        queue.add(request);

        String queueJson = gson.toJson(queue);
        prefs.edit().putString(KEY_QUEUE, queueJson).apply();
    }

    public List<JumpDataRequest> getPendingMovements() {
        String queueJson = prefs.getString(KEY_QUEUE, "[]");
        Type listType = new TypeToken<ArrayList<JumpDataRequest>>(){}.getType();
        return gson.fromJson(queueJson, listType);
    }

    public void clearPendingMovements() {
        prefs.edit().remove(KEY_QUEUE).apply();
    }

    public boolean hasPendingData() {
        return !getPendingMovements().isEmpty();
    }
}