package com.example.cataniaunited.game;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.LobbyService;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VictoryPointService {

    private static final Logger logger = Logger.getLogger(VictoryPointService.class);

    @Inject
    LobbyService lobbyService;

    @Inject
    GameService gameService;

    // extend when cities, longest road and development cards are implemented
    public int calculateVictoryPoints(String lobbyId, String playerId) throws GameException {
        GameBoard gameBoard = gameService.getGameboardByLobbyId(lobbyId);
        int settlementPoints = (int) gameBoard.getSettlementPositionGraph().stream()
                .filter(position -> playerId.equals(position.getBuildingOwner()))
                .count();
        return settlementPoints;
    }
}
