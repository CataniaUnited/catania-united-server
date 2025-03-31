package com.example.cataniaunited.api;

import game.activeGame.board.GameBoard;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
@WebSocket(path = "/game")
public class GameWebSocket {

    private static final Logger logger = Logger.getLogger(GameWebSocket.class);

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
    public Uni<String> onTextMessage(String message, WebSocketConnection connection) {
        logger.infof("Received message from client %s: %s\n", connection.id(), message);
        if(message.startsWith("generateBoard")){
            logger.infof("Generating Board");
            try {
                int boardSize = Integer.parseInt(message.substring("generateBoard".length()).trim());
                new GameBoard(boardSize);
            } catch (NumberFormatException e){
                logger.error("Cant Generate Board, cant extract numberOfLayers");
            }
        }
        return Uni.createFrom().item(message);
    }

}
