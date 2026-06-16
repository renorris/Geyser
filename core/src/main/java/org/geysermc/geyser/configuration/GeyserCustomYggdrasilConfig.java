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

package org.geysermc.geyser.configuration;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultBoolean;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultString;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public final class GeyserCustomYggdrasilConfig {
    @Comment("Whether to use the custom Yggdrasil username/password flow instead of Microsoft OAuth for online-mode login.")
    @DefaultBoolean(false)
    private boolean enabled;

    @Comment("""
            Base URL of the Yggdrasil-compatible auth server, for example "https://drasl.example.com".
            If an authlib-injector URL ending in "/authlib-injector" is entered by mistake, Geyser will strip that suffix.""")
    @DefaultString("")
    private String baseUrl = "";

    @Comment("Whether Bedrock players are allowed to enter a Java username and password into a Geyser form.")
    @DefaultBoolean(false)
    private boolean allowPasswordAuthentication;

    public boolean enabled() {
        return enabled;
    }

    public @NonNull String baseUrl() {
        return baseUrl == null ? "" : baseUrl;
    }

    public boolean allowPasswordAuthentication() {
        return allowPasswordAuthentication;
    }
}
