package com.innov4africa.gateway.model;

public class TokenRefreshRequest {
    private String username;
    private String token;

    public TokenRefreshRequest() {}

    public TokenRefreshRequest(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
