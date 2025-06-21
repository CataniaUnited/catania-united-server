package com.example.cataniaunited.util;

import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.player.Player;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.websockets.next.WebSocketConnection;

public class WebSocketUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectNode createMessage(MessageType type, String key, String value) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type.name());
        node.put(key, value);
        return node;
    }

    public static void sendToPlayer(Player player, ObjectNode message) {
        if (player == null) {
            System.err.println("sendToPlayer: Player is null");
            return;
        }

        WebSocketConnection connection = player.getConnection();

        if (connection == null) {
            System.err.println("sendToPlayer: Connection is null for player " + player.getUniqueId());
            return;
        }

        if (!connection.isOpen()) {
            System.err.println("sendToPlayer: Connection is not open for player " + player.getUniqueId());
            return;
        }

        String json = message.toString();
        if (json == null) {
            System.err.println("sendToPlayer: Message.toString() returned null");
            return;
        }

        System.out.println("sendToPlayer: Sending message to " + player.getUniqueId() + ": " + json);
        connection.sendText(json);
    }
}
