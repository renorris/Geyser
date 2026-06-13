/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.session.auth.yggdrasil;

import com.google.gson.JsonParseException;
import org.geysermc.geyser.GeyserImpl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CustomYggdrasilClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Agent MINECRAFT_AGENT = new Agent("Minecraft", 1);

    private final CustomYggdrasilUrls urls;
    private final HttpClient httpClient;

    public CustomYggdrasilClient(CustomYggdrasilUrls urls) {
        this(urls, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
    }

    CustomYggdrasilClient(CustomYggdrasilUrls urls, HttpClient httpClient) {
        this.urls = urls;
        this.httpClient = httpClient;
    }

    public CustomYggdrasilAuthResult authenticate(String username, String password, String clientToken) throws IOException {
        AuthenticateRequest request = new AuthenticateRequest(username, password, clientToken, MINECRAFT_AGENT, false);
        AuthenticateResponse response = sendJson(urls.authenticateUrl(), request, AuthenticateResponse.class, "authenticate");
        return validate(response.accessToken, response.clientToken, response.selectedProfile, "authenticate");
    }

    public CustomYggdrasilAuthResult refresh(String accessToken, String clientToken, CustomYggdrasilProfile selectedProfile) throws IOException {
        RefreshRequest request = new RefreshRequest(accessToken, clientToken, false, selectedProfile);
        RefreshResponse response = sendJson(urls.refreshUrl(), request, RefreshResponse.class, "refresh");
        return validate(response.accessToken, response.clientToken, response.selectedProfile, "refresh");
    }

    private <T> T sendJson(URI uri, Object requestBody, Class<T> responseType, String operation) throws IOException {
        String json = GeyserImpl.GSON.toJson(requestBody);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling custom Yggdrasil " + operation + " endpoint", e);
        }

        int status = response.statusCode();
        if (status == 403) {
            String message = yggdrasilErrorMessage(response.body());
            throw new CustomYggdrasilAuthenticationException(message == null ? "Invalid custom Yggdrasil credentials or token" : message);
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Custom Yggdrasil " + operation + " endpoint returned HTTP " + status);
        }

        try {
            T parsed = GeyserImpl.GSON.fromJson(response.body(), responseType);
            if (parsed == null) {
                throw new MalformedYggdrasilResponseException("Custom Yggdrasil " + operation + " response was empty");
            }
            return parsed;
        } catch (JsonParseException e) {
            throw new MalformedYggdrasilResponseException("Custom Yggdrasil " + operation + " response was not valid JSON", e);
        }
    }

    private static CustomYggdrasilAuthResult validate(String accessToken, String clientToken, CustomYggdrasilProfile selectedProfile,
                                                      String operation) throws MalformedYggdrasilResponseException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new MalformedYggdrasilResponseException("Custom Yggdrasil " + operation + " response did not include an access token");
        }
        if (clientToken == null || clientToken.isBlank()) {
            throw new MalformedYggdrasilResponseException("Custom Yggdrasil " + operation + " response did not include a client token");
        }
        if (selectedProfile == null || selectedProfile.id() == null || selectedProfile.id().isBlank()
            || selectedProfile.name() == null || selectedProfile.name().isBlank()) {
            throw new MalformedYggdrasilResponseException("Custom Yggdrasil " + operation + " response did not include a selected profile");
        }
        try {
            selectedProfile.uuid();
        } catch (IllegalArgumentException e) {
            throw new MalformedYggdrasilResponseException("Custom Yggdrasil " + operation + " response included an invalid selected profile UUID", e);
        }
        return new CustomYggdrasilAuthResult(accessToken, clientToken, selectedProfile);
    }

    private static String yggdrasilErrorMessage(String responseBody) {
        try {
            YggdrasilError error = GeyserImpl.GSON.fromJson(responseBody, YggdrasilError.class);
            if (error != null && error.errorMessage != null && !error.errorMessage.isBlank()) {
                return error.errorMessage;
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private record Agent(String name, int version) {
    }

    private record AuthenticateRequest(String username, String password, String clientToken, Agent agent, boolean requestUser) {
    }

    private record RefreshRequest(String accessToken, String clientToken, boolean requestUser, CustomYggdrasilProfile selectedProfile) {
    }

    private static final class AuthenticateResponse {
        private String accessToken;
        private String clientToken;
        private CustomYggdrasilProfile selectedProfile;
    }

    private static final class RefreshResponse {
        private String accessToken;
        private String clientToken;
        private CustomYggdrasilProfile selectedProfile;
    }

    private static final class YggdrasilError {
        private String errorMessage;
    }
}
