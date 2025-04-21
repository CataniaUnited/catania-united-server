package com.example.cataniaunited.dto;

public enum MessageType {

    //Server Messages
    CONNECTION_SUCCESSFUL,
    CLIENT_DISCONNECTED,
    ERROR,
    LOBBY_UPDATED,
    PLAYER_JOINED,
    LOBBY_CREATED,
    DICE_RESULT,

    //Client Messages
    CREATE_LOBBY,
    JOIN_LOBBY,
    SET_USERNAME,
    PLACE_SETTLEMENT,
    PLACE_ROAD,
    ROLL_DICE
}