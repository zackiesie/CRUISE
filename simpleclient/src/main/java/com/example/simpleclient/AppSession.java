package com.example.simpleclient;

import java.util.prefs.Preferences;

public class AppSession {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(AppSession.class);

    private static final String TOKEN_KEY =
            "car_rental_app_token";

    private AppSession() {
    }

    public static void saveToken(String token) {

        if (token == null || token.isBlank()) {
            return;
        }

        PREFS.put(TOKEN_KEY, token);
    }

    public static String getToken() {

        return PREFS.get(
                TOKEN_KEY,
                null
        );
    }

    public static void clearToken() {

        PREFS.remove(TOKEN_KEY);
    }

    public static boolean hasToken() {

        String token = getToken();

        return token != null
                && !token.isBlank();
    }
}