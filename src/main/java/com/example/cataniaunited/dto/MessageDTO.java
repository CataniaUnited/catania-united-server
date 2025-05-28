package com.example.cataniaunited.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

/**
 * Data Transfer Object for messages exchanged via WebSocket.
 * Contains information about the message type, sender, lobby, and payload.
 */
public class MessageDTO {

    private MessageType type;
    private String player;
    private String lobbyId;
    private List<String> players;
    private ObjectNode message;

    public MessageDTO() {
    }

    public MessageDTO(MessageType type, ObjectNode message) {
        this.type = type;
        this.message = message;
    }

    public MessageDTO(MessageType type, String player, String lobbyId) {
        this.type = type;
        this.player = player;
        this.lobbyId = lobbyId;
    }
    public MessageDTO(MessageType type, String player, String lobbyId, ObjectNode message) {
        this(type, player, lobbyId);
        this.message = message;
    }

    public MessageDTO(MessageType type, String player, String lobbyId, List<String> players) {
        this(type, player, lobbyId);
        this.players = players;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public List<String> getPlayers() {
        return players;
    }

    public ObjectNode getMessage() {
        return message;
    }


    /**
     * Retrieves a specific node from the message payload by its name.
     * If the node does not exist, an empty ObjectNode is returned.
     *
     * @param nodeName The name of the JSON node to retrieve.
     * @return The {@link JsonNode} if found, or an empty {@link ObjectNode} otherwise.
     */
    public JsonNode getMessageNode(String nodeName) {
        return Optional.ofNullable(message.get(nodeName)).orElse(JsonNodeFactory.instance.objectNode());
    }

    @Override
    public String toString() {
        return "MessageDTO{" +
                "type=" + type +
                ", player='" + player + '\'' +
                ", lobbyId='" + lobbyId + '\'' +
                ", players=" + players +
                ", message=" + message +
                '}';
    }
}