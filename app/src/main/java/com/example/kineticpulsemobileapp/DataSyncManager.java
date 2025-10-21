package com.example.kineticpulsemobileapp;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class DataSyncManager {
    private Context context;
    private NetworkManager networkManager;
    private OfflineDataManager offlineManager;
    private FirebaseAuth auth;

    public DataSyncManager(Context context) {
        this.context = context;
        this.networkManager = new NetworkManager(context);
        this.offlineManager = new OfflineDataManager(context);
        this.auth = FirebaseAuth.getInstance();
    }

    public void saveMovementData(int jumpLeft, int jumpRight, int jumpUp, int jumpBack) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        JumpDataRequest request = new JumpDataRequest(jumpLeft, jumpRight, jumpUp,jumpBack, currentUser.getUid());

        if (networkManager.isNetworkAvailable()) {
            saveOnline(request);
        } else {
            saveOffline(request);
        }
    }

    private void saveOnline(JumpDataRequest request) {
        RetrofitInstance.getApi().saveJumpData(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("DataSyncManager", "✅ Data saved online");
                    // After successful online save, sync any pending offline data
                    syncPendingData();
                } else {
                    Log.w("DataSyncManager", "❌ Server error, saving offline");
                    saveOffline(request);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("DataSyncManager", "❌ Network error, saving offline: " + t.getMessage());
                saveOffline(request);
            }
        });
    }

    private void saveOffline(JumpDataRequest request) {
        offlineManager.saveMovementOffline(request);
        Log.i("DataSyncManager", "📱 Data saved offline - pending sync");

        // Notify UI about offline mode (optional)
        if (context instanceof MainActivity) {
            ((MainActivity) context).showOfflineNotification();
        }
    }

    public void syncPendingData() {
        if (!networkManager.isNetworkAvailable()) return;

        List<JumpDataRequest> pendingMovements = offlineManager.getPendingMovements();
        if (pendingMovements.isEmpty()) return;

        Log.i("DataSyncManager", "🔄 Syncing " + pendingMovements.size() + " pending movements...");

        for (JumpDataRequest movement : pendingMovements) {
            RetrofitInstance.getApi().saveJumpData(movement).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful()) {
                        Log.d("DataSyncManager", "✅ Synced pending movement");
                        // Remove from offline queue on success
                        offlineManager.clearPendingMovements();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Log.e("DataSyncManager", "❌ Failed to sync pending movement");
                }
            });
        }
    }

    public boolean isOnline() {
        return networkManager.isNetworkAvailable();
    }

    public boolean hasOfflineData() {
        return offlineManager.hasPendingData();
    }
}