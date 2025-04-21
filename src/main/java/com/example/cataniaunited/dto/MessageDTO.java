package com.example.cataniaunited.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

public class MessageDTO {

    private MessageType type;
    private String player;
    private String lobbyId;
    private List<String> players;
    //Generic JSON Object
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
        this.type = type;
        this.player = player;
        this.lobbyId = lobbyId;
        this.message = message;
    }

    public MessageDTO(MessageType type, String player, String lobbyId, List<String> players) {
        this.type = type;
        this.player = player;
        this.lobbyId = lobbyId;
        this.players = players;
    }

    public MessageDTO(MessageType type, String player, String lobbyId, int dice1, int dice2) {
        this.type = type;
        this.player = player;
        this.lobbyId = lobbyId;
        this.message = JsonNodeFactory.instance.objectNode()
                .put("dice1", dice1)
                .put("dice2", dice2)
                .put("total", dice1 + dice2);
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

    // NEW getters/setters for players
    public List<String> getPlayers() {
        return players;
    }

    public ObjectNode getMessage() {
        return message;
    }

    /**
     * Returns the json node of this message with the given name.
     * If no node is found, an empty object node is returned to prevent NullPointerException
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
