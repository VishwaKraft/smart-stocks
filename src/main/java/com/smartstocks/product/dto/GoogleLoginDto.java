package com.smartstocks.product.dto;

import javax.validation.constraints.NotBlank;

public class GoogleLoginDto {
    @NotBlank(message = "Token is required")
    private String token;

    public GoogleLoginDto() {
    }

    public GoogleLoginDto(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
