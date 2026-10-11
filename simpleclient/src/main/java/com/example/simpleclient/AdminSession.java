package com.example.simpleclient;

import java.util.prefs.Preferences;

public final class AdminSession {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(
                    AdminSession.class
            );

    private static final String TOKEN_KEY =
            "cruise_admin_token";

    private AdminSession() {
    }

    public static void saveToken(
            String token) {

        if (token == null
                || token.isBlank()) {

            return;
        }

        PREFS.put(
                TOKEN_KEY,
                token
        );
    }

    public static String getToken() {

        return PREFS.get(
                TOKEN_KEY,
                null
        );
    }

    public static boolean hasToken() {

        String token =
                getToken();

        return token != null
                && !token.isBlank();
    }

    public static void clearToken() {

        PREFS.remove(
                TOKEN_KEY
        );
    }
}