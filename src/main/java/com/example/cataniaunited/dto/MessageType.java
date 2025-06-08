package com.example.cataniaunited.dto;

public enum MessageType {

    //Server Messages
    CONNECTION_SUCCESSFUL,
    CLIENT_DISCONNECTED,
    ERROR,
    LOBBY_UPDATED,
    PLAYER_JOINED,
    LOBBY_CREATED,
    GAME_WON,
    GAME_BOARD_JSON,
    GAME_STARTED,
    DICE_RESULT,
    ROBBER_PHASE,
    NEXT_TURN,

    //Client Messages
    CREATE_LOBBY,
    JOIN_LOBBY,
    LEAVE_LOBBY,
    SET_USERNAME,
    START_GAME,
    PLACE_SETTLEMENT,
    UPGRADE_SETTLEMENT,
    PLACE_ROAD,
    ROLL_DICE,
    PLACE_ROBBER,
    END_TURN,
    SET_READY
}