package com.example.cataniaunited.api;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class GameWebSocketUnitTest {

    @TestHTTPResource
    URI SERVER_URI;

    @Test
    void testWebSocketOnOpen() {
        BasicWebSocketConnector.create()
                .baseUri(SERVER_URI)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    assertEquals(connection.id(), message);
                })
                .connectAndAwait();
    }

    @Test
    void testWebSocketSendMessage() throws InterruptedException {
        String sentMessage = "Hello World!";
        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);  // Expecting 2 messages

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(SERVER_URI)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    receivedMessages.add(message);
                    messageLatch.countDown();  // Decrease latch count when a message arrives
                })
                .connectAndAwait();

        // Send message
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        // Ensure we received both messages
        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(2, receivedMessages.size());
        assertEquals(sentMessage, receivedMessages.getLast());
    }
}
