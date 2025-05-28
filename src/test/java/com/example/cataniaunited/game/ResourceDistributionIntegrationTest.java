package com.example.cataniaunited.game;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.game.board.Road;
import com.example.cataniaunited.game.board.BuildingSite;
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
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceDistributionIntegrationTest {

    @Inject
    GameService gameService;

    @InjectSpy
    LobbyService lobbyService;

    @Inject
    PlayerService playerService;

    private Player testPlayer;
    private String lobbyId;
    private GameBoard gameBoard;
    private Tile targetTile;
    private BuildingSite targetBuildingSite;
    private int wheatAmountBeforeTargetRoll;
    private int wheatAmountAfterTargetRoll;
    private int mockDiceRollTotal;
    private int d1Value;
    private int d2Value;
    private Dice mockDice1;
    private Dice mockDice2;

    @BeforeAll
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
        doNothing().when(lobbyService).checkPlayerDiceRoll(lobbyId, testPlayer.getUniqueId());

        gameBoard = gameService.createGameboard(lobbyId);
    }

    @AfterAll
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
    @Order(1)
    void selectTargetTile() {
        targetTile = gameBoard.getTileList().stream()
                .filter(t -> t.getType() == TileType.WHEAT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Test requires at least one WHEAT tile. Adjust board generation or test."));

        mockDiceRollTotal = targetTile.getValue(); // We want the dice to roll this sum
    }

    @Test
    @Order(2)
    void findSettlementPositionOfTile(){
        targetBuildingSite = targetTile.getBuildingSitesOfTile().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Target tile (ID: " + targetTile.getId() + ", Type: " + targetTile.getType() +
                                ") has no associated settlement positions. Check GameBoard setup and BuildingSite.addTile fix."));
    }

    @Test
    @Order(3)
    void placeRoadAndSettlement() throws GameException {
        PlayerColor playerColor = lobbyService.getPlayerColor(lobbyId, testPlayer.getUniqueId());
        assertNotNull(playerColor, "Player should have an assigned color");

        Road roadToPlace = targetBuildingSite.getRoads().stream()
                .filter(r -> r.getOwner() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No available (unowned) road found for target settlement position " + targetBuildingSite.getId()));
        gameService.placeRoad(lobbyId, testPlayer.getUniqueId(), roadToPlace.getId());

        gameService.placeSettlement(lobbyId, testPlayer.getUniqueId(), targetBuildingSite.getId());

        // Verify settlement was placed and owned by the testPlayer
        assertNotNull(targetBuildingSite.getBuildingOwner(), "Settlement should be placed on position " + targetBuildingSite.getId());
        assertEquals(testPlayer.getUniqueId(), targetBuildingSite.getBuildingOwner().getUniqueId(), "Settlement owner mismatch.");
    }

    @Test
    @Order(4)
    void configureDiceRollerToRollDesiredValue() throws NoSuchFieldException, IllegalAccessException {
        // Get the DiceRoller instance from the gameBoard using reflection to get predictable result
        Field diceRollerField = GameBoard.class.getDeclaredField("diceRoller");
        diceRollerField.setAccessible(true);
        DiceRoller actualDiceRoller = (DiceRoller) diceRollerField.get(gameBoard);

        // Mock the individual Dice objects within the DiceRoller
        mockDice1 = Mockito.mock(Dice.class);
        mockDice2 = Mockito.mock(Dice.class);

        // Determine dice values that sum to mockDiceRollTotal
        d1Value = mockDiceRollTotal / 2;
        d2Value = mockDiceRollTotal - d1Value;

        when(mockDice1.roll()).thenReturn(d1Value);
        when(mockDice2.roll()).thenReturn(d2Value);

        // Use reflection to replace the Dice instances in the actualDiceRoller
        Field d1FieldInRoller = DiceRoller.class.getDeclaredField("dice1");
        d1FieldInRoller.setAccessible(true);
        d1FieldInRoller.set(actualDiceRoller, mockDice1);

        Field d2FieldInRoller = DiceRoller.class.getDeclaredField("dice2");
        d2FieldInRoller.setAccessible(true);
        d2FieldInRoller.set(actualDiceRoller, mockDice2);
    }

    @Test
    @Order(5)
    void getWheatAmountBeforeTargetRoll(){
        wheatAmountBeforeTargetRoll = testPlayer.getResourceCount(TileType.WHEAT);
    }

    @Test
    @Order(6)
    void rollMockedDice() throws GameException {
        System.out.printf("Test: Rolling dice. Expecting %d + %d = %d. Target Tile %d (type %s) is set to value %d.%n",
                d1Value, d2Value, mockDiceRollTotal, targetTile.getId(), targetTile.getType(), targetTile.getValue());
        System.out.printf("Test: Player %s has settlement at %d. Initial %s: %d%n",
                testPlayer.getUniqueId(), targetBuildingSite.getId(), TileType.WHEAT, wheatAmountBeforeTargetRoll);

        gameService.rollDice(lobbyId, testPlayer.getUniqueId());
    }
    
    @Test
    @Order(7)
    void assertThatPlayerRecivedRessources(){
        wheatAmountAfterTargetRoll = testPlayer.getResourceCount(TileType.WHEAT);
        assertEquals(wheatAmountBeforeTargetRoll + 1, wheatAmountAfterTargetRoll,
                String.format("Player should have received 1 %s. Rolled %d (target tile value %d). Initial: %d, Final: %d",
                        TileType.WHEAT, mockDiceRollTotal, targetTile.getValue(), wheatAmountBeforeTargetRoll, wheatAmountAfterTargetRoll));
    }
    
    @Test
    @Order(8)
    void differentRollsDoNotDistributeResources() throws GameException {
        when(mockDice1.roll()).thenReturn(1);
        when(mockDice2.roll()).thenReturn(1);

        if (targetTile.getValue() == 2) {
            when(mockDice1.roll()).thenReturn(1);
            when(mockDice2.roll()).thenReturn(2);
        }

        gameService.rollDice(lobbyId, testPlayer.getUniqueId());
        int wheatAmountAfterWrongRoll = testPlayer.getResourceCount(TileType.WHEAT);
        assertEquals(wheatAmountAfterTargetRoll, wheatAmountAfterWrongRoll,
                "Player should not receive additional resources on a non-matching roll.");

        System.out.printf("Player has now %d Wheat%n", testPlayer.getResourceCount(TileType.WHEAT));
    }
}