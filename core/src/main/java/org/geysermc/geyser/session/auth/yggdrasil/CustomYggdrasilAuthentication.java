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

import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.configuration.GeyserCustomYggdrasilConfig;
import org.geysermc.geyser.configuration.GeyserConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class CustomYggdrasilAuthentication {
    private final CustomYggdrasilUrls urls;
    private final CustomYggdrasilClient client;
    private final CustomYggdrasilTokenCache cache;
    private final CustomYggdrasilSessionService sessionService;
    private final boolean cacheTokens;
    private final List<String> savedUserLogins;

    CustomYggdrasilAuthentication(CustomYggdrasilUrls urls, CustomYggdrasilClient client, CustomYggdrasilTokenCache cache,
                                  boolean cacheTokens, List<String> savedUserLogins) {
        this.urls = urls;
        this.client = client;
        this.cache = cache;
        this.sessionService = new CustomYggdrasilSessionService(urls);
        this.cacheTokens = cacheTokens;
        this.savedUserLogins = savedUserLogins;
    }

    public static CustomYggdrasilAuthentication create(GeyserConfig config, Path savedUserLoginsFolder, GeyserLogger logger) {
        GeyserCustomYggdrasilConfig customConfig = config.java().customYggdrasil();
        if (config.java().authType() != AuthType.ONLINE || !customConfig.enabled()) {
            return null;
        }
        if (!customConfig.allowPasswordAuthentication()) {
            throw new IllegalStateException("java.custom-yggdrasil is enabled, but allow-password-authentication is false");
        }

        CustomYggdrasilUrls urls = CustomYggdrasilUrls.fromBaseUrl(customConfig.baseUrl());
        CustomYggdrasilTokenCache cache = CustomYggdrasilTokenCache.load(savedUserLoginsFolder, urls.normalizedBaseUrl(),
            config.savedUserLogins(), logger);
        logger.info("Custom Yggdrasil authentication enabled for " + urls.normalizedBaseUrl());
        return new CustomYggdrasilAuthentication(urls, new CustomYggdrasilClient(urls), cache, customConfig.cacheTokens(),
            List.copyOf(config.savedUserLogins()));
    }

    public CustomYggdrasilAuthResult authenticate(String username, String password) throws IOException {
        return client.authenticate(username, password, UUID.randomUUID().toString());
    }

    public CustomYggdrasilAuthResult refresh(CustomYggdrasilTokenCache.Entry entry) throws IOException {
        return client.refresh(entry.accessToken(), entry.clientToken(), entry.profile());
    }

    public CustomYggdrasilTokenCache.Entry cachedLogin(String xuid, String bedrockUsername) {
        if (!canCache(bedrockUsername)) {
            cache.remove(xuid);
            return null;
        }
        return cache.get(xuid);
    }

    public void save(String xuid, String bedrockUsername, CustomYggdrasilAuthResult result) {
        if (canCache(bedrockUsername)) {
            cache.put(xuid, bedrockUsername, result);
        } else {
            cache.remove(xuid);
        }
    }

    public void remove(String xuid) {
        cache.remove(xuid);
    }

    public boolean canCache(String bedrockUsername) {
        return cacheTokens && savedUserLogins.contains(bedrockUsername);
    }

    public CustomYggdrasilSessionService sessionService() {
        return sessionService;
    }

    public CustomYggdrasilUrls urls() {
        return urls;
    }
}
