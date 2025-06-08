package com.example.cataniaunited.game.board;

import com.example.cataniaunited.game.GameService;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.dice.Dice;
import com.example.cataniaunited.game.dice.DiceRoller;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import io.quarkus.websockets.next.WebSocketConnection;
import java.lang.reflect.Field;
import jakarta.inject.Inject;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class RobberTest {

    @Inject
    GameService gameService;

    @InjectSpy
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;

    @Test
    void testRobberStartsOnDesert() {
        GameBoard board = new GameBoard(4);
        Tile desert = board.getTileList().stream()
                .filter(t -> t.getType() == TileType.DESERT)
                .findFirst()
                .orElseThrow();

        assertTrue(desert.hasRobber());
        assertEquals(desert.getId(), board.getRobberTileId());
    }

    /*@Test
    void rollingSevenSetsRobberPhase() throws Exception {
        String lobbyId = lobbyService.createLobby("Player 1");
        lobbyService.joinLobbyByCode(lobbyId, "Player 2");
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        String player1Id = String.valueOf(playerService.getPlayerById("Player 1"));
        lobby.setActivePlayer(player1Id);
        GameBoard board = gameService.createGameboard(lobbyId);

        Field diceRollerField = GameBoard.class.getDeclaredField("diceRoller");
        diceRollerField.setAccessible(true);
        DiceRoller roller = (DiceRoller) diceRollerField.get(board);

        Dice d1 = Mockito.mock(Dice.class);
        Dice d2 = Mockito.mock(Dice.class);
        Mockito.when(d1.roll()).thenReturn(3);
        Mockito.when(d2.roll()).thenReturn(4);

        Field d1Field = DiceRoller.class.getDeclaredField("dice1");
        d1Field.setAccessible(true);
        d1Field.set(roller, d1);
        Field d2Field = DiceRoller.class.getDeclaredField("dice2");
        d2Field.setAccessible(true);
        d2Field.set(roller, d2);

        gameService.rollDice(lobbyId, player1Id);
        assertTrue(gameService.isRobberPlaced(lobbyId));
    }*/

    @Test
    void testRobberBlocksResourceProduction() throws Exception {
        GameBoard board = new GameBoard(2);
        Tile targetTile = board.getTileList().stream()
                .filter(t -> t.getType() != TileType.DESERT)
                .findFirst()
                .orElseThrow();
        BuildingSite bs = targetTile.getBuildingSitesOfTile().get(0);
        Player player = new Player();
        Road road = bs.getRoads().get(0);
        road.setOwner(player);
        board.placeSettlement(player, PlayerColor.LAVENDER, bs.getId());

        int before = player.getResourceCount(targetTile.getType());

        Field diceRollerField = GameBoard.class.getDeclaredField("diceRoller");
        diceRollerField.setAccessible(true);
        DiceRoller roller = (DiceRoller) diceRollerField.get(board);
        Dice dice1 = Mockito.mock(Dice.class);
        Dice dice2 = Mockito.mock(Dice.class);
        int value = targetTile.getValue();
        int dice1Value = value / 2;
        int dice2Value = value - dice1Value;

        Mockito.when(dice1.roll()).thenReturn(dice1Value);
        Mockito.when(dice2.roll()).thenReturn(dice2Value);

        Field f1 = DiceRoller.class.getDeclaredField("dice1");
        f1.setAccessible(true);
        f1.set(roller, dice1);
        Field f2 = DiceRoller.class.getDeclaredField("dice2");
        f2.setAccessible(true);
        f2.set(roller, dice2);

        board.rollDice();
        int after = player.getResourceCount(targetTile.getType());
        assertEquals(before + 1, after);

        board.placeRobber(targetTile.getId());

        board.rollDice();
        int finalAmount = player.getResourceCount(targetTile.getType());
        assertEquals(after, finalAmount);
    }
}
