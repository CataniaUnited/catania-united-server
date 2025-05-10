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
