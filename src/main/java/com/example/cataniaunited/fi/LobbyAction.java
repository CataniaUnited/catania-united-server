package com.example.cataniaunited.fi;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.lobby.Lobby;

@FunctionalInterface
public interface LobbyAction {
    void execute(Lobby lobby) throws GameException;
}
