package com.example.cataniaunited.api;

import com.example.cataniaunited.dto.MessageDTO;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * WebSocket endpoint for handling Catan game interactions.
 * This class manages WebSocket connections, receives messages from clients,
 * processes game actions, and broadcasts updates to connected clients.
 */
@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    private static final Logger logger = Logger.getLogger(GameWebSocket.class);

    @Inject
    GameMessageHandler gameMessageHandler;

    /**
     * Handles a new WebSocket connection.
     * A new player is created and associated with the connection.
     * A success message with the player's ID is sent back to the client.
     *
     * @param connection The WebSocket connection established by the client.
     * @return A Uni emitting a {@link MessageDTO} indicating successful connection and the new player's ID.
     */
    @OnOpen
    public Uni<MessageDTO> onOpen(WebSocketConnection connection) {
        logger.infof("Client connected: %s", connection.id());
        return gameMessageHandler.handleInitialConnection(connection);
    }

    /**
     * Handles a WebSocket connection closure.
     * The associated player is removed from the game and a disconnection message
     * is broadcast to other clients in the same lobby (if any).
     *
     * @param connection The WebSocket connection that was closed.
     */
    @OnClose
    public Uni<Void> onClose(WebSocketConnection connection) {
        logger.infof("Client closed connection: %s", connection.id());
        return gameMessageHandler.handleDisconnect(connection);
    }

    /**
     * Handles incoming text messages from a WebSocket client.
     * The message is parsed and routed to the appropriate handler based on its type.
     *
     * @param message    The {@link MessageDTO} received from the client.
     * @param connection The WebSocket connection from which the message was received.
     * @return A Uni emitting a {@link MessageDTO} as a response, or null if no direct response is needed for the client.
     * In case of an error, a Uni emitting an error message is returned.
     */
    @OnTextMessage
    public Uni<MessageDTO> onTextMessage(MessageDTO message, WebSocketConnection connection) {
        logger.infof("Received text message: client = %s, message = %s", connection.id(), message);
        return gameMessageHandler.handleGameMessage(message);
    }

    /**
     * Handles errors occurring on a WebSocket connection.
     * Logs the error and sends an error message back to the client.
     *
     * @param connection The WebSocket connection where the error occurred.
     * @param error      The throwable representing the error.
     * @return A Uni emitting a {@link MessageDTO} containing an error message.
     */
    @OnError
    public Uni<MessageDTO> onError(WebSocketConnection connection, Throwable error) {
        logger.errorf("Unexpected Error occurred: connection = %s, error = %s", connection.id(), error.getMessage());
        return Uni.createFrom().item(gameMessageHandler.createErrorMessage("Unexpected error"));
    }
}

