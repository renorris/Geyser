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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public record CustomYggdrasilUrls(URI baseUrl, URI authenticateUrl, URI refreshUrl, URI joinUrl, URI hasJoinedUrl,
                                  URI profileUrl) {
    private static final String AUTHLIB_INJECTOR_SUFFIX = "/authlib-injector";

    public static CustomYggdrasilUrls fromBaseUrl(String configuredBaseUrl) {
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            throw new IllegalArgumentException("custom-yggdrasil.base-url must be set when custom Yggdrasil authentication is enabled");
        }

        URI parsed = URI.create(configuredBaseUrl.trim());
        String scheme = parsed.getScheme() == null ? "" : parsed.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("custom-yggdrasil.base-url must use http or https");
        }
        if (parsed.getHost() == null) {
            throw new IllegalArgumentException("custom-yggdrasil.base-url must include a host");
        }
        if (parsed.getQuery() != null || parsed.getFragment() != null) {
            throw new IllegalArgumentException("custom-yggdrasil.base-url must not include a query or fragment");
        }

        String path = trimTrailingSlashes(parsed.getPath());
        if (path.endsWith(AUTHLIB_INJECTOR_SUFFIX)) {
            path = path.substring(0, path.length() - AUTHLIB_INJECTOR_SUFFIX.length());
        } else if (path.contains(AUTHLIB_INJECTOR_SUFFIX + "/")) {
            throw new IllegalArgumentException("custom-yggdrasil.base-url must be the Drasl base URL, not a nested authlib-injector URL");
        }
        URI base = uri(parsed, path);
        return new CustomYggdrasilUrls(
            base,
            join(base, "auth", "authenticate"),
            join(base, "auth", "refresh"),
            join(base, "session", "minecraft", "join"),
            join(base, "session", "minecraft", "hasJoined"),
            join(base, "session", "minecraft", "profile")
        );
    }

    public String normalizedBaseUrl() {
        return baseUrl.toString();
    }

    URI profileUrl(String undashedUuid) {
        return URI.create(join(baseUrl, "session", "minecraft", "profile", undashedUuid) + "?unsigned=false");
    }

    URI hasJoinedUrl(String query) {
        return URI.create(hasJoinedUrl + "?" + query);
    }

    private static URI join(URI base, String... segments) {
        StringBuilder path = new StringBuilder(trimTrailingSlashes(base.getPath()));
        for (String segment : segments) {
            path.append('/').append(segment);
        }
        return uri(base, path.toString());
    }

    private static URI uri(URI source, String path) {
        try {
            String normalizedPath = path == null || path.isEmpty() ? "" : path;
            return new URI(source.getScheme(), source.getRawAuthority(), normalizedPath, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid custom Yggdrasil base URL", e);
        }
    }

    private static String trimTrailingSlashes(String path) {
        if (path == null || path.equals("/")) {
            return "";
        }
        int end = path.length();
        while (end > 0 && path.charAt(end - 1) == '/') {
            end--;
        }
        return path.substring(0, end);
    }
}
