package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


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
        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);
        var openConnections = connections.listAll().size();
        BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            receivedMessages.add(message);
            messageLatch.countDown();
        }).connectAndAwait();

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        assertEquals(openConnections + 1, connections.listAll().size());
        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);

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

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        // Expecting 2 messages
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            receivedMessages.add(message);
            messageLatch.countDown();  // Decrease latch count when a message arrives
        }).connectAndAwait();

        // Send message
        String sentMessage = objectMapper.writeValueAsString(messageDto);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(2, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);

        assertEquals(MessageType.LOBBY_CREATED, responseMessage.getType()); // Expect LOBBY_CREATED response
        assertEquals("Player 1", responseMessage.getPlayer()); // Player should remain the same
        assertNotNull(responseMessage.getLobbyId());
    }

    @Test
    void testWebSocketOnClose() throws InterruptedException, JsonProcessingException {
        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        // Expecting 2 messages
        CountDownLatch messageLatch = new CountDownLatch(2);

        var clientToClose = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").connectAndAwait();

        BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            receivedMessages.add(message);
            messageLatch.countDown();  // Decrease latch count when a message arrives
        }).connectAndAwait();

        assertEquals(2, connections.listAll().size());

        // Send message
        clientToClose.close().await().atMost(Duration.of(5, ChronoUnit.SECONDS));

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        //Check if client disconnected
        assertEquals(1, connections.listAll().size());
        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(2, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);
        assertEquals(MessageType.CLIENT_DISCONNECTED, responseMessage.getType()); // Expect LOBBY_CREATED response
        assertNotNull(responseMessage.getMessageNode("playerId").textValue());
        verify(playerService).removePlayerByConnectionId(any());
    }

    @Test
    void testInvalidCommand() throws InterruptedException, JsonProcessingException {
        var unknownMessageDto = new MessageDTO();
        unknownMessageDto.setPlayer("Player 1");
        unknownMessageDto.setType(MessageType.ERROR);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                receivedMessages.add(message);
                messageLatch.countDown();
            }
        }).connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(unknownMessageDto);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(2, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);

        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertEquals("Invalid client command", responseMessage.getMessageNode("error").textValue());
    }

    @Test
    void testInvalidClientMessage() throws InterruptedException, JsonProcessingException {
        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                receivedMessages.add(message);
                messageLatch.countDown();
            }
        }).connectAndAwait();

        String sentMessage = objectMapper.createObjectNode().put("type", "INVALID").toString();
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");
        assertEquals(2, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);

        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertEquals("Unexpected error", responseMessage.getMessageNode("error").textValue());
    }

    @Test
    void testSetUsernameCode() throws InterruptedException, JsonProcessingException {
        //Receiving two messages, since change is broadcast as well as returned directly
        CountDownLatch latch = new CountDownLatch(2);
        List<String> receivedMessages = new CopyOnWriteArrayList<>();

        var client = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                receivedMessages.add(message);
                latch.countDown();
            }
        }).connectAndAwait();

        MessageDTO setUsernameMsg = new MessageDTO();
        setUsernameMsg.setType(MessageType.SET_USERNAME);
        setUsernameMsg.setPlayer("Chicken");
        client.sendTextAndAwait(setUsernameMsg);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive LOBBY_UPDATED in time");

        MessageDTO received = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);
        assertEquals(MessageType.LOBBY_UPDATED, received.getType());
        assertEquals("Chicken", received.getPlayer());
        assertNotNull(received.getPlayers());
        assertTrue(received.getPlayers().contains("Chicken"));


    }

    @Test
    void testSetUsernameOfNonExistingPlayer() throws JsonProcessingException, InterruptedException {
        doReturn(null).when(playerService).getPlayerByConnection(any(WebSocketConnection.class));
        CountDownLatch latch = new CountDownLatch(2);
        List<String> receivedMessages = new CopyOnWriteArrayList<>();

        var client = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                receivedMessages.add(message);
                latch.countDown();
            }
        }).connectAndAwait();

        MessageDTO setUsernameMsg = new MessageDTO();
        setUsernameMsg.setType(MessageType.SET_USERNAME);
        setUsernameMsg.setPlayer("Chicken");
        client.sendTextAndAwait(setUsernameMsg);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive all messages");

        MessageDTO received = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);
        assertEquals(MessageType.ERROR, received.getType());
        assertEquals("No player session", received.getMessageNode("error").textValue());
    }

    @Test
    void testJoinLobbySuccess() throws InterruptedException, JsonProcessingException {
        MessageDTO joinLobbyMessage = new MessageDTO(MessageType.JOIN_LOBBY, "Player 1", "abc123");

        doReturn(true).when(lobbyService).joinLobbyByCode("abc123", "Player 1");
        CountDownLatch latch = new CountDownLatch(1);
        BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                latch.countDown();
            }
        }).connectAndAwait().sendTextAndAwait(objectMapper.writeValueAsString(joinLobbyMessage));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive PLAYER_JOINED message in time");
    }

    @Test
    void testPlayerJoinedLobbySuccess() throws JsonProcessingException, InterruptedException {
        String player = "TestPlayer";
        String lobbyId = "xyz123";

        doReturn(true).when(lobbyService).joinLobbyByCode(lobbyId, player);

        MessageDTO joinLobbyMessage = new MessageDTO(MessageType.JOIN_LOBBY, player, lobbyId);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(3);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    receivedMessages.add(message);
                    messageLatch.countDown();
                })
                .connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(joinLobbyMessage);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Not all messages were received in time!");
        assertEquals(3, receivedMessages.size());

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);

        assertEquals(MessageType.PLAYER_JOINED, responseMessage.getType());
        assertEquals(player, responseMessage.getPlayer());
        assertEquals(lobbyId, responseMessage.getLobbyId());
    }

    @Test
    void testJoinLobbyFailure() throws InterruptedException, JsonProcessingException {
        MessageDTO joinLobbyMessage = new MessageDTO(MessageType.JOIN_LOBBY, "Player 1", "invalidLobbyId");

        doReturn(false).when(lobbyService).joinLobbyByCode("invalidLobbyId", "Player 1");
        CountDownLatch latch = new CountDownLatch(1);
        BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                latch.countDown();
            }
        }).connectAndAwait().sendTextAndAwait(objectMapper.writeValueAsString(joinLobbyMessage));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive ERROR message in time");

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

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                receivedMessages.add(message);
                messageLatch.countDown();
            }
        }).connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(placeSettlementMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);
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
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setActivePlayer(player2);
        GameBoard gameBoard = gameService.createGameboard(lobbyId);

        assertNull(gameBoard.getRoadList().get(0).getOwnerPlayerId());
        //Create message DTO
        int positionId = gameBoard.getRoadList().get(0).getId();
        ObjectNode placeRoadMessageNode = objectMapper.createObjectNode().put("roadId", positionId);

        var placeRoadMessageDTO = new MessageDTO(MessageType.PLACE_ROAD, player2, lobbyId, placeRoadMessageNode);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(3);

        var webSocketClientConnection = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                receivedMessages.add(message);
                messageLatch.countDown();
            }
        }).connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(placeRoadMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);
        assertEquals(MessageType.PLACE_ROAD, responseMessage.getType());
        assertEquals(player2, responseMessage.getPlayer());
        assertEquals(lobbyId, responseMessage.getLobbyId());

        var actualRoad = gameService.getGameboardByLobbyId(lobbyId).getRoadList().get(0);
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

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                receivedMessages.add(message);
                messageLatch.countDown();
            }
        }).connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(placeRoadMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

        assertTrue(allMessagesReceived, "Not all messages were received in time!");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);
        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertEquals("Invalid road id: id = %s".formatted(placeRoadMessageDTO.getMessageNode("roadId")), responseMessage.getMessageNode("error").textValue());

        verify(gameService, never()).placeSettlement(anyString(), anyString(), anyInt());
    }

    static Stream<Arguments> invalidPlaceSettlementMessageNodes() {
        return Stream.of(Arguments.of(JsonNodeFactory.instance.objectNode().put("settlementPositionId", "NoInteger")), Arguments.of(JsonNodeFactory.instance.objectNode().put("roadId", "1")));
    }

    static Stream<Arguments> invalidPlaceRoadMessageNodes() {
        return Stream.of(Arguments.of(JsonNodeFactory.instance.objectNode().put("roadId", "NoInteger")), Arguments.of(JsonNodeFactory.instance.objectNode().put("settlementPositionId", "1")));
    }

    @Test
    void testCreateGameBoard() throws InterruptedException, JsonProcessingException, GameException {
        String lobbyId = "lobby123";
        String playerId = "playerABC";
        MessageDTO createBoardMsg = new MessageDTO(MessageType.CREATE_GAME_BOARD, playerId, lobbyId);


        GameBoard mockGameBoard = mock(GameBoard.class);
        ObjectNode expectedBoardJson = objectMapper.createObjectNode().put("boardData", "testValue");
        when(mockGameBoard.getJson()).thenReturn(expectedBoardJson);

        doReturn(mockGameBoard).when(gameService).createGameboard(lobbyId);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch gameBoardMessageLatch = new CountDownLatch(1);

        var client = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                try {
                    MessageDTO receivedDto = objectMapper.readValue(message, MessageDTO.class);
                    receivedMessages.add(message);
                    if (receivedDto.getType() == MessageType.GAME_BOARD_JSON) {
                        gameBoardMessageLatch.countDown();
                    }
                } catch (JsonProcessingException ignored) {
                    fail("A json Processing Exception occurred");
                }
            }
        }).connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(createBoardMsg);
        client.sendTextAndAwait(sentMessage); // Send the CREATE_GAME_BOARD message


        assertTrue(gameBoardMessageLatch.await(5, TimeUnit.SECONDS), "Did not receive GAME_BOARD_JSON message in time");
        verify(gameService).createGameboard(lobbyId);
        verify(mockGameBoard).getJson();

        MessageDTO responseMessage = receivedMessages.stream().map(msgStr -> {
            try {
                return objectMapper.readValue(msgStr, MessageDTO.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(dto -> dto != null && dto.getType() == MessageType.GAME_BOARD_JSON).findFirst().orElse(null);

        assertNotNull(responseMessage, "GAME_BOARD_JSON message should have been received");
        assertEquals(MessageType.GAME_BOARD_JSON, responseMessage.getType());
        assertNull(responseMessage.getPlayer(), "Player field should be null for GAME_BOARD_JSON");
        assertEquals(responseMessage.getLobbyId(), lobbyId, "LobbyId field should be null for GAME_BOARD_JSON");
        assertNotNull(responseMessage.getMessage(), "Message payload (board JSON) should not be null");
        assertEquals(expectedBoardJson, responseMessage.getMessage(), "Board JSON in message should match expected");
    }

    @Test
    void testCreateGameBoardWhereGameServiceThrowsException() throws InterruptedException, JsonProcessingException, GameException {
        String lobbyId = "lobby456";
        String playerId = "playerXYZ";
        String expectedErrorMessage = "Failed to create board for this lobby";
        MessageDTO createBoardMsg = new MessageDTO(MessageType.CREATE_GAME_BOARD, playerId, lobbyId);


        doThrow(new GameException(expectedErrorMessage)).when(gameService).createGameboard(lobbyId);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch errorMessageLatch = new CountDownLatch(1);

        var client = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                try {
                    MessageDTO receivedDto = objectMapper.readValue(message, MessageDTO.class);
                    receivedMessages.add(message);
                    if (receivedDto.getType() == MessageType.ERROR) {
                        errorMessageLatch.countDown();
                    }
                } catch (JsonProcessingException ignored) {
                    fail("A json Processing Exception occurred");
                }
            }
        }).connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(createBoardMsg);
        client.sendTextAndAwait(sentMessage); // Send the CREATE_GAME_BOARD message


        assertTrue(errorMessageLatch.await(5, TimeUnit.SECONDS), "Did not receive ERROR message in time");
        verify(gameService).createGameboard(lobbyId);
        MessageDTO responseMessage = receivedMessages.stream().map(msgStr -> {
            try {
                return objectMapper.readValue(msgStr, MessageDTO.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(dto -> dto != null && dto.getType() == MessageType.ERROR).findFirst().orElse(null);

        assertNotNull(responseMessage, "ERROR message should have been received");
        assertEquals(MessageType.ERROR, responseMessage.getType());
        assertNotNull(responseMessage.getMessage(), "Error payload should not be null");
        assertTrue(responseMessage.getMessage().has("error"), "Error payload should have 'error' field");
        assertEquals(expectedErrorMessage, responseMessage.getMessageNode("error").asText(), "Error message text should match");
    }

    @Test
    void testHandleDiceRoll() throws GameException, JsonProcessingException, InterruptedException {
        String player1 = "Player1";
        String player2 = "Player2";
        String lobbyId = lobbyService.createLobby(player1);
        lobbyService.joinLobbyByCode(lobbyId, player2);
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setActivePlayer(player1);

        gameService.createGameboard(lobbyId);

        MessageDTO rollDiceMessageDTO = new MessageDTO(MessageType.ROLL_DICE, player1, lobbyId);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        try {
                            MessageDTO dto = objectMapper.readValue(message, MessageDTO.class);
                            if (dto.getType() == MessageType.DICE_RESULT) {
                                receivedMessages.add(message);
                                messageLatch.countDown();
                            }
                        } catch (JsonProcessingException e) {
                            fail("Failed to parse message");
                        }
                    }
                }).connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(rollDiceMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        boolean messageReceived = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(messageReceived, "Dice result message not received in time!");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);
        assertEquals(MessageType.DICE_RESULT, responseMessage.getType());
        assertEquals(player1, responseMessage.getPlayer());
        assertEquals(lobbyId, responseMessage.getLobbyId());

        JsonNode diceResult = responseMessage.getMessage();
        assertNotNull(diceResult);
        int dice1 = diceResult.get("dice1").asInt();
        int dice2 = diceResult.get("dice2").asInt();
        assertTrue(dice1 >= 1 && dice1 <= 6, "Dice 1 value out of range");
        assertTrue(dice2 >= 1 && dice2 <= 6, "Dice 2 value out of range");
        assertEquals(dice1 + dice2, diceResult.get("total").asInt());

        verify(gameService).rollDice(lobbyId);
    }


    @Test
    void testPlaceSettlement_success_noWin() throws Exception {
        String playerId = "playerPS1";
        int settlementPositionId = 5;

        String actualLobbyId = lobbyService.createLobby(playerId);
        assertNotNull(actualLobbyId, "Lobby ID should not be null after creation");

        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setActivePlayer(playerId);

        GameBoard mockGameBoard = mock(GameBoard.class);
        ObjectNode updatedBoardJson = objectMapper.createObjectNode().put("boardState", "updatedAfterSettlement");

        when(mockGameBoard.getJson()).thenReturn(updatedBoardJson);

        doReturn(mockGameBoard).when(gameService).getGameboardByLobbyId(actualLobbyId);

        doNothing().when(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);
        when(playerService.checkForWin(playerId)).thenReturn(false);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch responseLatch = new CountDownLatch(2);

        var clientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.PLACE_SETTLEMENT) {
                            receivedMessages.add(msg);
                            responseLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Failed to parse message: " + msg, e);
                    }
                })
                .connectAndAwait();

        ObjectNode payload = JsonNodeFactory.instance.objectNode().put("settlementPositionId", settlementPositionId);
        MessageDTO placeSettlementMsg = new MessageDTO(MessageType.PLACE_SETTLEMENT, playerId, actualLobbyId, payload);

        clientConnection.sendTextAndAwait(objectMapper.writeValueAsString(placeSettlementMsg));

        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Did not receive PLACE_SETTLEMENT responses in time.");
        assertEquals(2, receivedMessages.size());

        MessageDTO responseDto1 = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);
        assertEquals(MessageType.PLACE_SETTLEMENT, responseDto1.getType());
        assertEquals(playerId, responseDto1.getPlayer());
        assertEquals(actualLobbyId, responseDto1.getLobbyId());
        assertEquals(updatedBoardJson, responseDto1.getMessage());

        MessageDTO responseDto2 = objectMapper.readValue(receivedMessages.get(1), MessageDTO.class);
        assertEquals(MessageType.PLACE_SETTLEMENT, responseDto2.getType());
        assertEquals(playerId, responseDto2.getPlayer());
        assertEquals(actualLobbyId, responseDto2.getLobbyId());
        assertEquals(updatedBoardJson, responseDto2.getMessage());

        verify(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);
        verify(playerService, atLeastOnce()).checkForWin(playerId);
        verify(gameService, times(1)).getGameboardByLobbyId(actualLobbyId);
    }


    private Player createMockPlayer(String playerId, String username) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getUsername()).thenReturn(username);
        ObjectNode resources = JsonNodeFactory.instance.objectNode().put(playerId + "_resource", 10);
        when(player.getResourceJSON()).thenReturn(resources);
        return player;
    }

    @Test
    void testHandleDiceRoll_ResourceDistribution_Success() throws Exception {
        String player1Username = "User1";
        String player2Username = "User2";

        final List<String> actualPlayerIds = new CopyOnWriteArrayList<>();

        List<MessageDTO> player1ReceivedMessages = new CopyOnWriteArrayList<>();
        List<MessageDTO> player2ReceivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch player1ResourceLatch = new CountDownLatch(1);
        CountDownLatch player2ResourceLatch = new CountDownLatch(1);
        CountDownLatch diceResultLatch = new CountDownLatch(2);
        CountDownLatch connectionLatch = new CountDownLatch(2);

        BasicWebSocketConnector client1Connector = BasicWebSocketConnector.create()
                .baseUri(serverUri).path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                            actualPlayerIds.add(dto.getMessageNode("playerId").asText());
                            connectionLatch.countDown();
                        } else if (dto.getType() == MessageType.PLAYER_RESOURCES && actualPlayerIds.contains(dto.getPlayer()) && dto.getPlayer().equals(actualPlayerIds.get(0))) {
                            player1ReceivedMessages.add(dto);
                            player1ResourceLatch.countDown();
                        } else if (dto.getType() == MessageType.DICE_RESULT) {
                            diceResultLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Client1: Failed to parse message: " + msg, e);
                    }
                });
        var client1Connection = client1Connector.connectAndAwait();


        BasicWebSocketConnector client2Connector = BasicWebSocketConnector.create()
                .baseUri(serverUri).path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                            actualPlayerIds.add(dto.getMessageNode("playerId").asText());
                            connectionLatch.countDown();
                        } else if (dto.getType() == MessageType.PLAYER_RESOURCES && actualPlayerIds.contains(dto.getPlayer()) && dto.getPlayer().equals(actualPlayerIds.get(1))) {
                            player2ReceivedMessages.add(dto);
                            player2ResourceLatch.countDown();
                        } else if (dto.getType() == MessageType.DICE_RESULT) {
                            diceResultLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Client2: Failed to parse message: " + msg, e);
                    }
                });
        var client2Connection = client2Connector.connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Not all clients connected and sent CONNECTION_SUCCESSFUL");
        assertEquals(2, actualPlayerIds.size(), "Should have two player IDs");

        String player1ActualId = actualPlayerIds.get(0);
        String player2ActualId = actualPlayerIds.get(1);

        String actualLobbyId = lobbyService.createLobby(player1ActualId);
        assertNotNull(actualLobbyId, "Lobby ID should not be null after creation");

        lobbyService.joinLobbyByCode(actualLobbyId, player2ActualId);
        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        assertNotNull(lobby, "Lobby should not be null after retrieval");
        lobby.setActivePlayer(player1ActualId);

        gameService.createGameboard(actualLobbyId);

        Player mockPlayer1 = createMockPlayer(player1ActualId, player1Username);
        Player mockPlayer2 = createMockPlayer(player2ActualId, player2Username);
        when(playerService.getPlayerById(player1ActualId)).thenReturn(mockPlayer1);
        when(playerService.getPlayerById(player2ActualId)).thenReturn(mockPlayer2);


        MessageDTO rollDiceMsg = new MessageDTO(MessageType.ROLL_DICE, player1ActualId, actualLobbyId);
        client1Connection.sendTextAndAwait(objectMapper.writeValueAsString(rollDiceMsg));

        assertTrue(diceResultLatch.await(10, TimeUnit.SECONDS),
                "Both clients did not receive DICE_RESULT message in time. Latch: " + diceResultLatch.getCount());
        assertTrue(player1ResourceLatch.await(10, TimeUnit.SECONDS),
                "Player 1 did not receive PLAYER_RESOURCES message in time. Received: " + player1ReceivedMessages.size() + ". Latch: " + player1ResourceLatch.getCount());
        assertTrue(player2ResourceLatch.await(10, TimeUnit.SECONDS),
                "Player 2 did not receive PLAYER_RESOURCES message in time. Received: " + player2ReceivedMessages.size() + ". Latch: " + player2ResourceLatch.getCount());

        assertEquals(1, player1ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).count());
        MessageDTO p1ResMsg = player1ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).findFirst().get();
        assertEquals(player1ActualId, p1ResMsg.getPlayer());
        assertEquals(actualLobbyId, p1ResMsg.getLobbyId());
        assertNotNull(p1ResMsg.getMessage());
        assertTrue(p1ResMsg.getMessage().has(player1ActualId + "_resource"), "Player 1 resource missing in payload");

        assertEquals(1, player2ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).count());
        MessageDTO p2ResMsg = player2ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).findFirst().get();
        assertEquals(player2ActualId, p2ResMsg.getPlayer());
        assertEquals(actualLobbyId, p2ResMsg.getLobbyId());
        assertNotNull(p2ResMsg.getMessage());
        assertTrue(p2ResMsg.getMessage().has(player2ActualId + "_resource"), "Player 2 resource missing in payload");

        verify(gameService).rollDice(actualLobbyId);
        verify(playerService, times(1)).getPlayerById(player1ActualId);
        verify(playerService, times(1)).getPlayerById(player2ActualId);
        verify(mockPlayer1, times(1)).getResourceJSON();
        verify(mockPlayer2, times(1)).getResourceJSON();

        client1Connection.closeAndAwait();
        client2Connection.closeAndAwait();
    }
    @Test
    void testHandleDiceRoll_ResourceDistribution_PlayerConnectionClosed() throws Exception {
        String player1Username = "UserOpen";
        String player2Username = "UserClosed";

        final String[] client1PlayerIdHolder = new String[1];
        final String[] client2PlayerIdHolder = new String[1];

        CountDownLatch connectionLatch = new CountDownLatch(2);

        List<MessageDTO> player1ReceivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch player1ResourceLatch = new CountDownLatch(1);
        CountDownLatch player1DiceResultLatch = new CountDownLatch(1);

        System.out.println("Setting up Client 1...");
        BasicWebSocketConnector client1Connector = BasicWebSocketConnector.create()
                .baseUri(serverUri).path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        System.out.println("Client1 RX: " + msg);

                        if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                            client1PlayerIdHolder[0] = dto.getMessageNode("playerId").asText();
                            System.out.println("Client1 Connected with ID: " + client1PlayerIdHolder[0]);
                            connectionLatch.countDown();
                        } else if (dto.getType() == MessageType.PLAYER_RESOURCES) {
                            String targetPlayerId = dto.getPlayer();
                            System.out.println("Client1 got PLAYER_RESOURCES for: " + targetPlayerId + ", self ID: " + client1PlayerIdHolder[0]);
                            if (client1PlayerIdHolder[0] != null && client1PlayerIdHolder[0].equals(targetPlayerId)) {
                                System.out.println("Client1: Matched PLAYER_RESOURCES. Counting down latch.");
                                player1ReceivedMessages.add(dto);
                                player1ResourceLatch.countDown();
                            }
                        } else if (dto.getType() == MessageType.DICE_RESULT) {
                            System.out.println("Client1 got DICE_RESULT.");
                            player1DiceResultLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Client1: Failed to parse message: " + msg, e);
                    }
                });
        var client1WebSocketClientConnection = client1Connector.connectAndAwait();
        System.out.println("Client 1 connection object: " + client1WebSocketClientConnection);


        CountDownLatch player2DiceResultLatch = new CountDownLatch(1);
        System.out.println("Setting up Client 2...");
        BasicWebSocketConnector client2Connector = BasicWebSocketConnector.create()
                .baseUri(serverUri).path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        System.out.println("Client2 RX: " + msg);
                        if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                            client2PlayerIdHolder[0] = dto.getMessageNode("playerId").asText();
                            System.out.println("Client2 Connected with ID: " + client2PlayerIdHolder[0]);
                            connectionLatch.countDown();
                        } else if (dto.getType() == MessageType.DICE_RESULT) {
                            System.out.println("Client2 got DICE_RESULT.");
                            player2DiceResultLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Client2: Failed to parse message: " + msg, e);
                    }
                });
        var client2WebSocketClientConnection = client2Connector.connectAndAwait();
        System.out.println("Client 2 connection object: " + client2WebSocketClientConnection);


        assertTrue(connectionLatch.await(10, TimeUnit.SECONDS), "Not all clients connected and received their IDs. Latch: " + connectionLatch.getCount());
        assertNotNull(client1PlayerIdHolder[0], "Client 1 Player ID not set");
        assertNotNull(client2PlayerIdHolder[0], "Client 2 Player ID not set");

        String player1ActualId = client1PlayerIdHolder[0];
        String player2ActualId = client2PlayerIdHolder[0];
        System.out.println("Test: player1ActualId = " + player1ActualId);
        System.out.println("Test: player2ActualId = " + player2ActualId);


        String actualLobbyId = lobbyService.createLobby(player1ActualId);
        System.out.println("Test: Created lobby with ID: " + actualLobbyId + " for host " + player1ActualId);
        lobbyService.joinLobbyByCode(actualLobbyId, player2ActualId);
        System.out.println("Test: Player " + player2ActualId + " joined lobby " + actualLobbyId);
        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setActivePlayer(player1ActualId);
        System.out.println("Test: Active player set to " + player1ActualId);


        gameService.createGameboard(actualLobbyId);
        System.out.println("Test: Gameboard created for lobby " + actualLobbyId);

        Player mockPlayer1 = createMockPlayer(player1ActualId, player1Username);
        Player mockPlayer2 = createMockPlayer(player2ActualId, player2Username);
        when(playerService.getPlayerById(player1ActualId)).thenReturn(mockPlayer1);
        when(playerService.getPlayerById(player2ActualId)).thenReturn(mockPlayer2);
        System.out.println("Test: Player mocks created and getPlayerById configured.");

        WebSocketConnection mockServerSideClosedConnectionForPlayer2 = mock(WebSocketConnection.class);
        when(mockServerSideClosedConnectionForPlayer2.isOpen()).thenReturn(false);
        when(mockServerSideClosedConnectionForPlayer2.id()).thenReturn("mockClosedConnIdForPlayer2-" + player2ActualId);
        when(playerService.getConnectionByPlayerId(player2ActualId)).thenReturn(mockServerSideClosedConnectionForPlayer2);
        System.out.println("Test: Mocked getConnectionByPlayerId for player2ActualId (" + player2ActualId + ") to return a closed connection.");


        MessageDTO rollDiceMsg = new MessageDTO(MessageType.ROLL_DICE, player1ActualId, actualLobbyId);
        System.out.println("Test: Sending ROLL_DICE from player " + player1ActualId + " for lobby " + actualLobbyId);
        client1WebSocketClientConnection.sendTextAndAwait(objectMapper.writeValueAsString(rollDiceMsg));

        System.out.println("Test: Awaiting DICE_RESULT for player 1...");
        assertTrue(player1DiceResultLatch.await(10, TimeUnit.SECONDS), "Player 1 did not receive DICE_RESULT. Latch: " + player1DiceResultLatch.getCount());
        System.out.println("Test: Awaiting DICE_RESULT for player 2...");
        assertTrue(player2DiceResultLatch.await(10, TimeUnit.SECONDS), "Player 2 did not receive DICE_RESULT (broadcast). Latch: " + player2DiceResultLatch.getCount());
        System.out.println("Test: Awaiting PLAYER_RESOURCES for player 1...");
        assertTrue(player1ResourceLatch.await(10, TimeUnit.SECONDS), "Player 1 did not receive PLAYER_RESOURCES. Latch: " + player1ResourceLatch.getCount() + ". Received messages for P1: " + player1ReceivedMessages.size());

        assertEquals(1, player1ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).count());
        verify(gameService).rollDice(actualLobbyId);
        verify(playerService, times(1)).getPlayerById(player1ActualId);
        verify(playerService, times(1)).getPlayerById(player2ActualId);
        verify(mockPlayer1).getResourceJSON();
        verify(mockPlayer2, never()).getResourceJSON();

        System.out.println("Test: Closing client connections...");
        client1WebSocketClientConnection.closeAndAwait();
        client2WebSocketClientConnection.closeAndAwait();
        System.out.println("Test: Finished.");
    }
    @Test
    void testHandleDiceRoll_ResourceDistribution_OnePlayerSendFails() throws Exception {
        String player1Username = "UserOk";
        String player2Username = "UserFail";

        final List<String> actualPlayerIds = new CopyOnWriteArrayList<>();
        CountDownLatch connectionLatch = new CountDownLatch(2);


        List<MessageDTO> player1ReceivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch player1ResourceLatch = new CountDownLatch(1);
        CountDownLatch diceResultLatch = new CountDownLatch(2);

        BasicWebSocketConnector client1Connector = BasicWebSocketConnector.create()
                .baseUri(serverUri).path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                            actualPlayerIds.add(dto.getMessageNode("playerId").asText());
                            connectionLatch.countDown();
                        } else if (dto.getType() == MessageType.PLAYER_RESOURCES && !actualPlayerIds.isEmpty() && dto.getPlayer().equals(actualPlayerIds.get(0))) {
                            player1ReceivedMessages.add(dto);
                            player1ResourceLatch.countDown();
                        } else if (dto.getType() == MessageType.DICE_RESULT) {
                            diceResultLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Client1: " + e.getMessage());
                    }
                });
        var client1WebSocketClientConnection = client1Connector.connectAndAwait();

        BasicWebSocketConnector client2Connector = BasicWebSocketConnector.create()
                .baseUri(serverUri).path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                            actualPlayerIds.add(dto.getMessageNode("playerId").asText());
                            connectionLatch.countDown();
                        } else if (dto.getType() == MessageType.DICE_RESULT) {
                            diceResultLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Client2: " + e.getMessage());
                    }
                });
        var client2WebSocketClientConnection = client2Connector.connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Not all clients connected.");
        assertEquals(2, actualPlayerIds.size());
        String player1ActualId = actualPlayerIds.get(0);
        String player2ActualId = actualPlayerIds.get(1);


        String actualLobbyId = lobbyService.createLobby(player1ActualId);
        lobbyService.joinLobbyByCode(actualLobbyId, player2ActualId);
        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setActivePlayer(player1ActualId);
        gameService.createGameboard(actualLobbyId);

        Player mockPlayer1 = createMockPlayer(player1ActualId, player1Username);
        Player mockPlayer2 = createMockPlayer(player2ActualId, player2Username);
        when(playerService.getPlayerById(player1ActualId)).thenReturn(mockPlayer1);
        when(playerService.getPlayerById(player2ActualId)).thenReturn(mockPlayer2);


        WebSocketConnection mockServerSideFailingConnectionForPlayer2 = mock(WebSocketConnection.class);
        when(mockServerSideFailingConnectionForPlayer2.id()).thenReturn("mockFailingConnIdForPlayer2");
        when(mockServerSideFailingConnectionForPlayer2.isOpen()).thenReturn(true);
        when(mockServerSideFailingConnectionForPlayer2.sendText(any(MessageDTO.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Simulated send failure")));
        when(playerService.getConnectionByPlayerId(player2ActualId)).thenReturn(mockServerSideFailingConnectionForPlayer2);


        MessageDTO rollDiceMsg = new MessageDTO(MessageType.ROLL_DICE, player1ActualId, actualLobbyId);
        client1WebSocketClientConnection.sendTextAndAwait(objectMapper.writeValueAsString(rollDiceMsg));

        assertTrue(diceResultLatch.await(5, TimeUnit.SECONDS), "Dice results not received by both.");
        assertTrue(player1ResourceLatch.await(5, TimeUnit.SECONDS), "Player 1 did not receive PLAYER_RESOURCES.");

        assertEquals(1, player1ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).count());

        verify(gameService).rollDice(actualLobbyId);
        verify(mockPlayer1).getResourceJSON();
        verify(mockPlayer2).getResourceJSON();
        verify(mockServerSideFailingConnectionForPlayer2).sendText(any(MessageDTO.class));

        client1WebSocketClientConnection.closeAndAwait();
        client2WebSocketClientConnection.closeAndAwait();
    }

    @Test
    void testPlaceSettlement_success_playerWins() throws Exception {
        String playerId = "playerPSW";
        int settlementPositionId = 7;

        String actualLobbyId = lobbyService.createLobby(playerId);
        assertNotNull(actualLobbyId);
        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setActivePlayer(playerId);

        doNothing().when(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);
        when(playerService.checkForWin(playerId)).thenReturn(true);


        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch gameWonLatch = new CountDownLatch(2);

        var clientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.GAME_WON) {
                            receivedMessages.add(msg);
                            gameWonLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Failed to parse message: " + msg, e);
                    }
                })
                .connectAndAwait();

        ObjectNode payload = JsonNodeFactory.instance.objectNode().put("settlementPositionId", settlementPositionId);
        MessageDTO placeSettlementMsg = new MessageDTO(MessageType.PLACE_SETTLEMENT, playerId, actualLobbyId, payload);

        clientConnection.sendTextAndAwait(objectMapper.writeValueAsString(placeSettlementMsg));

        assertTrue(gameWonLatch.await(5, TimeUnit.SECONDS),
                "Did not receive 2 GAME_WON responses in time. Received: " + receivedMessages.size());
        assertEquals(2, receivedMessages.size());

        MessageDTO responseDto1 = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);
        assertEquals(MessageType.GAME_WON, responseDto1.getType());
        assertEquals(playerId, responseDto1.getPlayer());
        assertEquals(actualLobbyId, responseDto1.getLobbyId());
        assertEquals(playerId, responseDto1.getMessageNode("winner").asText());

        MessageDTO responseDto2 = objectMapper.readValue(receivedMessages.get(1), MessageDTO.class);
        assertEquals(MessageType.GAME_WON, responseDto2.getType());
        assertEquals(playerId, responseDto2.getPlayer());

        verify(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);
        verify(playerService, atLeastOnce()).checkForWin(playerId);
        verify(gameService, times(1)).broadcastWin(any(WebSocketConnection.class), eq(actualLobbyId), eq(playerId));
        verify(gameService, never()).getGameboardByLobbyId(anyString());
    }

    @Test
    void testPlaceSettlement_invalidPositionId_string() throws Exception {
        String playerId = "playerPSE1";

        String actualLobbyId = lobbyService.createLobby(playerId);
        assertNotNull(actualLobbyId);
        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setActivePlayer(playerId);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch errorLatch = new CountDownLatch(1);

        var clientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.ERROR) {
                            receivedMessages.add(msg);
                            errorLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Failed to parse message: " + msg, e);
                    }
                })
                .connectAndAwait();

        clientConnection.sendTextAndAwait(objectMapper.writeValueAsString(
                new MessageDTO(MessageType.PLACE_SETTLEMENT, playerId, actualLobbyId,
                        JsonNodeFactory.instance.objectNode().put("settlementPositionId", "not-an-integer"))
        ));

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Did not receive ERROR response in time.");
        assertEquals(1, receivedMessages.size());

        MessageDTO responseDto = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);
        assertEquals(MessageType.ERROR, responseDto.getType());
        assertTrue(responseDto.getMessageNode("error").asText().startsWith("Invalid settlement position id: id = "));

        verify(gameService, never()).placeSettlement(anyString(), anyString(), anyInt());
        verify(playerService, never()).checkForWin(anyString());
        verify(gameService, never()).getGameboardByLobbyId(anyString());
    }

    @Test
    void testPlaceSettlement_gameServicePlaceSettlement_throwsGameException() throws Exception {
        String playerId = "playerPSE2";
        int settlementPositionId = 10;
        String gameServiceErrorMessage = "Cannot place settlement here (GameService error)";

        String actualLobbyId = lobbyService.createLobby(playerId);
        assertNotNull(actualLobbyId);
        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setActivePlayer(playerId);

        doThrow(new GameException(gameServiceErrorMessage))
                .when(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch errorLatch = new CountDownLatch(1);

        var clientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.ERROR) {
                            receivedMessages.add(msg);
                            errorLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Failed to parse message: " + msg, e);
                    }
                })
                .connectAndAwait();

        ObjectNode payload = JsonNodeFactory.instance.objectNode().put("settlementPositionId", settlementPositionId);
        MessageDTO placeSettlementMsg = new MessageDTO(MessageType.PLACE_SETTLEMENT, playerId, actualLobbyId, payload);

        clientConnection.sendTextAndAwait(objectMapper.writeValueAsString(placeSettlementMsg));

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Did not receive ERROR response in time.");
        assertEquals(1, receivedMessages.size());

        MessageDTO responseDto = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);
        assertEquals(MessageType.ERROR, responseDto.getType());
        assertEquals(gameServiceErrorMessage, responseDto.getMessageNode("error").asText());

        verify(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);
        verify(playerService, never()).checkForWin(anyString());
        verify(gameService, never()).getGameboardByLobbyId(anyString());
        verify(gameService, never()).broadcastWin(any(WebSocketConnection.class), anyString(), anyString());
    }
}
