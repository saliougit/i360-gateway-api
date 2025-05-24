package com.innov4africa.gateway.controller;

import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String username;
    private String token;
}
