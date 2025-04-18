package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class GameWebSocketTest {

    @TestHTTPResource
    URI serverUri;

    @Inject
    OpenConnections connections;

    @InjectSpy
    PlayerService playerService;

    @InjectSpy
    LobbyService lobbyService;

    @InjectSpy
    GameService gameService;

    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testWebSocketOnOpen() throws InterruptedException, JsonProcessingException {
        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);
        var openConnections = connections.listAll().size();
        BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    receivedMessages.add(message);
                    messageLatch.countDown();
                })
                .connectAndAwait();

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        assertEquals(openConnections + 1, connections.listAll().size());
        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);

        assertEquals(MessageType.CONNECTION_SUCCESSFUL, responseMessage.getType());
        assertNotNull(responseMessage.getMessageNode("playerId").textValue());
        verify(playerService).addPlayer(any());
    }

    @Test
    void testWebSocketSendMessage() throws InterruptedException, JsonProcessingException {
        var messageDto = new MessageDTO();
        messageDto.setType(MessageType.CREATE_LOBBY); // Send CREATE_LOBBY request
        messageDto.setPlayer("Player 1");
        messageDto.setLobbyId("1");

        List<String> receivedMessages = new ArrayList<>();
        // Expecting 2 messages
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
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
    void testWebSocketOnClose() throws InterruptedException, JsonProcessingException {
        List<String> receivedMessages = new ArrayList<>();
        // Expecting 2 messages
        CountDownLatch messageLatch = new CountDownLatch(2);

        var clientToClose = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .connectAndAwait();

        BasicWebSocketConnector.create()
                .baseUri(serverUri)
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

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);
        assertEquals(MessageType.CLIENT_DISCONNECTED, responseMessage.getType()); // Expect LOBBY_CREATED response
        assertNotNull(responseMessage.getMessageNode("playerId").textValue());
        verify(playerService).removePlayer(any());
    }

    @Test
    void testInvalidCommand() throws InterruptedException, JsonProcessingException {
        var unknownMessageDto = new MessageDTO();
        unknownMessageDto.setPlayer("Player 1");
        unknownMessageDto.setType(MessageType.ERROR);

        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
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
        assertEquals(2, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);

        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertEquals("Invalid client command", responseMessage.getMessageNode("error").textValue());
    }

    @Test
    void testInvalidClientMessage() throws InterruptedException, JsonProcessingException {
        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        receivedMessages.add(message);
                        messageLatch.countDown();
                    }
                })
                .connectAndAwait();

        String sentMessage = objectMapper
                .createObjectNode()
                .put("type", "INVALID")
                .toString();
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(2, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);

        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertEquals("Unexpected error", responseMessage.getMessageNode("error").textValue());
    }

    @Test
    void testSetUsernameCode() throws InterruptedException, JsonProcessingException {
        //Receiving two messages, since change is broadcast as well as returned directly
        CountDownLatch latch = new CountDownLatch(2);
        List<String> receivedMessages = new ArrayList<>();

        var client = BasicWebSocketConnector.create()
                .baseUri(serverUri)
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
        client.sendTextAndAwait(setUsernameMsg);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive LOBBY_UPDATED in time");
        assertEquals(2, receivedMessages.size());

        MessageDTO received = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);
        assertEquals(MessageType.LOBBY_UPDATED, received.getType());
        assertEquals("Chicken", received.getPlayer());
        assertNotNull(received.getPlayers());
        assertTrue(received.getPlayers().contains("Chicken"));
    }

    @Test
    void testSetUsernameOfNonExistingPlayer() throws JsonProcessingException, InterruptedException {
        doReturn(null).when(playerService).getPlayerByConnection(any(WebSocketConnection.class));
        CountDownLatch latch = new CountDownLatch(2);
        List<String> receivedMessages = new ArrayList<>();

        var client = BasicWebSocketConnector.create()
                .baseUri(serverUri)
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
        client.sendTextAndAwait(setUsernameMsg);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive all messages");

        MessageDTO received = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);
        assertEquals(MessageType.ERROR, received.getType());
        assertEquals("No player session", received.getMessageNode("error").textValue());
    }

    @Test
    void testJoinLobbySuccess() throws InterruptedException, JsonProcessingException {
        MessageDTO joinLobbyMessage = new MessageDTO(MessageType.JOIN_LOBBY, "Player 1", "abc123");

        doReturn(true).when(lobbyService).joinLobbyByCode("abc123", "Player 1");
        CountDownLatch latch = new CountDownLatch(1);
        BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        latch.countDown();
                    }
                })
                .connectAndAwait()
                .sendTextAndAwait(objectMapper.writeValueAsString(joinLobbyMessage));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive PLAYER_JOINED message in time");
    }

    @Test
    void testJoinLobbyFailure() throws InterruptedException, JsonProcessingException {
        MessageDTO joinLobbyMessage = new MessageDTO(MessageType.JOIN_LOBBY, "Player 1", "invalidLobbyId");

        doReturn(false).when(lobbyService).joinLobbyByCode("invalidLobbyId", "Player 1");
        CountDownLatch latch = new CountDownLatch(1);
        BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        latch.countDown();
                    }
                })
                .connectAndAwait()
                .sendTextAndAwait(objectMapper.writeValueAsString(joinLobbyMessage));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive ERROR message in time");

    }

    @Test
    void testPlacementOfSettlement() throws GameException, JsonProcessingException, InterruptedException {
        //Setup Players, Lobby and Gameboard
        String player1 = "Player1";
        String player2 = "Player2";
        String lobbyId = lobbyService.createLobby(player1);
        lobbyService.joinLobbyByCode(lobbyId, player2);
        GameBoard gameBoard = gameService.createGameboard(lobbyId);

        assertNull(gameBoard.getSettlementPositionGraph().getFirst().getBuildingOwner());
        //Create message DTO
        int positionId = gameBoard.getSettlementPositionGraph().getFirst().getId();
        ObjectNode placeSettlementMessageNode = objectMapper
                .createObjectNode()
                .put("settlementPositionId", positionId);

        var placeSettlementMessageDTO = new MessageDTO(MessageType.PLACE_SETTLEMENT, player2, lobbyId, placeSettlementMessageNode);

        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(3);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        receivedMessages.add(message);
                        messageLatch.countDown();
                    }
                })
                .connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(placeSettlementMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);
        assertEquals(MessageType.PLACE_SETTLEMENT, responseMessage.getType());
        assertEquals(player2, responseMessage.getPlayer());
        assertEquals(lobbyId, responseMessage.getLobbyId());

        var actualSettlementPosition = gameService.getGameboardByLobbyId(lobbyId).getSettlementPositionGraph().getFirst();
        assertEquals(player2, actualSettlementPosition.getBuildingOwner());
        verify(gameService).placeSettlement(lobbyId, player2, actualSettlementPosition.getId());
    }

    @ParameterizedTest
    @MethodSource("invalidPlaceSettlementMessageNodes")
    void placementOfSettlementShouldFailForInvalidMessageNode(ObjectNode placeSettlementMessageNode) throws GameException, JsonProcessingException, InterruptedException {
        //Setup Players, Lobby and Gameboard
        String player1 = "Player1";
        String player2 = "Player2";
        String lobbyId = lobbyService.createLobby(player1);
        lobbyService.joinLobbyByCode(lobbyId, player2);
        gameService.createGameboard(lobbyId);

        //Create message DTO
        var placeSettlementMessageDTO = new MessageDTO(MessageType.PLACE_SETTLEMENT, player2, lobbyId, placeSettlementMessageNode);

        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        receivedMessages.add(message);
                        messageLatch.countDown();
                    }
                })
                .connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(placeSettlementMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);
        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertEquals("Invalid settlement position id: id = %s".formatted(placeSettlementMessageDTO.getMessageNode("settlementPositionId").toString()), responseMessage.getMessageNode("error").textValue());

        verify(gameService, never()).placeSettlement(anyString(), anyString(), anyInt());
    }

    @Test
    void testPlacementOfRoad() throws GameException, JsonProcessingException, InterruptedException {
        //Setup Players, Lobby and Gameboard
        String player1 = "Player1";
        String player2 = "Player2";
        String lobbyId = lobbyService.createLobby(player1);
        lobbyService.joinLobbyByCode(lobbyId, player2);
        GameBoard gameBoard = gameService.createGameboard(lobbyId);

        assertNull(gameBoard.getRoadList().getFirst().getOwnerPlayerId());
        //Create message DTO
        int positionId = gameBoard.getRoadList().getFirst().getId();
        ObjectNode placeRoadMessageNode = objectMapper
                .createObjectNode()
                .put("roadId", positionId);

        var placeRoadMessageDTO = new MessageDTO(MessageType.PLACE_ROAD, player2, lobbyId, placeRoadMessageNode);

        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(3);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        receivedMessages.add(message);
                        messageLatch.countDown();
                    }
                })
                .connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(placeRoadMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);
        assertEquals(MessageType.PLACE_ROAD, responseMessage.getType());
        assertEquals(player2, responseMessage.getPlayer());
        assertEquals(lobbyId, responseMessage.getLobbyId());

        var actualRoad = gameService.getGameboardByLobbyId(lobbyId).getRoadList().getFirst();
        assertEquals(player2, actualRoad.getOwnerPlayerId());
        verify(gameService).placeRoad(lobbyId, player2, actualRoad.getId());
    }

    @ParameterizedTest
    @MethodSource("invalidPlaceRoadMessageNodes")
    void placementOfRoadShouldFailForInvalidMessageNode(ObjectNode placeRoadMessageNode) throws GameException, JsonProcessingException, InterruptedException {
        //Setup Players, Lobby and Gameboard
        String player1 = "Player1";
        String player2 = "Player2";
        String lobbyId = lobbyService.createLobby(player1);
        lobbyService.joinLobbyByCode(lobbyId, player2);
        gameService.createGameboard(lobbyId);
        //Create message DTO
        var placeRoadMessageDTO = new MessageDTO(MessageType.PLACE_ROAD, player2, lobbyId, placeRoadMessageNode);

        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        receivedMessages.add(message);
                        messageLatch.countDown();
                    }
                })
                .connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(placeRoadMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.getLast(), MessageDTO.class);
        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertEquals("Invalid road id: id = %s".formatted(placeRoadMessageDTO.getMessageNode("roadId")), responseMessage.getMessageNode("error").textValue());

        verify(gameService, never()).placeSettlement(anyString(), anyString(), anyInt());
    }

    static Stream<Arguments> invalidPlaceSettlementMessageNodes() {
        return Stream.of(
                Arguments.of(JsonNodeFactory.instance.objectNode().put("settlementPositionId", "NoInteger")),
                Arguments.of(JsonNodeFactory.instance.objectNode().put("roadId", "1"))
        );
    }

    static Stream<Arguments> invalidPlaceRoadMessageNodes() {
        return Stream.of(
                Arguments.of(JsonNodeFactory.instance.objectNode().put("roadId", "NoInteger")),
                Arguments.of(JsonNodeFactory.instance.objectNode().put("settlementPositionId", "1"))
        );
    }
}
