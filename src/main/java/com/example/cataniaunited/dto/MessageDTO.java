package com.example.cataniaunited.dto;

public class MessageDTO {
    private String type;
    private String player;
    private String lobbyId;

    public MessageDTO() {}

    public MessageDTO(String type, String player, String lobbyId) {
        this.type = type;
        this.player = player;
        this.lobbyId = lobbyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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
}