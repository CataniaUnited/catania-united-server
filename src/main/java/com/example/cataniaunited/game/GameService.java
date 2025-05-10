// src/main/java/com/example/cataniaunited/game/GameService.java
package com.example.cataniaunited.game;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@ApplicationScoped
public class GameService {

    private static final Logger LOG = Logger.getLogger(GameService.class);

    private static final ConcurrentHashMap<String, GameBoard> BOARDS = new ConcurrentHashMap<>();

    @Inject LobbyService  lobbyService;
    @Inject PlayerService playerService;


    public GameBoard createGameboard(String lobbyId) throws GameException {
        Lobby lob   = lobbyService.getLobbyById(lobbyId);
        GameBoard b = new GameBoard(lob.getPlayers().size());
        BOARDS.put(lobbyId, b);
        return b;
    }

    public MessageDTO startGame(String lobbyId) throws GameException {
        Lobby lob = lobbyService.getLobbyById(lobbyId);

        if (lob.isGameStarted())
            throw new GameException("Game already started");
        if (lob.getPlayers().size() < 2)
            throw new GameException("Need at least 2 players to start");

        GameBoard board = createGameboard(lobbyId);

        List<String> order = new ArrayList<>(lob.getPlayers());
        Collections.shuffle(order);
        lob.setPlayerOrder(order);      // (tiny setter added in Lobby)
        lob.setActivePlayer(order.getFirst());
        lob.setGameStarted(true);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.putPOJO("playerOrder", order);
        payload.set("board", board.getJson());

        MessageDTO dto = new MessageDTO(
                MessageType.START_GAME,
                null,          // sent by “system”
                lobbyId,
                payload
        );

        /* 4) multicast */
        for (String pid : order) {
            Player p = playerService.getPlayerById(pid);
            if (p != null) p.sendMessage(dto);
        }
        LOG.infov("Game started in lobby {0}  – order {1}", lobbyId, order);
        return dto;
    }


    public void placeSettlement(String lobbyId,
                                String playerId,
                                int pos) throws GameException {

        GameBoard b = getBoard(lobbyId);
        b.placeSettlement(
                playerId,
                lobbyService.getPlayerColor(lobbyId, playerId),
                pos
        );
        playerService.addVictoryPoints(playerId, 1);
    }

    public void placeRoad(String lobbyId,
                          String playerId,
                          int roadId) throws GameException {

        GameBoard b = getBoard(lobbyId);
        b.placeRoad(
                playerId,
                lobbyService.getPlayerColor(lobbyId, playerId),
                roadId
        );
    }


    public GameBoard getGameboardByLobbyId(String lobbyId) throws GameException {
        return getBoard(lobbyId);
    }

    public ObjectNode getBoardJson(String lobbyId) throws GameException {
        return getBoard(lobbyId).getJson();
    }

    private GameBoard getBoard(String lobbyId) throws GameException {
        GameBoard b = BOARDS.get(lobbyId);
        if (b == null) {
            LOG.errorf("No board cached for lobby %s", lobbyId);
            throw new GameException("Gameboard for Lobby not found: %s", lobbyId);
        }
        return b;
    }


    public Uni<MessageDTO> broadcastWin(WebSocketConnection conn,
                                        String lobbyId,
                                        String winnerId) {

        ObjectNode n = JsonNodeFactory.instance.objectNode()
                .put("winner", winnerId);

        MessageDTO m = new MessageDTO(
                MessageType.GAME_WON,
                winnerId,
                lobbyId,
                n
        );
        LOG.infov("Player {0} won in lobby {1}", winnerId, lobbyId);

        return conn.broadcast()
                .sendText(m)
                .chain(i -> Uni.createFrom().item(m));
    }
}
