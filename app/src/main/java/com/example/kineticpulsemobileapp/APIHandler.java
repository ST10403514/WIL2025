package com.example.kineticpulsemobileapp;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Path;


public interface APIHandler {

    @POST("/register")
    Call<ApiResponse> registerUser(@Body RegisterRequest user);

    @POST("/saveJumpData")
    Call<ApiResponse> saveJumpData(@Body JumpDataRequest scoreData);

    @GET("/getJumpData/{uid}")
    Call<JumpDataResponse> getjumpData(@Path("uid") String userId);
}


class RegisterRequest {

    private String uid;
    private String username;


    public RegisterRequest(String uid, String username) {
        this.uid = uid;
        this.username = username;
    }


    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }


    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

class ApiResponse {

    private boolean success;
    private String message;

    // Constructor
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

class JumpDataResponse {

    private int leftJump;
    private int rightJump;
    private int middleJump;

    // Constructor
    public JumpDataResponse(int leftJump, int rightJump, int middleJump) {
        this.leftJump = leftJump;
        this.rightJump = rightJump;
        this.middleJump = middleJump;
    }

    // Getters
    public int getLeftJump() {
        return leftJump;
    }

    public int getRightJump() {
        return rightJump;
    }

    public int getMiddleJump() {
        return middleJump;
    }

    // Setters
    public void setLeftJump(int leftJump) {
        this.leftJump = leftJump;
    }

    public void setRightJump(int rightJump) {
        this.rightJump = rightJump;
    }

    public void setMiddleJump(int middleJump) {
        this.middleJump = middleJump;
    }
}

class JumpDataRequest {

    private int leftJump;
    private int rightJump;
    private int middleJump;
    private String uid;

    // Constructor
    public JumpDataRequest(int leftJump, int rightJump, int middleJump, String uid) {
        this.leftJump = leftJump;
        this.rightJump = rightJump;
        this.middleJump = middleJump;
        this.uid = uid;
    }

    // Getters
    public int getLeftJump() {
        return leftJump;
    }

    public int getRightJump() {
        return rightJump;
    }

    public int getMiddleJump() {
        return middleJump;
    }

    public String getUid() {
        return uid;
    }

    // Setters
    public void setLeftJump(int leftJump) {
        this.leftJump = leftJump;
    }

    public void setRightJump(int rightJump) {
        this.rightJump = rightJump;
    }

    public void setMiddleJump(int middleJump) {
        this.middleJump = middleJump;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}