package com.example.simpleclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;

import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.SecureRandom;

import java.time.Duration;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GoogleDesktopAuthService {

    private static final String AUTHORIZATION_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";

    private static final String TOKEN_ENDPOINT =
            "https://oauth2.googleapis.com/token";

    private static final String USERINFO_ENDPOINT =
            "https://openidconnect.googleapis.com/v1/userinfo";

    private final HttpClient httpClient;

    private final ObjectMapper mapper;

    private final SecureRandom secureRandom;

    public GoogleDesktopAuthService() {

        httpClient =
                HttpClient
                        .newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(20)
                        )
                        .build();

        mapper =
                new ObjectMapper();

        secureRandom =
                new SecureRandom();
    }

    public GoogleUser login()
            throws Exception {

        // =====================================================
        // DESKTOP CLIENT ID
        // =====================================================

        String clientId =
                System.getenv(
                        "GOOGLE_DESKTOP_CLIENT_ID"
                );

        if (clientId == null
                || clientId.isBlank()) {

            throw new IllegalStateException(
                    "GOOGLE_DESKTOP_CLIENT_ID is not configured"
            );
        }

        // =====================================================
        // PKCE
        // =====================================================

        String codeVerifier =
                createCodeVerifier();

        String codeChallenge =
                createCodeChallenge(
                        codeVerifier
                );

        // =====================================================
        // STATE
        // =====================================================

        String state =
                createRandomValue();

        // =====================================================
        // LOCAL CALLBACK SERVER
        //
        // Port 0 = Windows pilih free port automatically.
        // =====================================================

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(
                                "127.0.0.1",
                                0
                        ),
                        0
                );

        int port =
                server
                        .getAddress()
                        .getPort();

        String redirectUri =
                "http://127.0.0.1:"
                + port
                + "/";

        CompletableFuture<String> codeFuture =
                new CompletableFuture<>();

        // =====================================================
        // CALLBACK
        // =====================================================

        server.createContext(
                "/",
                exchange -> {

                    try {

                        handleCallback(
                                exchange,
                                state,
                                codeFuture
                        );

                    } catch (Exception e) {

                        codeFuture
                                .completeExceptionally(e);

                        sendBrowserResponse(
                                exchange,
                                500,
                                """
                                <html>
                                <body>
                                <h2>Google Login Failed</h2>
                                <p>You may return to the Car Rental application.</p>
                                </body>
                                </html>
                                """
                        );
                    }
                }
        );

        server.start();

        try {

            // =================================================
            // GOOGLE AUTHORIZATION URL
            // =================================================

            String authorizationUrl =
                    AUTHORIZATION_ENDPOINT

                    + "?client_id="
                    + encode(clientId)

                    + "&redirect_uri="
                    + encode(redirectUri)

                    + "&response_type=code"

                    + "&scope="
                    + encode(
                            "openid email profile"
                    )

                    + "&code_challenge="
                    + encode(codeChallenge)

                    + "&code_challenge_method=S256"

                    + "&state="
                    + encode(state)

                    + "&access_type=offline"

                    + "&prompt=select_account";

            // =================================================
            // OPEN SYSTEM BROWSER
            // =================================================

            if (!Desktop.isDesktopSupported()) {

                throw new IllegalStateException(
                        "Desktop browser is not supported"
                );
            }

            Desktop
                    .getDesktop()
                    .browse(
                            URI.create(
                                    authorizationUrl
                            )
                    );

            // =================================================
            // WAIT FOR GOOGLE CALLBACK
            //
            // Max 5 minutes.
            // =================================================

            String authorizationCode =
                    codeFuture.get(
                            5,
                            TimeUnit.MINUTES
                    );

            // =================================================
            // EXCHANGE CODE FOR TOKEN
            // =================================================

            JsonNode tokenJson =
                    exchangeAuthorizationCode(
                            clientId,
                            redirectUri,
                            authorizationCode,
                            codeVerifier
                    );

            String accessToken =
                    getRequiredText(
                            tokenJson,
                            "access_token"
                    );

            String refreshToken =
                    getOptionalText(
                            tokenJson,
                            "refresh_token"
                    );

            // =================================================
            // GET GOOGLE USER
            // =================================================

            JsonNode userJson =
                    loadGoogleUser(
                            accessToken
                    );

            String googleId =
                    getRequiredText(
                            userJson,
                            "sub"
                    );

            String name =
                    getOptionalText(
                            userJson,
                            "name"
                    );

            String email =
                    getRequiredText(
                            userJson,
                            "email"
                    );

            String picture =
                    getOptionalText(
                            userJson,
                            "picture"
                    );

            return new GoogleUser(
                    googleId,
                    name,
                    email,
                    picture,
                    accessToken,
                    refreshToken
            );

        } finally {

            server.stop(0);
        }
    }

    public JsonNode loginToCarRentalBackend(
            GoogleUser googleUser)
            throws Exception {

        if (googleUser == null) {
            throw new IllegalArgumentException(
                    "Google user is required"
            );
        }

        String accessToken =
                googleUser.getAccessToken();

        if (accessToken == null
                || accessToken.isBlank()) {

            throw new IllegalStateException(
                    "Google access token is missing"
            );
        }

        String requestJson =
                mapper.writeValueAsString(
                        Map.of(
                                "accessToken",
                                accessToken
                        )
                );

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        "http://localhost:8080/api/auth/desktop/login"
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(30)
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(requestJson)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new RuntimeException(
                    "Car Rental backend login failed: "
                    + response.body()
            );
        }

        return mapper.readTree(
                response.body()
        );
    }

    // =========================================================
    // CALLBACK
    // =========================================================

    private void handleCallback(
            HttpExchange exchange,
            String expectedState,
            CompletableFuture<String> codeFuture)
            throws IOException {

        Map<String, String> parameters =
                parseQuery(
                        exchange
                                .getRequestURI()
                                .getRawQuery()
                );

        String error =
                parameters.get(
                        "error"
                );

        if (error != null
                && !error.isBlank()) {

            codeFuture
                    .completeExceptionally(
                            new RuntimeException(
                                    "Google login error: "
                                    + error
                            )
                    );

            sendBrowserResponse(
                    exchange,
                    400,
                    """
                    <html>
                    <body>
                    <h2>Google Login Cancelled</h2>
                    <p>You may return to the Car Rental application.</p>
                    </body>
                    </html>
                    """
            );

            return;
        }

        String returnedState =
                parameters.get(
                        "state"
                );

        if (returnedState == null
                || !returnedState.equals(
                        expectedState
                )) {

            codeFuture
                    .completeExceptionally(
                            new SecurityException(
                                    "Invalid OAuth state"
                            )
                    );

            sendBrowserResponse(
                    exchange,
                    400,
                    """
                    <html>
                    <body>
                    <h2>Invalid Login Request</h2>
                    <p>You may return to the Car Rental application.</p>
                    </body>
                    </html>
                    """
            );

            return;
        }

        String code =
                parameters.get(
                        "code"
                );

        if (code == null
                || code.isBlank()) {

            codeFuture
                    .completeExceptionally(
                            new RuntimeException(
                                    "Google authorization code was not received"
                            )
                    );

            sendBrowserResponse(
                    exchange,
                    400,
                    """
                    <html>
                    <body>
                    <h2>Google Login Failed</h2>
                    <p>Authorization code was not received.</p>
                    </body>
                    </html>
                    """
            );

            return;
        }

        codeFuture.complete(code);

        sendBrowserResponse(
                exchange,
                200,
                """
                <html>
                <head>
                    <title>Car Rental Login</title>
                </head>

                <body style="font-family: Arial; text-align: center; padding-top: 60px;">

                    <h2>Google Login Successful!</h2>

                    <p>
                        You may close this browser tab
                        and return to the Car Rental application.
                    </p>

                </body>
                </html>
                """
        );
    }

    // =========================================================
    // TOKEN EXCHANGE
    // =========================================================

    private JsonNode exchangeAuthorizationCode(
            String clientId,
            String redirectUri,
            String authorizationCode,
            String codeVerifier)
            throws Exception {

        String clientSecret =
                System.getenv(
                        "GOOGLE_DESKTOP_CLIENT_SECRET"
                );

        if (clientSecret == null
                || clientSecret.isBlank()) {

            throw new IllegalStateException(
                    "GOOGLE_DESKTOP_CLIENT_SECRET is not configured"
            );
        }

        String form =
                "client_id="
                + encode(clientId)

                + "&client_secret="
                + encode(clientSecret)

                + "&code="
                + encode(authorizationCode)

                + "&code_verifier="
                + encode(codeVerifier)

                + "&grant_type=authorization_code"

                + "&redirect_uri="
                + encode(redirectUri);

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        TOKEN_ENDPOINT
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(30)
                        )
                        .header(
                                "Content-Type",
                                "application/x-www-form-urlencoded"
                        )
                        .POST(
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(form)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new RuntimeException(
                    "Google token exchange failed: "
                    + response.body()
            );
        }

        return mapper.readTree(
                response.body()
        );
    }

    // =========================================================
    // GOOGLE USERINFO
    // =========================================================

    private JsonNode loadGoogleUser(
            String accessToken)
            throws Exception {

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        USERINFO_ENDPOINT
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(30)
                        )
                        .header(
                                "Authorization",
                                "Bearer "
                                + accessToken
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                );

        if (response.statusCode() != 200) {

            throw new RuntimeException(
                    "Unable to load Google user: "
                    + response.body()
            );
        }

        return mapper.readTree(
                response.body()
        );
    }

    // =========================================================
    // PKCE VERIFIER
    // =========================================================

    private String createCodeVerifier() {

        byte[] bytes =
                new byte[64];

        secureRandom.nextBytes(
                bytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        bytes
                );
    }

    // =========================================================
    // PKCE CHALLENGE
    // =========================================================

    private String createCodeChallenge(
            String verifier)
            throws Exception {

        MessageDigest digest =
                MessageDigest
                        .getInstance(
                                "SHA-256"
                        );

        byte[] hash =
                digest.digest(
                        verifier.getBytes(
                                StandardCharsets.US_ASCII
                        )
                );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        hash
                );
    }

    // =========================================================
    // RANDOM STATE
    // =========================================================

    private String createRandomValue() {

        byte[] bytes =
                new byte[32];

        secureRandom.nextBytes(
                bytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        bytes
                );
    }

    // =========================================================
    // QUERY PARSER
    // =========================================================

    private Map<String, String> parseQuery(
            String rawQuery) {

        Map<String, String> values =
                new HashMap<>();

        if (rawQuery == null
                || rawQuery.isBlank()) {

            return values;
        }

        String[] pairs =
                rawQuery.split("&");

        for (String pair : pairs) {

            String[] parts =
                    pair.split(
                            "=",
                            2
                    );

            String key =
                    decode(
                            parts[0]
                    );

            String value =
                    parts.length > 1
                    ? decode(parts[1])
                    : "";

            values.put(
                    key,
                    value
            );
        }

        return values;
    }

    // =========================================================
    // BROWSER RESPONSE
    // =========================================================

    private void sendBrowserResponse(
            HttpExchange exchange,
            int statusCode,
            String html)
            throws IOException {

        byte[] bytes =
                html.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange
                .getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/html; charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                statusCode,
                bytes.length
        );

        try (
                OutputStream output =
                        exchange.getResponseBody()
        ) {

            output.write(bytes);
        }
    }

    // =========================================================
    // JSON REQUIRED TEXT
    // =========================================================

    private String getRequiredText(
            JsonNode json,
            String fieldName) {

        JsonNode node =
                json.get(
                        fieldName
                );

        if (node == null
                || node.isNull()
                || node.asText().isBlank()) {

            throw new RuntimeException(
                    "Missing Google field: "
                    + fieldName
            );
        }

        return node.asText();
    }

    // =========================================================
    // JSON OPTIONAL TEXT
    // =========================================================

    private String getOptionalText(
            JsonNode json,
            String fieldName) {

        JsonNode node =
                json.get(
                        fieldName
                );

        if (node == null
                || node.isNull()) {

            return null;
        }

        String value =
                node.asText();

        if (value.isBlank()) {
            return null;
        }

        return value;
    }

    // =========================================================
    // URL ENCODE
    // =========================================================

    private String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    // =========================================================
    // URL DECODE
    // =========================================================

    private String decode(
            String value) {

        return URLDecoder.decode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
