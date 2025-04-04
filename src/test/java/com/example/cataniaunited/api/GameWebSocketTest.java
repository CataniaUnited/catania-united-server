package com.example.cataniaunited.api;

import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.OpenConnections;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class GameWebSocketTest {

    @TestHTTPResource
    URI SERVER_URI;

    @Inject
    OpenConnections connections;

    @Test
    void testWebSocketOnOpen() throws InterruptedException {
        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);
        var openConnections = connections.listAll().size();
        BasicWebSocketConnector.create()
                .baseUri(SERVER_URI)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    receivedMessages.add(message);
                    messageLatch.countDown();
                })
                .connectAndAwait();

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals("Connection successful", receivedMessages.getFirst());
        assertEquals(openConnections + 1, connections.listAll().size());
    }

    @Test
    void testWebSocketSendMessage() throws InterruptedException, JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var messageDto = new MessageDTO();
        messageDto.setType(MessageType.CREATE_LOBBY); // Send CREATE_LOBBY request
        messageDto.setPlayer("Player 1");
        messageDto.setLobbyId("1");

        List<String> receivedMessages = new ArrayList<>();
        // Expecting 2 messages
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(SERVER_URI)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    receivedMessages.add(message);
                    messageLatch.countDown();  // Decrease latch count when a message arrives
                })
                .connectAndAwait();

        // Send message
        String sentMessage = objectMapper.writeValueAsString(messageDto);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(2, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);

        assertEquals(MessageType.LOBBY_CREATED, responseMessage.getType()); // Expect LOBBY_CREATED response
        assertEquals("Player 1", responseMessage.getPlayer()); // Player should remain the same
        assertNotNull(responseMessage.getLobbyId());
    }

    @Test
    void testWebSocketOnClose() throws InterruptedException {
        List<String> receivedMessages = new ArrayList<>();
        // Expecting 2 messages
        CountDownLatch messageLatch = new CountDownLatch(2);

        var clientToClose = BasicWebSocketConnector.create()
                .baseUri(SERVER_URI)
                .path("/game")
                .connectAndAwait();

        BasicWebSocketConnector.create()
                .baseUri(SERVER_URI)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    receivedMessages.add(message);
                    messageLatch.countDown();  // Decrease latch count when a message arrives
                })
                .connectAndAwait();

        assertEquals(2, connections.listAll().size());

        // Send message
        clientToClose.close().await().atMost(Duration.of(5, ChronoUnit.SECONDS));

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        //Check if client disconnected
        assertEquals(1, connections.listAll().size());
        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(2, receivedMessages.size());
        assertEquals("Client disconnected", receivedMessages.getLast());
    }

    @Test
    void testUnknownCommand() throws InterruptedException, JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var unknownMessageDto = new MessageDTO();
        unknownMessageDto.setPlayer("Player 1");
        // Set a non-null type so the switch statement can work. This will cause the default branch.
        unknownMessageDto.setType(MessageType.ERROR);

        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(SERVER_URI)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        receivedMessages.add(message);
                        messageLatch.countDown();
                    }
                })
                .connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(unknownMessageDto);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(1, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);

        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertEquals("Server", responseMessage.getPlayer());
        assertEquals("Unknown command", responseMessage.getLobbyId());
    }

    @Test
    void testSetUsernameCode() throws InterruptedException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        CountDownLatch latch = new CountDownLatch(1);
        List<String> receivedMessages = new ArrayList<>();

        var client = BasicWebSocketConnector.create()
                .baseUri(SERVER_URI)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        receivedMessages.add(message);
                        latch.countDown();
                    }
                })
                .connectAndAwait();

        MessageDTO setUsernameMsg = new MessageDTO();
        setUsernameMsg.setType(MessageType.SET_USERNAME);
        setUsernameMsg.setPlayer("Chicken");
        client.sendTextAndAwait(objectMapper.writeValueAsString(setUsernameMsg));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive LOBBY_UPDATED in time");
        assertEquals(1, receivedMessages.size());

        MessageDTO received = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);
        assertEquals(MessageType.LOBBY_UPDATED, received.getType());
        assertEquals("Chicken", received.getPlayer());
        assertNotNull(received.getPlayers());
        assertTrue(received.getPlayers().contains("Chicken"));
    }
}
