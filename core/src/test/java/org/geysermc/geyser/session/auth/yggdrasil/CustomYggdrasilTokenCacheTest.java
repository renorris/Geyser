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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomYggdrasilTokenCacheTest {
    private static final String BASE_URL = "https://drasl.example.com";
    private static final String PROFILE_ID = "f84c6a790a4e45c38fcd1b3c4d5e6f70";

    @TempDir
    Path tempDirectory;

    @Test
    void loadsValidCache() throws Exception {
        Path file = cacheFile();
        Files.writeString(file, """
            {
              "123": {
                "baseUrl": "https://drasl.example.com",
                "bedrockUsername": "BedrockUser",
                "javaUsername": "JavaPlayer",
                "profileId": "%s",
                "accessToken": "access-token",
                "clientToken": "client-token",
                "lastRefreshed": "2026-06-13T00:00:00Z"
              }
            }
            """.formatted(PROFILE_ID));

        CustomYggdrasilTokenCache cache = CustomYggdrasilTokenCache.load(tempDirectory, BASE_URL, new TestLogger());

        assertNotNull(cache.get("123"));
        assertEquals(1, cache.size());
    }

    @Test
    void ignoresCorruptCache() throws Exception {
        Files.writeString(cacheFile(), "not-json");
        TestLogger logger = new TestLogger();

        CustomYggdrasilTokenCache cache = CustomYggdrasilTokenCache.load(tempDirectory, BASE_URL, logger);

        assertEquals(0, cache.size());
        assertTrue(logger.messages.stream().anyMatch(message -> message.contains("Ignoring corrupt custom Yggdrasil token cache")));
    }

    @Test
    void purgesEntriesForDifferentCustomYggdrasilServer() throws Exception {
        Files.writeString(cacheFile(), """
            {
              "allowed-xuid": {
                "baseUrl": "https://drasl.example.com",
                "bedrockUsername": "AllowedBedrock",
                "javaUsername": "JavaPlayer",
                "profileId": "%s",
                "accessToken": "allowed-token",
                "clientToken": "allowed-client",
                "lastRefreshed": "2026-06-13T00:00:00Z"
              },
              "wrong-server-xuid": {
                "baseUrl": "https://other.example.com",
                "bedrockUsername": "OtherBedrock",
                "javaUsername": "RemovedJava",
                "profileId": "%s",
                "accessToken": "removed-token",
                "clientToken": "removed-client",
                "lastRefreshed": "2026-06-13T00:00:00Z"
              }
            }
            """.formatted(PROFILE_ID, PROFILE_ID));

        CustomYggdrasilTokenCache cache = CustomYggdrasilTokenCache.load(tempDirectory, BASE_URL, new TestLogger());

        assertNotNull(cache.get("allowed-xuid"));
        assertNull(cache.get("wrong-server-xuid"));
        String rewritten = Files.readString(cacheFile());
        assertFalse(rewritten.contains("OtherBedrock"));
        assertFalse(rewritten.contains("removed-token"));
    }

    @Test
    void savesLoginWithoutSavedUserLoginsAllowlist() {
        CustomYggdrasilTokenCache cache = CustomYggdrasilTokenCache.load(tempDirectory, BASE_URL, new TestLogger());
        CustomYggdrasilAuthentication auth = new CustomYggdrasilAuthentication(CustomYggdrasilUrls.fromBaseUrl(BASE_URL),
            new CustomYggdrasilClient(CustomYggdrasilUrls.fromBaseUrl(BASE_URL)), cache, true);

        auth.save("123", "BedrockUser", new CustomYggdrasilAuthResult("access-token", "client-token",
            new CustomYggdrasilProfile(PROFILE_ID, "JavaPlayer")));

        assertNotNull(auth.cachedLogin("123"));
    }

    @Test
    void doesNotSerializePasswords() throws Exception {
        String password = "this-password-must-not-be-written";
        CustomYggdrasilTokenCache cache = CustomYggdrasilTokenCache.load(tempDirectory, BASE_URL, new TestLogger());

        cache.put("123", "BedrockUser", new CustomYggdrasilAuthResult("access-token", "client-token",
            new CustomYggdrasilProfile(PROFILE_ID, "JavaPlayer")));

        String serialized = Files.readString(cacheFile());
        assertFalse(serialized.contains(password));
        assertFalse(serialized.toLowerCase().contains("password"));
    }

    @Test
    void refreshesCachedTokenAndWritesNewToken() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/auth/refresh", exchange -> respond(exchange, 200, """
            {
              "accessToken": "new-access-token",
              "clientToken": "client-token",
              "selectedProfile": {"id": "%s", "name": "JavaPlayer"}
            }
            """.formatted(PROFILE_ID)));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            CustomYggdrasilUrls urls = CustomYggdrasilUrls.fromBaseUrl(baseUrl);
            CustomYggdrasilTokenCache cache = CustomYggdrasilTokenCache.load(tempDirectory, urls.normalizedBaseUrl(),
                new TestLogger());
            cache.put("123", "BedrockUser", new CustomYggdrasilAuthResult("old-access-token", "client-token",
                new CustomYggdrasilProfile(PROFILE_ID, "JavaPlayer")));
            CustomYggdrasilAuthentication auth = new CustomYggdrasilAuthentication(urls, new CustomYggdrasilClient(urls), cache,
                true);

            CustomYggdrasilTokenCache.Entry entry = auth.cachedLogin("123");
            assertNotNull(entry);
            CustomYggdrasilAuthResult refreshed = auth.refresh(entry);
            auth.save("123", "BedrockUser", refreshed);

            String serialized = Files.readString(cacheFile());
            assertTrue(serialized.contains("new-access-token"));
            assertFalse(serialized.contains("old-access-token"));
        } finally {
            server.stop(0);
        }
    }

    private Path cacheFile() {
        return tempDirectory.resolve(Constants.SAVED_CUSTOM_YGGDRASIL_TOKENS_FILE);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static final class TestLogger implements GeyserLogger {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void severe(String message) {
            messages.add(message);
        }

        @Override
        public void severe(String message, Throwable error) {
            messages.add(message);
        }

        @Override
        public void error(String message) {
            messages.add(message);
        }

        @Override
        public void error(String message, Throwable error) {
            messages.add(message);
        }

        @Override
        public void warning(String message) {
            messages.add(message);
        }

        @Override
        public void info(String message) {
            messages.add(message);
        }

        @Override
        public void info(Component message) {
            messages.add(message.toString());
        }

        @Override
        public void debug(String message) {
            messages.add(message);
        }

        @Override
        public void debug(String message, Object... arguments) {
            messages.add(message);
        }

        @Override
        public void setDebug(boolean debug) {
        }

        @Override
        public boolean isDebug() {
            return true;
        }

        @Override
        public void sendMessage(@NonNull String message) {
            messages.add(message);
        }

        @Override
        public @Nullable UUID playerUuid() {
            return null;
        }
    }
}
