package com.example.cataniaunited.api;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;



import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.player.Player;
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

@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    private static final Logger logger = Logger.getLogger(GameWebSocket.class);

    @Inject
    LobbyService lobbyService;

    @OnOpen
    public Uni<String> onOpen(WebSocketConnection connection) {
        logger.infof("Client connected: %s", connection.id());
        new Player(connection);
        return Uni.createFrom().item("Connection successful");
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        logger.infof("Client closed connection: %s", connection.id());
        Player.removePlayer(connection);
        connection.broadcast().sendTextAndAwait("Client disconnected");
    }

    @OnTextMessage
    public Uni<MessageDTO> onTextMessage(MessageDTO message, WebSocketConnection connection) {
        logger.infof("Received message from client %s: %s", connection.id(), message.getType());
        if (message.getType() == MessageType.SET_USERNAME) {
            Player player = Player.getPlayerByConnection(connection);
            if (player != null) {
                player.setUsername(message.getPlayer());

                List<String> allPlayers = Player.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .collect(Collectors.toList());
                MessageDTO update = new MessageDTO(MessageType.LOBBY_UPDATED, player.getUsername(), null);
                update.setPlayers(allPlayers);

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(update);
                    return connection.broadcast().sendText(json)
                            .replaceWith(Uni.createFrom().item(new MessageDTO(MessageType.LOBBY_UPDATED, player.getUsername(), null)));
                } catch (JsonProcessingException e) {
                    logger.error("Error converting LOBBY_UPDATED to JSON", e);
                    return Uni.createFrom().item(
                            new MessageDTO(MessageType.ERROR, "Server", "JSON error")
                    );
                }
            }
            return Uni.createFrom().item(
                    new MessageDTO(MessageType.ERROR, "Server", "No player session")
            );
        }
        else if (message.getType() == MessageType.CREATE_LOBBY) {
            String lobbyId = lobbyService.createLobby(message.getPlayer());
            return Uni.createFrom().item(
                    new MessageDTO(MessageType.LOBBY_CREATED, message.getPlayer(), lobbyId)
            );
        }
        return Uni.createFrom().item(
                new MessageDTO(MessageType.ERROR, "Server", "Unknown command")
        );
    }
}