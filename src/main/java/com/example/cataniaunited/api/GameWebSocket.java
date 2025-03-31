package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.service.LobbyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        return Uni.createFrom().item("Connection successful");
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        logger.infof("Client closed connection: %s", connection.id());
        connection.broadcast().sendTextAndAwait("Client disconnected");
    }

    @OnTextMessage
    public Uni<MessageDTO> onTextMessage(MessageDTO message, WebSocketConnection connection) throws JsonProcessingException {
        var mapper = new ObjectMapper();
        logger.infof("Received message from client %s: %s", connection.id(), message);
        return Uni.createFrom().item(message);
    }

    private Uni<MessageDTO> handleMessageDTO(MessageDTO message, WebSocketConnection connection) {
        logger.infof("Received message from client %s: %s", connection.id(), message.getType());

        if ("CREATE_LOBBY".equals(message.getType())) {
            String lobbyId = lobbyService.createLobby(message.getPlayer());
            return Uni.createFrom().item(new MessageDTO("LOBBY_CREATED", message.getPlayer(), lobbyId));
        }

        return Uni.createFrom().item(new MessageDTO("ERROR", "Server", "Unknown command"));
    }
}
