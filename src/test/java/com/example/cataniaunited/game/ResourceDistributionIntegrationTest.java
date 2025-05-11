package com.example.cataniaunited.game;

import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.game.board.Road;
import com.example.cataniaunited.game.board.SettlementPosition;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.dice.Dice;
import com.example.cataniaunited.game.dice.DiceRoller;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.lobby.LobbyService;
import com.example.cataniaunited.lobby.LobbyServiceImpl;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.example.cataniaunited.player.PlayerService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@QuarkusTest
class ResourceDistributionIntegrationTest {

    @Inject
    GameService gameService;

    @Inject
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;

    private Player testPlayer;
    private String lobbyId;
    private GameBoard gameBoard;

    @BeforeEach
    void setUp() throws Exception {
        WebSocketConnection mockWsConn = Mockito.mock(WebSocketConnection.class);
        Mockito.when(mockWsConn.id()).thenReturn("mock-test-conn-" + System.nanoTime());
        testPlayer = playerService.addPlayer(mockWsConn);
        WebSocketConnection mockWsConn2 = Mockito.mock(WebSocketConnection.class);
        Mockito.when(mockWsConn2.id()).thenReturn("mock-test-conn-" + System.nanoTime() + 1);
        Player testPlayer2 = playerService.addPlayer(mockWsConn2);


        lobbyId = lobbyService.createLobby(testPlayer.getUniqueId());
        lobbyService.joinLobbyByCode(lobbyId, testPlayer2.getUniqueId());
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        lobby.setActivePlayer(testPlayer.getUniqueId());

        gameBoard = gameService.createGameboard(lobbyId);
    }

    @AfterEach
    void tearDown() {
        PlayerService.clearAllPlayersForTesting();
        if (lobbyService instanceof LobbyServiceImpl) {
            lobbyService.clearLobbies();
        }
        if (gameService != null) {
            gameService.clearGameBoardsForTesting();
        }
    }

    @Test
    void testRollDiceDistributesResourcesToPlayer() throws Exception {
        // 1. Select a target Tile and set its value for predictability
        Tile targetTile = gameBoard.getTileList().stream()
                .filter(t -> t.getType() == TileType.WHEAT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Test requires at least one WHEAT tile. Adjust board generation or test."));

        int mockDiceRollTotal = targetTile.getValue(); // We want the dice to roll this sum

        // 2. Find a SettlementPosition on this tile.
        SettlementPosition targetSettlementPosition = targetTile.getSettlementsOfTile().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Target tile (ID: " + targetTile.getId() + ", Type: " + targetTile.getType() +
                                ") has no associated settlement positions. Check GameBoard setup and SettlementPosition.addTile fix."));

        // 3. Place a Road and a Settlement
        PlayerColor playerColor = lobbyService.getPlayerColor(lobbyId, testPlayer.getUniqueId());
        assertNotNull(playerColor, "Player should have an assigned color");

        Road roadToPlace = targetSettlementPosition.getRoads().stream()
                .filter(r -> r.getOwner() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No available (unowned) road found for target settlement position " + targetSettlementPosition.getId()));
        gameService.placeRoad(lobbyId, testPlayer.getUniqueId(), roadToPlace.getId());

        gameService.placeSettlement(lobbyId, testPlayer.getUniqueId(), targetSettlementPosition.getId());

        // Verify settlement was placed and owned by the testPlayer
        assertNotNull(targetSettlementPosition.getBuildingOwner(), "Settlement should be placed on position " + targetSettlementPosition.getId());
        assertEquals(testPlayer.getUniqueId(), targetSettlementPosition.getBuildingOwner().getUniqueId(), "Settlement owner mismatch.");

        // 4. Configure DiceRoller for a predictable roll
        // Get the DiceRoller instance from the gameBoard using reflection to get predictable result
        Field diceRollerField = GameBoard.class.getDeclaredField("diceRoller");
        diceRollerField.setAccessible(true);
        DiceRoller actualDiceRoller = (DiceRoller) diceRollerField.get(gameBoard);

        // Mock the individual Dice objects within the DiceRoller
        Dice mockDice1 = Mockito.mock(Dice.class);
        Dice mockDice2 = Mockito.mock(Dice.class);

        // Determine dice values that sum to mockDiceRollTotal
        int d1Value = mockDiceRollTotal / 2;
        int d2Value = mockDiceRollTotal - d1Value;

        when(mockDice1.roll()).thenReturn(d1Value);
        when(mockDice2.roll()).thenReturn(d2Value);

        // Use reflection to replace the Dice instances in the actualDiceRoller
        Field d1FieldInRoller = DiceRoller.class.getDeclaredField("dice1");
        d1FieldInRoller.setAccessible(true);
        d1FieldInRoller.set(actualDiceRoller, mockDice1);

        Field d2FieldInRoller = DiceRoller.class.getDeclaredField("dice2");
        d2FieldInRoller.setAccessible(true);
        d2FieldInRoller.set(actualDiceRoller, mockDice2);

        // 5. Get initial resource count
        int initialWheatAmount = testPlayer.getResourceCount(TileType.WHEAT);

        // 6. Roll (mocked) dice
        System.out.printf("Test: Rolling dice. Expecting %d + %d = %d. Target Tile %d (type %s) is set to value %d.%n",
                d1Value, d2Value, mockDiceRollTotal, targetTile.getId(), targetTile.getType(), targetTile.getValue());
        System.out.printf("Test: Player %s has settlement at %d. Initial %s: %d%n",
                testPlayer.getUniqueId(), targetSettlementPosition.getId(), TileType.WHEAT, initialWheatAmount);

        gameService.rollDice(lobbyId);

        // 7. Assert that the player received the resource
        int finalWheatAmount = testPlayer.getResourceCount(TileType.WHEAT);
        assertEquals(initialWheatAmount + 1, finalWheatAmount,
                String.format("Player should have received 1 %s. Rolled %d (target tile value %d). Initial: %d, Final: %d",
                        TileType.WHEAT, mockDiceRollTotal, targetTile.getValue(), initialWheatAmount, finalWheatAmount));

        // 8. Test that a different roll does not give resources
        when(mockDice1.roll()).thenReturn(1);
        when(mockDice2.roll()).thenReturn(1);

        if (targetTile.getValue() == 2) {
            when(mockDice1.roll()).thenReturn(1);
            when(mockDice2.roll()).thenReturn(2);
        }

        gameService.rollDice(lobbyId);
        int wheatAmountAfterWrongRoll = testPlayer.getResourceCount(TileType.WHEAT);
        assertEquals(finalWheatAmount, wheatAmountAfterWrongRoll,
                "Player should not receive additional resources on a non-matching roll.");

        System.out.printf("Player has now %d Wheat%n", testPlayer.getResourceCount(TileType.WHEAT));
    }
}