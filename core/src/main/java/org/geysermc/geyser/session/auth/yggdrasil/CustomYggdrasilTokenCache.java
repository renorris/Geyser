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

import com.google.gson.reflect.TypeToken;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomYggdrasilTokenCache {
    private static final Type CACHE_TYPE = new TypeToken<Map<String, Entry>>() { }.getType();

    private final Path file;
    private final String baseUrl;
    private final GeyserLogger logger;
    private final Map<String, Entry> entries;

    private CustomYggdrasilTokenCache(Path file, String baseUrl, GeyserLogger logger, Map<String, Entry> entries) {
        this.file = file;
        this.baseUrl = baseUrl;
        this.logger = logger;
        this.entries = new ConcurrentHashMap<>(entries);
    }

    public static CustomYggdrasilTokenCache load(Path folder, String baseUrl, GeyserLogger logger) {
        Path file = folder.resolve(Constants.SAVED_CUSTOM_YGGDRASIL_TOKENS_FILE);
        Map<String, Entry> loaded = new ConcurrentHashMap<>();

        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Entry> parsed = GeyserImpl.GSON.fromJson(reader, CACHE_TYPE);
                if (parsed != null) {
                    for (Map.Entry<String, Entry> entry : parsed.entrySet()) {
                        String xuid = entry.getKey();
                        Entry value = entry.getValue();
                        if (xuid != null && !xuid.isBlank() && value != null && value.isUsableFor(baseUrl)) {
                            loaded.put(xuid, value);
                        }
                    }
                    if (loaded.size() != parsed.size()) {
                        CustomYggdrasilTokenCache cache = new CustomYggdrasilTokenCache(file, baseUrl, logger, loaded);
                        cache.write();
                        return cache;
                    }
                }
            } catch (IOException | RuntimeException e) {
                logger.warning("Ignoring corrupt custom Yggdrasil token cache: " + e.getMessage());
            }
        }

        return new CustomYggdrasilTokenCache(file, baseUrl, logger, loaded);
    }

    public Entry get(String xuid) {
        if (xuid == null || xuid.isBlank()) {
            return null;
        }
        Entry entry = entries.get(xuid);
        if (entry == null || !entry.isUsableFor(baseUrl)) {
            return null;
        }
        return entry;
    }

    public void put(String xuid, String bedrockUsername, CustomYggdrasilAuthResult result) {
        if (xuid == null || xuid.isBlank()) {
            return;
        }
        Entry entry = Entry.fromResult(baseUrl, bedrockUsername, result);
        if (!Objects.equals(entries.put(xuid, entry), entry)) {
            write();
        }
    }

    public void remove(String xuid) {
        if (xuid == null || xuid.isBlank()) {
            return;
        }
        if (entries.remove(xuid) != null) {
            write();
        }
    }

    public int size() {
        return entries.size();
    }

    private synchronized void write() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GeyserImpl.GSON.toJson(entries, CACHE_TYPE, writer);
            }
        } catch (IOException e) {
            logger.error("Unable to write custom Yggdrasil token cache!", e);
        }
    }

    public record Entry(String baseUrl, String bedrockUsername, String javaUsername, String profileId, String accessToken,
                        String clientToken, String lastRefreshed) {
        static Entry fromResult(String baseUrl, String bedrockUsername, CustomYggdrasilAuthResult result) {
            return new Entry(baseUrl, bedrockUsername, result.selectedProfile().name(), result.selectedProfile().id(),
                result.accessToken(), result.clientToken(), Instant.now().toString());
        }

        boolean isUsableFor(String expectedBaseUrl) {
            return Objects.equals(baseUrl, expectedBaseUrl)
                && bedrockUsername != null && !bedrockUsername.isBlank()
                && javaUsername != null && !javaUsername.isBlank()
                && profileId != null && !profileId.isBlank()
                && accessToken != null && !accessToken.isBlank()
                && clientToken != null && !clientToken.isBlank();
        }

        CustomYggdrasilProfile profile() {
            return new CustomYggdrasilProfile(profileId, javaUsername);
        }
    }
}
