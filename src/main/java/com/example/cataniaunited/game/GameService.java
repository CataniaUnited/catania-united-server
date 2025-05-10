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
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central game-state service.
 */
@ApplicationScoped
public class GameService {

    private static final Logger LOG = Logger.getLogger(GameService.class);
    private static final ConcurrentHashMap<String, GameBoard> BOARDS = new ConcurrentHashMap<>();

    @Inject LobbyService  lobbyService;
    @Inject PlayerService playerService;

    /* ────────────────── board / start game ────────────────── */

    public GameBoard createGameboard(String lobbyId) throws GameException {
        Lobby lob = lobbyService.getLobbyById(lobbyId);
        GameBoard board = new GameBoard(lob.getPlayers().size());
        BOARDS.put(lobbyId, board);
        return board;
    }

    public MessageDTO startGame(String lobbyId) throws GameException {
        Lobby lob = lobbyService.getLobbyById(lobbyId);

        if (lob.isGameStarted())
            throw new GameException("Game already started");
        if (lob.getPlayers().size() < 2)
            throw new GameException("Need at least 2 players");

        GameBoard board = createGameboard(lobbyId);

        List<String> order = new ArrayList<>(lob.getPlayers());
        Collections.shuffle(order);
        lob.setPlayerOrder(order);
        lob.setActivePlayer(order.get(0));
        lob.setGameStarted(true);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.putPOJO("playerOrder", order);
        payload.set("board", board.getJson());

        MessageDTO dto = new MessageDTO(
                MessageType.GAME_STARTED, null, lobbyId, payload);

        order.stream()
                .map(playerService::getPlayerById)
                .filter(Objects::nonNull)
                .forEach(p -> p.sendMessage(dto));

        LOG.infov("Game started in lobby {0}  order {1}", lobbyId, order);
        return dto;
    }

    /* ────────────────── in-game actions ──────────────────── */

    public void placeSettlement(String lobbyId, String playerId, int pos)
            throws GameException {

        GameBoard b = board(lobbyId);
        b.placeSettlement(playerId,
                lobbyService.getPlayerColor(lobbyId, playerId), pos);
        playerService.addVictoryPoints(playerId, 1);
    }

    public void placeRoad(String lobbyId, String playerId, int roadId)
            throws GameException {

        GameBoard b = board(lobbyId);
        b.placeRoad(playerId,
                lobbyService.getPlayerColor(lobbyId, playerId), roadId);
    }

    /* ────────────────── helper / access ──────────────────── */

    public ObjectNode getGameboardJsonByLobbyId(String lobbyId)
            throws GameException {
        return board(lobbyId).getJson();
    }

    private GameBoard board(String lobbyId) throws GameException {
        GameBoard b = BOARDS.get(lobbyId);
        if (b == null)
            throw new GameException("Gameboard for Lobby not found: %s", lobbyId);
        return b;
    }

    /* ────────────────── broadcast win ────────────────────── */

    public Uni<MessageDTO> broadcastWin(io.quarkus.websockets.next.WebSocketConnection conn,
                                        String lobbyId, String winner) {

        ObjectNode win = JsonNodeFactory.instance.objectNode()
                .put("winner", winner);
        MessageDTO msg = new MessageDTO(MessageType.GAME_WON, winner, lobbyId, win);
        return conn.broadcast().sendText(msg)
                .chain(__ -> Uni.createFrom().item(msg));
    }
}
