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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomYggdrasilUrlsTest {
    @Test
    void normalizesBaseUrlWithoutTrailingSlash() {
        CustomYggdrasilUrls urls = CustomYggdrasilUrls.fromBaseUrl("https://drasl.example.com");
        assertEquals("https://drasl.example.com", urls.normalizedBaseUrl());
        assertEquals("https://drasl.example.com/auth/authenticate", urls.authenticateUrl().toString());
        assertEquals("https://drasl.example.com/session/minecraft/join", urls.joinUrl().toString());
    }

    @Test
    void normalizesBaseUrlWithTrailingSlash() {
        CustomYggdrasilUrls urls = CustomYggdrasilUrls.fromBaseUrl("https://drasl.example.com/");
        assertEquals("https://drasl.example.com", urls.normalizedBaseUrl());
        assertEquals("https://drasl.example.com/auth/refresh", urls.refreshUrl().toString());
    }

    @Test
    void stripsAccidentalAuthlibInjectorSuffix() {
        CustomYggdrasilUrls urls = CustomYggdrasilUrls.fromBaseUrl("https://drasl.example.com/authlib-injector/");
        assertEquals("https://drasl.example.com", urls.normalizedBaseUrl());
        assertEquals("https://drasl.example.com/session/minecraft/profile/abc?unsigned=false", urls.profileUrl("abc").toString());
    }

    @Test
    void preservesDeploymentSubPath() {
        CustomYggdrasilUrls urls = CustomYggdrasilUrls.fromBaseUrl("https://example.com/drasl/authlib-injector");
        assertEquals("https://example.com/drasl", urls.normalizedBaseUrl());
        assertEquals("https://example.com/drasl/auth/authenticate", urls.authenticateUrl().toString());
    }

    @Test
    void rejectsInvalidBaseUrls() {
        assertThrows(IllegalArgumentException.class, () -> CustomYggdrasilUrls.fromBaseUrl(""));
        assertThrows(IllegalArgumentException.class, () -> CustomYggdrasilUrls.fromBaseUrl("ftp://drasl.example.com"));
        assertThrows(IllegalArgumentException.class, () -> CustomYggdrasilUrls.fromBaseUrl("https://drasl.example.com?bad=true"));
        assertThrows(IllegalArgumentException.class, () -> CustomYggdrasilUrls.fromBaseUrl("https://drasl.example.com/authlib-injector/nested"));
    }
}
