package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.BuildingSite;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.buildings.City;
import com.example.cataniaunited.game.buildings.Settlement;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@QuarkusTest
public class GameWebSocketTest {

    @TestHTTPResource
    URI serverUri;

    @Inject
    OpenConnections connections;

    @InjectSpy
    GameMessageHandler gameMessageHandler;

    @InjectSpy
    GameWebSocket gameWebSocket;

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

        String color = responseMessage.getMessageNode("color").textValue();
        assertNotNull(color, "Color should be present");
        assertTrue(color.matches("#[0-9A-Fa-f]{6}"), "Color should be a valid hex code");
    }

    @Test
    void testWebSocketOnClose() throws InterruptedException, JsonProcessingException {
        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var clientToClose = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").connectAndAwait();

        BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            receivedMessages.add(message);
            messageLatch.countDown();  // Decrease latch count when a message arrives
        }).connectAndAwait();

        assertEquals(2, connections.listAll().size());

        clientToClose.close().await().atMost(Duration.of(5, ChronoUnit.SECONDS));

        // Wait up to 5 seconds for both messages to arrive
        boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);

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

        Player player = new Player("Player 1");
        when(playerService.getPlayerById(player.getUniqueId())).thenReturn(player);
        when(playerService.getAllPlayers()).thenReturn(List.of(player));
        String lobbyId = lobbyService.createLobby("HostPlayer");
        lobbyService.joinLobbyByCode(lobbyId, player.getUniqueId());

        var client = BasicWebSocketConnector.create().baseUri(serverUri).path("/game").onTextMessage((connection, message) -> {
            if (message.startsWith("{")) {
                receivedMessages.add(message);
                latch.countDown();
            }
        }).connectAndAwait();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("username", "Chicken");
        MessageDTO setUsernameMsg = new MessageDTO(MessageType.SET_USERNAME, player.getUniqueId(), lobbyId, payload);
        client.sendTextAndAwait(setUsernameMsg);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Did not receive LOBBY_UPDATED in time");

        MessageDTO received = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);
        assertEquals(MessageType.LOBBY_UPDATED, received.getType());
        assertNotNull(received.getPlayers());
        assertTrue(received.getPlayers().contains("Chicken"));

    }

    @Test
    void testSetUsernameOfNonExistingPlayer() throws Exception {
        String invalidId = "InvalidId";
        String newUsername = "Chicken";
        String expectedErrorMessage = "No player session";

        // We expect one CONNECTION_SUCCESSFUL and one ERROR message
        CountDownLatch connectionLatch = new CountDownLatch(1);
        CountDownLatch errorLatch = new CountDownLatch(1);
        List<MessageDTO> receivedErrorMessages = new CopyOnWriteArrayList<>(); // Store only error messages

        doReturn(null).when(playerService).getPlayerByConnection(any(WebSocketConnection.class));

        var client = BasicWebSocketConnector.create().baseUri(serverUri).path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        try {
                            MessageDTO dto = objectMapper.readValue(message, MessageDTO.class);
                            if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                                System.out.println("Test Client: Received CONNECTION_SUCCESSFUL");
                                connectionLatch.countDown();
                            } else if (dto.getType() == MessageType.ERROR) {
                                System.out.println("Test Client: Received ERROR: " + dto.getMessageNode("error").asText());
                                receivedErrorMessages.add(dto);
                                errorLatch.countDown();
                            } else {
                                System.out.println("Test Client: Received unexpected message type: " + dto.getType());
                            }
                        } catch (JsonProcessingException e) {
                            fail("Test Client: Failed to parse message: " + message, e);
                        }
                    }
                }).connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Did not receive CONNECTION_SUCCESSFUL message");

        // Now send the SET_USERNAME message
        MessageDTO setUsernameMsg = new MessageDTO();
        setUsernameMsg.setType(MessageType.SET_USERNAME);
        setUsernameMsg.setPlayer(invalidId);
        System.out.println("Test Client: Sending SET_USERNAME with username: " + newUsername);
        client.sendTextAndAwait(setUsernameMsg);

        // Wait for the ERROR message
        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Did not receive ERROR message after SET_USERNAME");

        assertEquals(1, receivedErrorMessages.size(), "Should have received exactly one ERROR message DTO.");
        MessageDTO errorDto = receivedErrorMessages.get(0);
        assertEquals(MessageType.ERROR, errorDto.getType());
        assertNotNull(errorDto.getMessage());
        assertEquals(expectedErrorMessage, errorDto.getMessageNode("error").textValue());

        verify(playerService, times(1)).getPlayerById(invalidId);

        client.closeAndAwait();
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
    void testPlayerJoinedLobbySuccess() throws JsonProcessingException, InterruptedException, GameException {
        Player hostPlayer = new Player("HostPlayer");
        when(playerService.getPlayerById(hostPlayer.getUniqueId())).thenReturn(hostPlayer);
        String lobbyId = lobbyService.createLobby(hostPlayer.getUniqueId());

        List<MessageDTO> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(3);
        CountDownLatch connectionLatch = new CountDownLatch(1);
        List<String> playerIds = new ArrayList<>();

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        try {
                            MessageDTO dto = objectMapper.readValue(message, MessageDTO.class);
                            if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                                System.out.println("Test Client: Received CONNECTION_SUCCESSFUL");
                                playerIds.add(dto.getMessageNode("playerId").asText());
                                receivedMessages.add(dto);
                                messageLatch.countDown();
                                connectionLatch.countDown();
                            } else {
                                receivedMessages.add(dto);
                                messageLatch.countDown();
                            }
                        } catch (JsonProcessingException e) {
                            fail("Test Client: Failed to parse message: " + message, e);
                        }
                    }
                })
                .connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Did not receive CONNECTION_SUCCESSFUL");
        String playerId = playerIds.get(0);
        MessageDTO joinLobbyMessage = new MessageDTO(MessageType.JOIN_LOBBY, playerId, lobbyId);

        String sentMessage = objectMapper.writeValueAsString(joinLobbyMessage);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Not all messages were received in time!");
        assertEquals(3, receivedMessages.size());

        MessageDTO responseMessage = receivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_JOINED).findFirst().get();
        assertNotNull(responseMessage);

        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        Player player = playerService.getPlayerById(playerId);
        assertEquals(MessageType.PLAYER_JOINED, responseMessage.getType());
        assertEquals(player.getUniqueId(), responseMessage.getPlayer());
        assertEquals(lobbyId, responseMessage.getLobbyId());
        assertTrue(lobby.getPlayers().contains(player.getUniqueId()));
        assertEquals(lobby.getPlayerColor(player.getUniqueId()).getHexCode(), responseMessage.getMessageNode("color").asText());
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
    void placeSettlementShouldTriggerBroadcastWinIfPlayerWins() throws Exception {
        ObjectMapper localObjectMapper = new ObjectMapper();
        List<String> messages = new CopyOnWriteArrayList<>();

        CountDownLatch connectionLatch1 = new CountDownLatch(1);
        CountDownLatch connectionLatch2 = new CountDownLatch(1);
        CountDownLatch gameLatch = new CountDownLatch(2);

        final String[] player1IdHolder = new String[1];
        var client1 = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    if (msg.startsWith("{")) {
                        messages.add(msg);
                        try {
                            MessageDTO dto = localObjectMapper.readValue(msg, MessageDTO.class);
                            if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                                player1IdHolder[0] = dto.getMessageNode("playerId").asText();
                                connectionLatch1.countDown();
                            } else {
                                gameLatch.countDown();
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse WebSocket message", e);
                        }
                    }
                }).connectAndAwait();

        assertTrue(connectionLatch1.await(5, TimeUnit.SECONDS), "Did not receive player1 connection message");
        String player1Id = player1IdHolder[0];
        assertNotNull(player1Id, "Failed to capture playerId for winning player");

        final String[] player2IdHolder = new String[1];
        BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    if (msg.startsWith("{")) {
                        try {
                            MessageDTO dto = localObjectMapper.readValue(msg, MessageDTO.class);
                            if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                                player2IdHolder[0] = dto.getMessageNode("playerId").asText();
                                connectionLatch2.countDown();
                            } else {
                                gameLatch.countDown();
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse WebSocket message", e);
                        }
                    }
                }).connectAndAwait();

        assertTrue(connectionLatch2.await(5, TimeUnit.SECONDS), "Did not receive player2 connection message");
        String player2Id = player2IdHolder[0];
        assertNotNull(player2Id, "Failed to capture playerId for dummy player");

        String lobbyId = lobbyService.createLobby(player1Id);
        lobbyService.joinLobbyByCode(lobbyId, player2Id);

        GameBoard board = gameService.createGameboard(lobbyId);
        int settlementId = board.getBuildingSitePositionGraph().get(0).getId();
        var playerMock = mock(Player.class);
        when(playerMock.getUsername()).thenReturn("Player 1");
        when(playerMock.getUniqueId()).thenReturn(player1Id);
        when(playerMock.getResourceCount(any(TileType.class))).thenReturn(10);
        board.getBuildingSitePositionGraph().get(0).getRoads().get(0).setOwner(playerMock);
        lobbyService.getLobbyById(lobbyId).setActivePlayer(player1Id);

        doReturn(playerMock).when(playerService).getPlayerById(player1Id);
        doReturn(true).when(playerService).checkForWin(player1Id);

        ObjectNode msgNode = JsonNodeFactory.instance.objectNode().put("settlementPositionId", settlementId);
        MessageDTO msg = new MessageDTO(MessageType.PLACE_SETTLEMENT, player1Id, lobbyId, msgNode);
        client1.sendTextAndAwait(localObjectMapper.writeValueAsString(msg));

        assertTrue(gameLatch.await(5, TimeUnit.SECONDS), "Expected game messages were not received");

        MessageDTO response = messages.stream()
                .map(m -> {
                    try {
                        return localObjectMapper.readValue(m, MessageDTO.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(m -> m != null && m.getType() == MessageType.GAME_WON)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No GAME_WON message received"));

        assertEquals(MessageType.GAME_WON, response.getType());
        assertEquals(player1Id, response.getPlayer());

        assertEquals(playerMock.getUsername(), response.getMessageNode("winner").asText());

        verify(gameMessageHandler).broadcastWin(lobbyId, player1Id);
    }

    @Test
    void testPlaceSettlementIncludesAllPlayersInResponse() throws GameException, JsonProcessingException, InterruptedException {
        String player1 = "Player1";
        String player2 = "Player2";
        String player3 = "Player3";
        String lobbyId = lobbyService.createLobby(player1);
        lobbyService.joinLobbyByCode(lobbyId, player2);
        lobbyService.joinLobbyByCode(lobbyId, player3);

        Player mockPlayer1 = mock(Player.class);
        when(mockPlayer1.getUniqueId()).thenReturn(player1);
        when(mockPlayer1.getUsername()).thenReturn(player1);
        when(mockPlayer1.getResourceJSON()).thenReturn(JsonNodeFactory.instance.objectNode());
        when(mockPlayer1.getResourceCount(any(TileType.class))).thenReturn(10);
        when(mockPlayer1.toJson()).thenReturn(objectMapper.createObjectNode().put("username", player1));

        Player mockPlayer2 = mock(Player.class);
        when(mockPlayer2.getUniqueId()).thenReturn(player2);
        when(mockPlayer2.getUsername()).thenReturn(player2);
        when(mockPlayer2.getResourceJSON()).thenReturn(JsonNodeFactory.instance.objectNode());
        when(mockPlayer2.getResourceCount(any(TileType.class))).thenReturn(10);
        when(mockPlayer2.toJson()).thenReturn(objectMapper.createObjectNode().put("username", player2));

        Player mockPlayer3 = mock(Player.class);
        when(mockPlayer3.getUniqueId()).thenReturn(player3);
        when(mockPlayer3.getUsername()).thenReturn(player3);
        when(mockPlayer3.getResourceJSON()).thenReturn(JsonNodeFactory.instance.objectNode());
        when(mockPlayer3.getResourceCount(any(TileType.class))).thenReturn(10);
        when(mockPlayer3.toJson()).thenReturn(objectMapper.createObjectNode().put("username", player3));

        gameService.startGame(lobbyId, player1);

        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setActivePlayer(player1);
        GameBoard gameBoard = gameService.getGameboardByLobbyId(lobbyId);
        BuildingSite buildingSite = gameBoard.getBuildingSitePositionGraph().get(0);
        buildingSite.getRoads().get(0).setOwner(mockPlayer1);

        int positionId = buildingSite.getId();
        ObjectNode placeSettlementMessageNode = objectMapper.createObjectNode().put("settlementPositionId", positionId);
        var placeSettlementMessageDTO = new MessageDTO(MessageType.PLACE_SETTLEMENT, player1, lobbyId, placeSettlementMessageNode);

        when(playerService.getPlayerById(player1)).thenReturn(mockPlayer1);
        when(playerService.getPlayerById(player2)).thenReturn(mockPlayer2);
        when(playerService.getPlayerById(player3)).thenReturn(mockPlayer3);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    receivedMessages.add(message);
                    messageLatch.countDown();
                })
                .connectAndAwait();

        String sentMessage = objectMapper.writeValueAsString(placeSettlementMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Message not received in time");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);

        JsonNode playersNode = responseMessage.getMessageNode("players");
        assertNotNull(playersNode, "Players node missing from message payload");

        assertTrue(playersNode.has(player1), "Missing player1 in response");
        assertTrue(playersNode.has(player2), "Missing player2 in response");
        assertTrue(playersNode.has(player3), "Missing player3 in response");

        assertEquals(player1, playersNode.get(player1).get("username").asText());
        assertEquals(player2, playersNode.get(player2).get("username").asText());
        assertEquals(player3, playersNode.get(player3).get("username").asText());
    }

    @Test
    void testUpgradeOfSettlement() throws GameException, JsonProcessingException, InterruptedException {
        Player player = new Player("Player1");
        player.receiveResource(TileType.WHEAT, 2);
        player.receiveResource(TileType.ORE, 3);
        String playerId = player.getUniqueId();
        when(playerService.getPlayerById(playerId)).thenReturn(player);

        String lobbyId = lobbyService.createLobby("Host Player");
        lobbyService.joinLobbyByCode(lobbyId, playerId);
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setActivePlayer(playerId);
        GameBoard gameBoard = gameService.createGameboard(lobbyId);
        BuildingSite buildingSite = gameBoard.getBuildingSitePositionGraph().get(0);
        buildingSite.getRoads().get(0).setOwner(player);
        buildingSite.setBuilding(new Settlement(player, lobby.getPlayerColor(playerId)));

        int positionId = buildingSite.getId();
        ObjectNode placeSettlementMessageNode = objectMapper.createObjectNode().put("settlementPositionId", positionId);
        var upgradeSettlementMessageDTO = new MessageDTO(MessageType.UPGRADE_SETTLEMENT, playerId, lobbyId, placeSettlementMessageNode);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(3);

        final List<String> actualPlayerIds = new CopyOnWriteArrayList<>();
        CountDownLatch connectionLatch = new CountDownLatch(1);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        try {
                            MessageDTO dto = objectMapper.readValue(message, MessageDTO.class);
                            if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                                actualPlayerIds.add(dto.getMessageNode("playerId").asText());
                                connectionLatch.countDown();
                                messageLatch.countDown();
                            } else {
                                receivedMessages.add(message);
                                messageLatch.countDown();
                            }
                        } catch (JsonProcessingException e) {
                            fail("Failed to parse message");
                        }
                    }
                }).connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS));
        lobbyService.joinLobbyByCode(lobbyId, actualPlayerIds.get(0));

        String sentMessage = objectMapper.writeValueAsString(upgradeSettlementMessageDTO);
        webSocketClientConnection.sendTextAndAwait(sentMessage);

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Message not received in time");

        MessageDTO responseMessage = objectMapper.readValue(receivedMessages.get(receivedMessages.size() - 1), MessageDTO.class);
        assertEquals(MessageType.UPGRADE_SETTLEMENT, responseMessage.getType());
        assertEquals(playerId, responseMessage.getPlayer());
        assertEquals(lobbyId, responseMessage.getLobbyId());

        var building = gameService.getGameboardByLobbyId(lobbyId).getJson().get("settlementPositions").get(0).get("building");
        assertEquals(player, buildingSite.getBuildingOwner());
        assertEquals(City.class.getSimpleName(), building.get("type").asText());
        verify(gameService).upgradeSettlement(lobbyId, playerId, buildingSite.getId());
    }


    @Test
    void testPlacementOfRoad() throws Exception {
        String player1 = "Player1";
        String player2 = "Player2";

        Player mockPlayer2 = mock(Player.class);
        when(mockPlayer2.getUniqueId()).thenReturn(player2);
        when(mockPlayer2.getUsername()).thenReturn(player2);
        when(mockPlayer2.getResourceJSON()).thenReturn(objectMapper.createObjectNode().put("wood", 1));
        when(mockPlayer2.getResourceCount(any(TileType.class))).thenReturn(10);
        when(mockPlayer2.toJson()).thenReturn(objectMapper.createObjectNode().put("username", player2));
        when(playerService.getPlayerById(player2)).thenReturn(mockPlayer2);

        String lobbyId = lobbyService.createLobby(player1);
        lobbyService.joinLobbyByCode(lobbyId, player2);
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setActivePlayer(player2);

        GameBoard gameBoard = gameService.createGameboard(lobbyId);
        int roadId = gameBoard.getRoadList().get(0).getId();
        doReturn(gameBoard).when(gameService).getGameboardByLobbyId(lobbyId);

        ObjectNode mergedBoardJson = objectMapper.createObjectNode().put("merged", "gameData");
        doReturn(mergedBoardJson).when(gameMessageHandler).createGameBoardWithPlayers(lobbyId);

        ObjectNode placeRoadMessageNode = objectMapper.createObjectNode().put("roadId", roadId);
        MessageDTO placeRoadMessageDTO = new MessageDTO(MessageType.PLACE_ROAD, player2, lobbyId, placeRoadMessageNode);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(3); // Expect CONNECTION_SUCCESSFUL, PLAYER_RESOURCES, PLACE_ROAD

        final List<String> actualPlayerIds = new CopyOnWriteArrayList<>();
        CountDownLatch connectionLatch = new CountDownLatch(1);

        var webSocketClientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((connection, message) -> {
                    if (message.startsWith("{")) {
                        try {
                            MessageDTO dto = objectMapper.readValue(message, MessageDTO.class);
                            if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                                actualPlayerIds.add(dto.getMessageNode("playerId").asText());
                                connectionLatch.countDown();
                                latch.countDown();
                            } else {
                                receivedMessages.add(message);
                                latch.countDown();
                            }
                        } catch (JsonProcessingException e) {
                            fail("Failed to parse message");
                        }
                    }
                }).connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS));
        lobbyService.joinLobbyByCode(lobbyId, actualPlayerIds.get(0));

        webSocketClientConnection.sendTextAndAwait(objectMapper.writeValueAsString(placeRoadMessageDTO));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Not all expected messages were received");

        MessageDTO placeRoadResponse = receivedMessages.stream()
                .map(msg -> {
                    try {
                        return objectMapper.readValue(msg, MessageDTO.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(dto -> dto != null && dto.getType() == MessageType.PLACE_ROAD)
                .findFirst()
                .orElseThrow(() -> new AssertionError("PLACE_ROAD message not received"));

        assertEquals(player2, placeRoadResponse.getPlayer());
        assertEquals(lobbyId, placeRoadResponse.getLobbyId());
        assertNotNull(placeRoadResponse.getMessage());
        assertTrue(placeRoadResponse.getMessage().has("merged"), "Expected 'merged' field in message payload");

        var actualRoad = gameService.getGameboardByLobbyId(lobbyId).getRoadList().get(0);
        assertEquals(mockPlayer2, actualRoad.getOwner());

        verify(gameService).placeRoad(lobbyId, player2, roadId);
        verify(playerService, times(2)).getPlayerById(player2);
        verify(gameMessageHandler).createGameBoardWithPlayers(lobbyId);
        verify(gameService, times(2)).getGameboardByLobbyId(lobbyId);
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
    void testPlayerColorIsIncludedInGameBoardJson() throws Exception {
        String playerId = "TestPlayer";
        String lobbyId = lobbyService.createLobby(playerId);

        PlayerColor assignedColor = lobbyService.getPlayerColor(lobbyId, playerId);
        assertNotNull(assignedColor);

        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        ObjectNode playerJson = objectMapper.createObjectNode().put("username", playerId);
        when(mockPlayer.toJson()).thenReturn(playerJson);
        when(playerService.getPlayerById(playerId)).thenReturn(mockPlayer);

        ObjectNode boardJson = objectMapper.createObjectNode().put("hexes", "fake");
        ObjectNode fullJson = objectMapper.createObjectNode();
        fullJson.set("gameboard", boardJson);
        ObjectNode playersNode = fullJson.putObject("players");
        ObjectNode playerData = playerJson.deepCopy();
        playerData.put("color", assignedColor.getHexCode());
        playersNode.set(playerId, playerData);

        doReturn(fullJson).when(gameMessageHandler).createGameBoardWithPlayers(lobbyId);

        ObjectNode result = gameMessageHandler.createGameBoardWithPlayers(lobbyId);

        assertTrue(result.has("gameboard"));
        assertTrue(result.has("players"));

        JsonNode playersJson = result.get("players");
        assertTrue(playersJson.has(playerId));

        JsonNode playerNode = playersJson.get(playerId);
        assertTrue(playerNode.has("color"));
        assertEquals(assignedColor.getHexCode(), playerNode.get("color").asText());
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

        final List<String> actualPlayerIds = new CopyOnWriteArrayList<>();
        CountDownLatch connectionLatch = new CountDownLatch(1);

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
                            } else if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                                actualPlayerIds.add(dto.getMessageNode("playerId").asText());
                                connectionLatch.countDown();
                            }
                        } catch (JsonProcessingException e) {
                            fail("Failed to parse message");
                        }
                    }
                }).connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS));
        lobbyService.joinLobbyByCode(lobbyId, actualPlayerIds.get(0));

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

        verify(gameService).rollDice(lobbyId, player1);
    }


    @Test
    void testPlaceSettlement_success_noWin() throws Exception {
        Player player = new Player("Player1");
        String playerId = player.getUniqueId();
        doReturn(player).when(playerService).getPlayerById(playerId);

        int settlementPositionId = 5;

        String actualLobbyId = lobbyService.createLobby(playerId);
        assertNotNull(actualLobbyId, "Lobby ID should not be null after creation");

        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setActivePlayer(playerId);

        GameBoard mockGameBoard = mock(GameBoard.class);
        ObjectNode boardStateJson = objectMapper.createObjectNode().put("boardState", "updatedAfterSettlement");

        ObjectNode fullMessage = objectMapper.createObjectNode();
        fullMessage.set("gameboard", boardStateJson);
        fullMessage.set("players", objectMapper.createObjectNode()); // Simulates empty player map

        when(mockGameBoard.getJson()).thenReturn(boardStateJson);
        doReturn(mockGameBoard).when(gameService).getGameboardByLobbyId(actualLobbyId);
        doNothing().when(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);
        when(playerService.checkForWin(playerId)).thenReturn(false);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch responseLatch = new CountDownLatch(2);

        final List<String> actualPlayerIds = new CopyOnWriteArrayList<>();
        CountDownLatch connectionLatch = new CountDownLatch(1);

        var clientConnection = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.PLACE_SETTLEMENT) {
                            receivedMessages.add(msg);
                            responseLatch.countDown();
                        } else if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                            actualPlayerIds.add(dto.getMessageNode("playerId").asText());
                            connectionLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Failed to parse message: " + msg, e);
                    }
                }).connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS));
        lobbyService.joinLobbyByCode(actualLobbyId, actualPlayerIds.get(0));

        ObjectNode payload = JsonNodeFactory.instance.objectNode().put("settlementPositionId", settlementPositionId);
        MessageDTO placeSettlementMsg = new MessageDTO(MessageType.PLACE_SETTLEMENT, playerId, actualLobbyId, payload);
        clientConnection.sendTextAndAwait(objectMapper.writeValueAsString(placeSettlementMsg));

        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Did not receive PLACE_SETTLEMENT responses in time.");
        assertEquals(2, receivedMessages.size());

        for (String msg : receivedMessages) {
            MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
            assertEquals(MessageType.PLACE_SETTLEMENT, dto.getType());
            assertEquals(playerId, dto.getPlayer());
            assertEquals(actualLobbyId, dto.getLobbyId());

            assertTrue(dto.getMessage().has("gameboard"));
            assertEquals("updatedAfterSettlement", dto.getMessage().get("gameboard").get("boardState").asText());
        }

        verify(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);
        verify(playerService, atLeastOnce()).checkForWin(playerId);
        verify(gameService, times(1)).getGameboardByLobbyId(actualLobbyId);
    }

    @Test
    void testHandleDiceRoll_ResourceDistribution_Success() throws Exception {
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

        gameService.startGame(actualLobbyId, player1ActualId);
        lobby.setActivePlayer(player1ActualId);

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
        assertTrue(p1ResMsg.getMessage().has("CLAY"), "Player 1 resource missing in payload");

        assertEquals(1, player2ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).count());
        MessageDTO p2ResMsg = player2ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).findFirst().get();
        assertEquals(player2ActualId, p2ResMsg.getPlayer());
        assertEquals(actualLobbyId, p2ResMsg.getLobbyId());
        assertNotNull(p2ResMsg.getMessage());
        assertTrue(p2ResMsg.getMessage().has("CLAY"), "Player 2 resource missing in payload");

        verify(gameService).rollDice(actualLobbyId, player1ActualId);
        verify(playerService, atLeastOnce()).getPlayerById(player1ActualId);
        verify(playerService, atLeastOnce()).getPlayerById(player2ActualId);

        client1Connection.closeAndAwait();
        client2Connection.closeAndAwait();
    }

    @Test
    void testHandleDiceRoll_ResourceDistribution_PlayerConnectionClosed() throws Exception {
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
                            fail("Client2 got DICE_RESULT.");
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

        gameService.startGame(actualLobbyId, player1ActualId);
        System.out.println("Test: Gameboard created for lobby " + actualLobbyId);

        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setActivePlayer(player1ActualId);
        System.out.println("Test: Active player set to " + player1ActualId);

        client2WebSocketClientConnection.closeAndAwait();

        MessageDTO rollDiceMsg = new MessageDTO(MessageType.ROLL_DICE, player1ActualId, actualLobbyId);
        System.out.println("Test: Sending ROLL_DICE from player " + player1ActualId + " for lobby " + actualLobbyId);
        client1WebSocketClientConnection.sendTextAndAwait(objectMapper.writeValueAsString(rollDiceMsg));

        System.out.println("Test: Awaiting DICE_RESULT for player 1...");
        assertTrue(player1DiceResultLatch.await(3, TimeUnit.SECONDS), "Player 1 did not receive DICE_RESULT. Latch: " + player1DiceResultLatch.getCount());
        System.out.println("Test: Awaiting PLAYER_RESOURCES for player 1...");
        assertTrue(player1ResourceLatch.await(3, TimeUnit.SECONDS), "Player 1 did not receive PLAYER_RESOURCES. Latch: " + player1ResourceLatch.getCount() + ". Received messages for P1: " + player1ReceivedMessages.size());

        assertEquals(1, player1ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).count());
        verify(gameService).rollDice(actualLobbyId, player1ActualId);
        verify(playerService, atLeastOnce()).getPlayerById(player1ActualId);
        verify(playerService, atLeastOnce()).getPlayerById(player2ActualId);

        System.out.println("Test: Closing client connections...");
        client1WebSocketClientConnection.closeAndAwait();
        System.out.println("Test: Finished.");
    }

    @Test
    void testHandleDiceRoll_ResourceDistribution_OnePlayerSendFails() throws Exception {
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
                        } else if (dto.getType() == MessageType.PLAYER_RESOURCES) {
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

        MessageDTO rollDiceMsg = new MessageDTO(MessageType.ROLL_DICE, player1ActualId, actualLobbyId);
        client1WebSocketClientConnection.sendTextAndAwait(objectMapper.writeValueAsString(rollDiceMsg));

        assertTrue(diceResultLatch.await(5, TimeUnit.SECONDS), "Dice results not received by both.");
        assertTrue(player1ResourceLatch.await(5, TimeUnit.SECONDS), "Player 1 did not receive PLAYER_RESOURCES.");
        assertEquals(1, player1ReceivedMessages.stream().filter(m -> m.getType() == MessageType.PLAYER_RESOURCES).count());

        verify(gameService).rollDice(actualLobbyId, player1ActualId);
        verify(lobbyService).notifyPlayers(eq(lobby.getLobbyId()), any(MessageDTO.class));

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

        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUsername()).thenReturn(playerId);
        when(playerService.getPlayerById(playerId)).thenReturn(mockPlayer); // <-- Ensure this is how it's resolved

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch gameWonLatch = new CountDownLatch(1);

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
                "Did not receive GAME_WON response in time. Received: " + receivedMessages.size());
        assertEquals(1, receivedMessages.size());

        for (String msg : receivedMessages) {
            MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
            assertEquals(MessageType.GAME_WON, dto.getType());
            assertEquals(playerId, dto.getPlayer());
            assertEquals(actualLobbyId, dto.getLobbyId());
            assertEquals(playerId, dto.getMessageNode("winner").asText());
        }

        verify(gameService).placeSettlement(actualLobbyId, playerId, settlementPositionId);
        verify(playerService, atLeastOnce()).checkForWin(playerId);
        verify(gameMessageHandler, times(1)).broadcastWin(actualLobbyId, playerId);
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
        verify(gameMessageHandler, never()).broadcastWin(anyString(), anyString());
    }

    @Test
    void testHandleStartGame_simple() throws Exception {
        CopyOnWriteArrayList<MessageDTO> seen = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch connectionLatch = new CountDownLatch(1);

        List<String> playerIds = new ArrayList<>();

        var client = BasicWebSocketConnector
                .create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, text) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(text, MessageDTO.class);
                        if (dto.getType() == MessageType.CONNECTION_SUCCESSFUL) {
                            playerIds.add(dto.getMessageNode("playerId").asText());
                            connectionLatch.countDown();
                        } else if (dto.getType() == MessageType.GAME_STARTED) {
                            seen.add(dto);
                            latch.countDown();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        var connection = client.connectAndAwait();

        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS));
        Player player1 = new Player("player1");
        Player player2 = playerService.getPlayerById(playerIds.get(0));

        String lobbyId = lobbyService.createLobby(player1.getUniqueId());
        lobbyService.joinLobbyByCode(lobbyId, player2.getUniqueId());

        MessageDTO startedMessage = new MessageDTO(MessageType.START_GAME, player1.getUniqueId(), lobbyId);
        connection.sendTextAndAwait(startedMessage);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "never saw GAME_STARTED");
        assertEquals(1, seen.size());
        MessageDTO response = seen.get(0);
        assertEquals(MessageType.GAME_STARTED, response.getType());
        assertNotNull(response.getMessageNode("gameboard"));
        assertNotNull(response.getMessageNode("players"));
        assertNotNull(response.getMessageNode("activePlayer"));
        assertTrue(lobbyService.getLobbyById(lobbyId).isGameStarted());

        verify(gameService).startGame(lobbyId, player1.getUniqueId());
        verify(lobbyService, atLeastOnce()).getLobbyById(lobbyId);
    }

    @Test
    void testEndTurn() throws InterruptedException, GameException {
        final String[] client1PlayerIdHolder = new String[1];
        final String[] client2PlayerIdHolder = new String[1];

        CountDownLatch connectionLatch = new CountDownLatch(2);

        List<MessageDTO> player1ReceivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch player1NextTurnLatch = new CountDownLatch(1);

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
                        } else if (dto.getType() == MessageType.NEXT_TURN) {
                            player1ReceivedMessages.add(dto);
                            player1NextTurnLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Client1: Failed to parse message: " + msg, e);
                    }
                });
        var client1WebSocketClientConnection = client1Connector.connectAndAwait();
        System.out.println("Client 1 connection object: " + client1WebSocketClientConnection);


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

        gameService.startGame(actualLobbyId, player1ActualId);
        System.out.println("Test: Gameboard created for lobby " + actualLobbyId);

        Lobby lobby = lobbyService.getLobbyById(actualLobbyId);
        lobby.setPlayerOrder(List.of(player1ActualId, player2ActualId));
        lobby.setActivePlayer(player1ActualId);

        MessageDTO messageDTO = new MessageDTO(MessageType.END_TURN, player1ActualId, actualLobbyId);
        client1WebSocketClientConnection.sendTextAndAwait(messageDTO);

        assertTrue(player1NextTurnLatch.await(10, TimeUnit.SECONDS));
        assertEquals(1, player1ReceivedMessages.size());

        var receivedDTO = player1ReceivedMessages.get(0);
        assertEquals(MessageType.NEXT_TURN, receivedDTO.getType());
        assertEquals(player2ActualId, receivedDTO.getMessageNode("activePlayerId").asText());
        assertNotNull(receivedDTO.getMessageNode("gameboard"));
        assertNotNull(receivedDTO.getMessageNode("players"));

    }

    @Test
    void testJoinLobby_Failure_ThrowsGameException() throws Exception {
        String invalidLobbyId = "invalid123";
        String playerId = "testPlayer";

        doThrow(new GameException("Failed to join lobby: lobby session not found or full"))
                .when(lobbyService).getLobbyById(invalidLobbyId);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch errorLatch = new CountDownLatch(1);

        var client = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    if (msg.startsWith("{")) {
                        try {
                            MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                            if (dto.getType() == MessageType.ERROR) {
                                receivedMessages.add(msg);
                                errorLatch.countDown();
                            }
                        } catch (JsonProcessingException e) {
                            fail("Failed to parse message");
                        }
                    }
                })
                .connectAndAwait();

        MessageDTO joinMessage = new MessageDTO(MessageType.JOIN_LOBBY, playerId, invalidLobbyId);
        client.sendTextAndAwait(objectMapper.writeValueAsString(joinMessage));

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Did not receive ERROR message");
        assertEquals(1, receivedMessages.size());

        MessageDTO errorMessage = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);
        assertEquals(MessageType.ERROR, errorMessage.getType());
        assertEquals("Failed to join lobby: lobby session not found or full",
                errorMessage.getMessageNode("error").asText());

        verify(lobbyService).joinLobbyByCode(invalidLobbyId, playerId);
    }

    @Test
    void testJoinLobby_GameBoardUpdateFails_StillReturnsSuccess() throws Exception {

        Player hostPlayer = new Player("Host Player");
        when(playerService.getPlayerById(hostPlayer.getUniqueId())).thenReturn(hostPlayer);

        Player player = new Player("Player 1");
        String playerId = player.getUniqueId();
        when(playerService.getPlayerById(playerId)).thenReturn(player);

        String lobbyId = lobbyService.createLobby(hostPlayer.getUniqueId());

        doThrow(new GameException("Test exception")).when(gameMessageHandler).createGameBoardWithPlayers(lobbyId);

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch successLatch = new CountDownLatch(1);

        var client = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    if (msg.startsWith("{")) {
                        try {
                            MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                            if (dto.getType() == MessageType.PLAYER_JOINED) {
                                receivedMessages.add(msg);
                                successLatch.countDown();
                            }
                        } catch (JsonProcessingException e) {
                            fail("Failed to parse message");
                        }
                    }
                })
                .connectAndAwait();

        MessageDTO joinMessage = new MessageDTO(MessageType.JOIN_LOBBY, playerId, lobbyId);
        client.sendTextAndAwait(objectMapper.writeValueAsString(joinMessage));

        assertTrue(successLatch.await(5, TimeUnit.SECONDS), "Did not receive PLAYER_JOINED message");
        assertEquals(1, receivedMessages.size());

        MessageDTO successMessage = objectMapper.readValue(receivedMessages.get(0), MessageDTO.class);
        assertEquals(MessageType.PLAYER_JOINED, successMessage.getType());

        verify(lobbyService).joinLobbyByCode(lobbyId, playerId);
    }

    @Test
    void testJoinLobbyFailsWithJoinReturnsFalse() throws Exception {
        String playerId = "FailPlayer";
        String lobbyId = "failLobby";

        MessageDTO joinMessage = new MessageDTO(MessageType.JOIN_LOBBY, playerId, lobbyId);
        doReturn(false).when(lobbyService).joinLobbyByCode(lobbyId, playerId);

        List<MessageDTO> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch errorLatch = new CountDownLatch(1);

        var client = BasicWebSocketConnector.create()
                .baseUri(serverUri)
                .path("/game")
                .onTextMessage((conn, msg) -> {
                    try {
                        MessageDTO dto = objectMapper.readValue(msg, MessageDTO.class);
                        if (dto.getType() == MessageType.ERROR) {
                            receivedMessages.add(dto);
                            errorLatch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        fail("Failed to parse message");
                    }
                })
                .connectAndAwait();

        client.sendTextAndAwait(objectMapper.writeValueAsString(joinMessage));

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS));
        assertEquals(1, receivedMessages.size());

        MessageDTO error = receivedMessages.get(0);
        assertEquals(MessageType.ERROR, error.getType());
        assertEquals("Failed to join lobby: lobby session not found or full",
                error.getMessageNode("error").asText());

        verify(lobbyService).joinLobbyByCode(lobbyId, playerId);
    }


}