package com.example.simpleclient;

public class GoogleUser {

    private final String googleId;
    private final String name;
    private final String email;
    private final String picture;
    private final String accessToken;
    private final String refreshToken;

    public GoogleUser(
            String googleId,
            String name,
            String email,
            String picture,
            String accessToken,
            String refreshToken) {

        this.googleId = googleId;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPicture() {
        return picture;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}