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
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.SessionService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public final class CustomYggdrasilSessionService extends SessionService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final CustomYggdrasilUrls urls;
    private final HttpClient httpClient;

    public CustomYggdrasilSessionService(CustomYggdrasilUrls urls) {
        this.urls = urls;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    @Override
    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws IOException {
        JoinServerRequest request = new JoinServerRequest(authenticationToken, CustomYggdrasilUuid.withoutDashes(profile.getId()), serverId);
        sendJson(urls.joinUrl(), request, 204);
    }

    @Override
    public GameProfile getProfileByServer(String name, String serverId) throws IOException {
        String query = "username=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
            + "&serverId=" + URLEncoder.encode(serverId, StandardCharsets.UTF_8);
        HttpResponse<String> response = sendGet(urls.hasJoinedUrl(query));
        if (response.statusCode() == 403 || response.statusCode() == 204) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IOException("Custom Yggdrasil hasJoined endpoint returned HTTP " + response.statusCode());
        }
        try {
            ProfileResponse parsed = GeyserImpl.GSON.fromJson(response.body(), ProfileResponse.class);
            if (parsed == null || parsed.id == null) {
                return null;
            }
            GameProfile result = new GameProfile(CustomYggdrasilUuid.fromString(parsed.id), parsed.name == null ? name : parsed.name);
            result.setProperties(parsed.properties);
            return result;
        } catch (JsonParseException e) {
            throw new IOException("Custom Yggdrasil hasJoined response was not valid JSON", e);
        }
    }

    @Override
    public void fillProfileProperties(GameProfile profile) throws IOException {
        if (profile.getId() == null) {
            return;
        }

        HttpResponse<String> response = sendGet(urls.profileUrl(CustomYggdrasilUuid.withoutDashes(profile.getId())));
        if (response.statusCode() != 200) {
            throw new IOException("Custom Yggdrasil profile endpoint returned HTTP " + response.statusCode());
        }
        try {
            ProfileResponse parsed = GeyserImpl.GSON.fromJson(response.body(), ProfileResponse.class);
            if (parsed == null || parsed.id == null) {
                throw new IOException("Custom Yggdrasil profile response was empty");
            }
            profile.setProperties(parsed.properties);
        } catch (JsonParseException e) {
            throw new IOException("Custom Yggdrasil profile response was not valid JSON", e);
        }
    }

    private void sendJson(URI uri, Object body, int expectedNoContentStatus) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(GeyserImpl.GSON.toJson(body)))
            .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != expectedNoContentStatus) {
            throw new IOException("Custom Yggdrasil join endpoint returned HTTP " + response.statusCode());
        }
    }

    private HttpResponse<String> sendGet(URI uri) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build();
        return send(request);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling custom Yggdrasil session endpoint", e);
        }
    }

    private record JoinServerRequest(String accessToken, String selectedProfile, String serverId) {
    }

    private static final class ProfileResponse {
        private String id;
        private String name;
        private List properties;
    }
}
