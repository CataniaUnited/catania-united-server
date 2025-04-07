package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.example.cataniaunited.service.LobbyService;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    private static final Logger logger = Logger.getLogger(GameWebSocket.class);
    private static final String SERVER_NAME = "Server";

    @Inject
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;

    @OnOpen
    public Uni<String> onOpen(WebSocketConnection connection) {
        logger.infof("Client connected: %s", connection.id());
        playerService.addPlayer(connection);
        return Uni.createFrom().item("Connection successful");
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        logger.infof("Client closed connection: %s", connection.id());
        playerService.removePlayer(connection);
        connection.broadcast().sendTextAndAwait("Client disconnected");
    }

    @OnTextMessage
    public Uni<MessageDTO> onTextMessage(MessageDTO message, WebSocketConnection connection) {
        logger.infof("Received message from client %s: %s", connection.id(), message.getType());
        if (message.getType() == MessageType.SET_USERNAME) {
            Player player = playerService.getPlayerByConnection(connection);
            if (player != null) {
                player.setUsername(message.getPlayer());

                List<String> allPlayers = playerService.getAllPlayers().stream()
                        .map(Player::getUsername).toList();
                MessageDTO update = new MessageDTO(MessageType.LOBBY_UPDATED, player.getUsername(), null, allPlayers);
                return connection.broadcast().sendText(update).chain(i -> Uni.createFrom().item(update));
            }
            return Uni.createFrom().item(
                    new MessageDTO(MessageType.ERROR, SERVER_NAME, "No player session")
            );
        } else if (message.getType() == MessageType.CREATE_LOBBY) {
            String lobbyId = lobbyService.createLobby(message.getPlayer());
            return Uni.createFrom().item(
                    new MessageDTO(MessageType.LOBBY_CREATED, message.getPlayer(), lobbyId)
            );
        } else if(message.getType() == MessageType.JOIN_LOBBY){
            boolean joined = lobbyService.joinLobbyByCode(message.getLobbyId(), message.getPlayer());
            if(joined){
                MessageDTO update = new MessageDTO(MessageType.PLAYER_JOINED, message.getPlayer(), message.getLobbyId());
                return connection.broadcast().sendText(update).chain(i -> Uni.createFrom().item(update));
            }
            return Uni.createFrom().item(
                    new MessageDTO(MessageType.ERROR, SERVER_NAME,"Invalid or expired lobby code"));
        }


        return Uni.createFrom().item(
                new MessageDTO(MessageType.ERROR, SERVER_NAME, "Unknown command")
        );
    }
}