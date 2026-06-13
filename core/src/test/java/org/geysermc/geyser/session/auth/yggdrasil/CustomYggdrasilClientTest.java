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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomYggdrasilClientTest {
    private static final String PROFILE_ID = "f84c6a790a4e45c38fcd1b3c4d5e6f70";

    private HttpServer server;
    private CustomYggdrasilClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new CustomYggdrasilClient(CustomYggdrasilUrls.fromBaseUrl(baseUrl));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void authenticateSuccess() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/auth/authenticate", exchange -> {
            requestBody.set(readBody(exchange));
            respond(exchange, 200, """
                {
                  "accessToken": "access-token",
                  "clientToken": "client-token",
                  "selectedProfile": {"id": "%s", "name": "JavaPlayer"}
                }
                """.formatted(PROFILE_ID));
        });

        CustomYggdrasilAuthResult result = client.authenticate("JavaPlayer", "secret-password", "client-token");

        assertEquals("access-token", result.accessToken());
        assertEquals("client-token", result.clientToken());
        assertEquals("JavaPlayer", result.selectedProfile().name());
        String compactRequest = compactJson(requestBody.get());
        assertTrue(compactRequest.contains("\"username\":\"JavaPlayer\""));
        assertTrue(compactRequest.contains("\"password\":\"secret-password\""));
        assertTrue(compactRequest.contains("\"agent\""));
    }

    @Test
    void authenticateBadCredentials() {
        server.createContext("/auth/authenticate", exchange -> respond(exchange, 403, """
            {"error":"ForbiddenOperationException","errorMessage":"Invalid credentials. Invalid username or password."}
            """));

        IOException thrown = assertThrows(IOException.class, () -> client.authenticate("JavaPlayer", "bad-password", "client-token"));
        assertInstanceOf(CustomYggdrasilAuthenticationException.class, thrown);
    }

    @Test
    void authenticateMalformedResponse() {
        server.createContext("/auth/authenticate", exchange -> respond(exchange, 200, "not-json"));

        IOException thrown = assertThrows(IOException.class, () -> client.authenticate("JavaPlayer", "secret-password", "client-token"));
        assertInstanceOf(MalformedYggdrasilResponseException.class, thrown);
    }

    @Test
    void authenticateMissingSelectedProfile() {
        server.createContext("/auth/authenticate", exchange -> respond(exchange, 200, """
            {"accessToken":"access-token","clientToken":"client-token"}
            """));

        IOException thrown = assertThrows(IOException.class, () -> client.authenticate("JavaPlayer", "secret-password", "client-token"));
        assertInstanceOf(MalformedYggdrasilResponseException.class, thrown);
    }

    @Test
    void refreshSuccess() throws Exception {
        server.createContext("/auth/refresh", exchange -> respond(exchange, 200, """
            {
              "accessToken": "new-access-token",
              "clientToken": "client-token",
              "selectedProfile": {"id": "%s", "name": "JavaPlayer"}
            }
            """.formatted(PROFILE_ID)));

        CustomYggdrasilAuthResult result = client.refresh("old-access-token", "client-token",
            new CustomYggdrasilProfile(PROFILE_ID, "JavaPlayer"));

        assertEquals("new-access-token", result.accessToken());
        assertEquals("client-token", result.clientToken());
    }

    @Test
    void refreshFailure() {
        server.createContext("/auth/refresh", exchange -> respond(exchange, 403, """
            {"error":"ForbiddenOperationException","errorMessage":"Invalid token"}
            """));

        IOException thrown = assertThrows(IOException.class, () -> client.refresh("bad-token", "client-token",
            new CustomYggdrasilProfile(PROFILE_ID, "JavaPlayer")));
        assertInstanceOf(CustomYggdrasilAuthenticationException.class, thrown);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String compactJson(String json) {
        return json.replace(" ", "").replace("\n", "").replace("\r", "");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
