package com.example.cataniaunited.dto;

public enum MessageType {

    //Server Messages
    CONNECTION_SUCCESSFUL,
    CLIENT_DISCONNECTED,
    ERROR,
    LOBBY_UPDATED,
    PLAYER_JOINED,
    LOBBY_CREATED,
    START_GAME,

    GAME_BOARD_JSON,

    //Client Messages
    CREATE_LOBBY,
    JOIN_LOBBY,
    SET_USERNAME,
    PLACE_SETTLEMENT,
    PLACE_ROAD,
    CREATE_GAME_BOARD
}