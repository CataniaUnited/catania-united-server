package com.example.cataniaunited.game;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VictoryPointService {

    private static final Logger logger = Logger.getLogger(VictoryPointService.class);

    @Inject
    GameService gameService;

    // extend when cities, longest road and development cards are implemented
    public int calculateVictoryPoints(String lobbyId, String playerId) throws GameException {
        GameBoard gameBoard = gameService.getGameboardByLobbyId(lobbyId);
        return (int) gameBoard.getSettlementPositionGraph().stream()
                .filter(position -> playerId.equals(position.getBuildingOwner()))
                .count();
    }

    public boolean checkForWin(String lobbyId, String playerId) throws GameException {
        int victoryPoints = calculateVictoryPoints(lobbyId, playerId);
        return victoryPoints >= 10;
    }

    public Uni<MessageDTO> broadcastWin(WebSocketConnection connection,String lobbyId, String winnerPlayerId){
        ObjectNode message = JsonNodeFactory.instance.objectNode();
        message.put("winner", winnerPlayerId);
        MessageDTO messageDTO = new MessageDTO(MessageType.GAME_WON, winnerPlayerId, lobbyId, message);
        logger.infof("Player %s has won the game in lobby %s", winnerPlayerId, lobbyId);
        return connection.broadcast().sendText(messageDTO).chain(i -> Uni.createFrom().item(messageDTO));
    }
}
