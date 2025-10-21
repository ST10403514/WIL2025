package com.example.kineticpulsemobileapp;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitInstance {

    // Base URL for the API
    // Change to your computer's IP address
    private static final String BASE_URL = "http://192.168.1.100:5000/";

    // The Retrofit instance
    private static Retrofit retrofit;

    // Private constructor to prevent instantiation
    private RetrofitInstance() {}

    // Method to get the Retrofit instance lazily
    public static APIHandler getApi() {
        if (retrofit == null) {
            // Configure OkHttpClient with timeout settings
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Create Retrofit instance with OkHttpClient
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)                // Use the configured OkHttpClient
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(APIHandler.class);
    }
}
