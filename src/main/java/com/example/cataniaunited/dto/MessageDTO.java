package com.example.cataniaunited.dto;

import java.util.List;

public class MessageDTO {

    private MessageType type;
    private String player;
    private String lobbyId;

    private List<String> players;

    public MessageDTO() {
    }

    public MessageDTO(MessageType type, String player, String lobbyId) {
        this.type = type;
        this.player = player;
        this.lobbyId = lobbyId;
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

    // NEW getters/setters for players
    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }
}
