package com.example.cataniaunited.game.trade;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.ports.GeneralPort;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.ports.SpecificResourcePort;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class TradingServiceTest {

    @InjectSpy
    TradingService tradingService;

    @InjectMock
    PlayerService playerService;

    private Player mockPlayer;
    private final String playerId = "testPlayer1";

    @BeforeEach
    void setUp() {
        mockPlayer = mock(Player.class);
        EnumMap<TileType, Integer> resources = new EnumMap<>(TileType.class);
        // Initialize all resources to 0 for a clean state
        for (TileType type : TileType.values()) {
            if (type != TileType.WASTE) {
                resources.put(type, 0);
            }
        }

        when(playerService.getPlayerById(playerId)).thenReturn(mockPlayer);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockPlayer.getResources()).thenReturn(resources);
        // Default to no ports unless specified in a test
        when(mockPlayer.getAccessiblePorts()).thenReturn(Collections.emptySet());
    }

    @Test
    void handleBankTradeRequest_PlayerNotFound_ThrowsException() {
        when(playerService.getPlayerById(playerId)).thenReturn(null);
        TradeRequest request = new TradeRequest(Map.of(TileType.WOOD, 4), Map.of(TileType.CLAY, 1));

        GameException exception = assertThrows(GameException.class,
                () -> tradingService.handleBankTradeRequest(playerId, request));
        assertEquals("Player not found, cannot process trade.", exception.getMessage());
    }

    @Test
    void testHasPlayerSufficientResources_True() {
        when(mockPlayer.getResources()).thenReturn(Map.of(TileType.WOOD, 5, TileType.CLAY, 3));
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 4, TileType.CLAY, 1);
        assertTrue(tradingService.hasPlayerSufficientResources(mockPlayer.getResources(), offered));
    }

    @Test
    void testHasPlayerSufficientResources_False() {
        when(mockPlayer.getResources()).thenReturn(Map.of(TileType.WOOD, 3, TileType.CLAY, 3)); // Only has 3 wood
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 4, TileType.CLAY, 1);
        assertFalse(tradingService.hasPlayerSufficientResources(mockPlayer.getResources(), offered));
    }

    @Test
    void testHasPlayerSufficientResources_OfferedEmpty_IsFalse() {
        assertFalse(tradingService.hasPlayerSufficientResources(mockPlayer.getResources(), Collections.emptyMap()));
    }

    @Test
    void testCheckIfCanTradeWithBank_Valid_4_to_1() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 4);
        Map<TileType, Integer> target = Map.of(TileType.CLAY, 1);
        assertTrue(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBank_Valid_8_to_2() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 8);
        Map<TileType, Integer> target = Map.of(TileType.CLAY, 2);
        assertTrue(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBank_Invalid_Mixed_Bundle() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 2, TileType.SHEEP, 2);
        Map<TileType, Integer> target = Map.of(TileType.CLAY, 1);
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBank_Invalid_Ratio() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 3);
        Map<TileType, Integer> target = Map.of(TileType.CLAY, 1);
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBank_InvalidRatio_WrongOutputCount() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 4);
        Map<TileType, Integer> target = Map.of(TileType.CLAY, 2);
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBank_EmptyOrNullMaps_ReturnsFalse() {
        Map<TileType, Integer> validMap = Map.of(TileType.WOOD, 4);
        assertFalse(tradingService.checkIfCanTradeWithBank(null, validMap));
        assertFalse(tradingService.checkIfCanTradeWithBank(validMap, null));
        assertFalse(tradingService.checkIfCanTradeWithBank(Collections.emptyMap(), validMap));
        assertFalse(tradingService.checkIfCanTradeWithBank(validMap, Collections.emptyMap()));
    }

    @Test
    void testCheckIfCanTradeWithBank_Invalid_SelfTrade() {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 4);
        Map<TileType, Integer> target = Map.of(TileType.WOOD, 1);
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testTradeResources_Successful() throws GameException {
        Map<TileType, Integer> offered = Map.of(TileType.WOOD, 4);
        Map<TileType, Integer> target = Map.of(TileType.CLAY, 1);

        tradingService.tradeResources(mockPlayer, offered, target);

        verify(mockPlayer, times(1)).removeResource(TileType.WOOD, 4);
        verify(mockPlayer, times(1)).receiveResource(TileType.CLAY, 1);
    }

    @Test
    void handleBankTradeRequest_Successful_BankTrade() throws GameException {
        // Player has 4 wood, wants 1 clay
        when(mockPlayer.getResources()).thenReturn(new EnumMap<>(Map.of(TileType.WOOD, 4, TileType.CLAY, 0)));
        TradeRequest request = new TradeRequest(Map.of(TileType.WOOD, 4), Map.of(TileType.CLAY, 1));

        tradingService.handleBankTradeRequest(playerId, request);

        verify(mockPlayer).removeResource(TileType.WOOD, 4);
        verify(mockPlayer).receiveResource(TileType.CLAY, 1);
    }

    @Test
    void handleBankTradeRequest_SuccessfulBankTrade_WithUnusedPort() throws GameException {
        // Player has a sheep port, but wants to trade wood 4:1
        when(mockPlayer.getResources()).thenReturn(new EnumMap<>(Map.of(TileType.WOOD, 4, TileType.ORE, 0)));
        Set<Port> ports = Set.of(new SpecificResourcePort(TileType.SHEEP)); // Port is irrelevant to the trade
        when(mockPlayer.getAccessiblePorts()).thenReturn(ports);

        TradeRequest request = new TradeRequest(Map.of(TileType.WOOD, 4), Map.of(TileType.ORE, 1));

        tradingService.handleBankTradeRequest(playerId, request);

        // Verify the trade was successful, meaning it passed the port check and used the bank logic
        verify(mockPlayer).removeResource(TileType.WOOD, 4);
        verify(mockPlayer).receiveResource(TileType.ORE, 1);
    }

    @Test
    void handleBankTradeRequest_Successful_GeneralPortTrade() throws GameException {
        // Player has 3 wood, wants 1 clay via 3:1 port
        when(mockPlayer.getResources()).thenReturn(new EnumMap<>(Map.of(TileType.WOOD, 3, TileType.CLAY, 0)));
        Set<Port> ports = Set.of(new GeneralPort());
        when(mockPlayer.getAccessiblePorts()).thenReturn(ports);
        TradeRequest request = new TradeRequest(Map.of(TileType.WOOD, 3), Map.of(TileType.CLAY, 1));

        tradingService.handleBankTradeRequest(playerId, request);

        verify(mockPlayer).removeResource(TileType.WOOD, 3);
        verify(mockPlayer).receiveResource(TileType.CLAY, 1);
    }

    @Test
    void handleBankTradeRequest_Successful_SpecificPortTrade() throws GameException {
        // Player has 2 wood, wants 1 clay via 2:1 wood port
        when(mockPlayer.getResources()).thenReturn(new EnumMap<>(Map.of(TileType.WOOD, 2, TileType.CLAY, 0)));
        Set<Port> ports = Set.of(new SpecificResourcePort(TileType.WOOD));
        when(mockPlayer.getAccessiblePorts()).thenReturn(ports);
        TradeRequest request = new TradeRequest(Map.of(TileType.WOOD, 2), Map.of(TileType.CLAY, 1));

        tradingService.handleBankTradeRequest(playerId, request);

        verify(mockPlayer).removeResource(TileType.WOOD, 2);
        verify(mockPlayer).receiveResource(TileType.CLAY, 1);
    }

    @Test
    void handleBankTradeRequest_InsufficientResources_ThrowsException() throws GameException {
        // Player has only 1 wood, tries to trade 4
        when(mockPlayer.getResources()).thenReturn(new EnumMap<>(Map.of(TileType.WOOD, 1)));
        TradeRequest request = new TradeRequest(Map.of(TileType.WOOD, 4), Map.of(TileType.CLAY, 1));

        GameException exception = assertThrows(GameException.class, () -> tradingService.handleBankTradeRequest(playerId, request));
        assertEquals("Insufficient Resources of Player", exception.getMessage());
        verify(mockPlayer, never()).removeResource(any(), anyInt());
    }

    @Test
    void handleBankTradeRequest_InvalidRatio_ThrowsException() throws GameException {
        // Player has enough wood, but tries an invalid 1:1 trade with no valid ports
        when(mockPlayer.getResources()).thenReturn(new EnumMap<>(Map.of(TileType.WOOD, 1)));
        TradeRequest request = new TradeRequest(Map.of(TileType.WOOD, 1), Map.of(TileType.CLAY, 1));

        GameException exception = assertThrows(GameException.class, () -> tradingService.handleBankTradeRequest(playerId, request));
        assertEquals("Trade ratio is invalid", exception.getMessage());
        verify(mockPlayer, never()).removeResource(any(), anyInt());
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("invalidPlayerTradeRequestSource")
    void verifyPlayerTradeRequestShouldFailForInvalidTrad(PlayerTradeRequest request) throws GameException {
        assertFalse(tradingService.verifyPlayerTradeRequest(request));
    }

    static Stream<Arguments> invalidPlayerTradeRequestSource() {
        return Stream.of(
                Arguments.of(new PlayerTradeRequest(null, null, null)),
                Arguments.of(new PlayerTradeRequest("id", null, null)),
                Arguments.of(new PlayerTradeRequest(null, "id", null)),
                Arguments.of(new PlayerTradeRequest("id", "id", null)),
                Arguments.of(new PlayerTradeRequest(null, "id", new TradeRequest(Map.of(), Map.of())))
        );
    }

    @Test
    void acceptPlayerTradeRequestShouldFailForWrongTargetPlayer() throws GameException {
        String sourcePlayerId = "sourcePlayerId";
        String targetPlayerId = "targetPlayerId";
        String tradeId = "l#test";
        PlayerTradeRequest request = new PlayerTradeRequest(targetPlayerId, sourcePlayerId, new TradeRequest(Map.of(), Map.of()));
        doReturn(request).when(tradingService).getPlayerTradeRequest(tradeId);

        GameException exception = assertThrows(GameException.class, () -> tradingService.acceptPlayerTradeRequest("invalidPlayer", tradeId));
        assertEquals("Not your trade request!", exception.getMessage());
    }

    @Test
    void rejectPlayerTradeRequestShouldFailForWrongTargetPlayer() throws GameException {
        String sourcePlayerId = "sourcePlayerId";
        String targetPlayerId = "targetPlayerId";
        String tradeId = "l#test";
        PlayerTradeRequest request = new PlayerTradeRequest(targetPlayerId, sourcePlayerId, new TradeRequest(Map.of(), Map.of()));
        doReturn(request).when(tradingService).getPlayerTradeRequest(tradeId);

        GameException exception = assertThrows(GameException.class, () -> tradingService.rejectPlayerTradeRequest("invalidPlayer", tradeId));
        assertEquals("Not your trade request!", exception.getMessage());
    }
}