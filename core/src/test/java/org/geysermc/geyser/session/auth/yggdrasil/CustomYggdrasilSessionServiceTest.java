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
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomYggdrasilSessionServiceTest {
    @Test
    void joinServerUsesConfiguredSessionEndpoint() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/session/minecraft/join", exchange -> {
            requestBody.set(readBody(exchange));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            CustomYggdrasilSessionService service = new CustomYggdrasilSessionService(CustomYggdrasilUrls.fromBaseUrl(baseUrl));
            UUID profileId = UUID.fromString("f84c6a79-0a4e-45c3-8fcd-1b3c4d5e6f70");

            service.joinServer(new GameProfile(profileId, "JavaPlayer"), "access-token", "server-id");

            assertNotNull(requestBody.get());
            String compactRequest = requestBody.get().replace(" ", "").replace("\n", "").replace("\r", "");
            assertTrue(compactRequest.contains("\"accessToken\":\"access-token\""));
            assertTrue(compactRequest.contains("\"selectedProfile\":\"f84c6a790a4e45c38fcd1b3c4d5e6f70\""));
            assertTrue(compactRequest.contains("\"serverId\":\"server-id\""));
        } finally {
            server.stop(0);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
