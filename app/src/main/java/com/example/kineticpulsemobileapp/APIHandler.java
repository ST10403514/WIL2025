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

    // Optional V2 endpoint that accepts separate front/back/up. Enable on backend before use.
    @POST("/saveJumpDataV2")
    Call<ApiResponse> saveJumpDataV2(@Body ExtendedJumpDataRequest scoreData);
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

// Extended request if backend supports separate front/back/up fields
class ExtendedJumpDataRequest {
    private int leftJump;
    private int rightJump;
    private int upJump;
    private int frontJump;
    private int backJump;
    private String uid;

    public ExtendedJumpDataRequest(int leftJump, int rightJump, int upJump, int frontJump, int backJump, String uid) {
        this.leftJump = leftJump;
        this.rightJump = rightJump;
        this.upJump = upJump;
        this.frontJump = frontJump;
        this.backJump = backJump;
        this.uid = uid;
    }

    public int getLeftJump() { return leftJump; }
    public int getRightJump() { return rightJump; }
    public int getUpJump() { return upJump; }
    public int getFrontJump() { return frontJump; }
    public int getBackJump() { return backJump; }
    public String getUid() { return uid; }
}
