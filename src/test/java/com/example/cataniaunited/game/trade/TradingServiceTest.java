package com.example.cataniaunited.game.trade;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.ports.GeneralPort;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.ports.SpecificResourcePort;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@QuarkusTest
class TradingServiceTest {

    @Inject
    TradingService tradingService;

    @InjectMock
    PlayerService playerService;

    private Player mockPlayer;
    private final String playerId = "testPlayer1";
    private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

    @BeforeEach
    void setUp() {
        mockPlayer = mock(Player.class);
        EnumMap<TileType, Integer> resources = new EnumMap<>(TileType.class);
        for (TileType type : TileType.values()) {
            if (type != TileType.WASTE) {
                resources.put(type, 0);
            }
        }

        when(playerService.getPlayerById(playerId)).thenReturn(mockPlayer);
        when(mockPlayer.getResources()).thenReturn(resources);
        when(mockPlayer.getAccessiblePorts()).thenReturn(new HashSet<>());
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
    }


    private ObjectNode createTradeMessagePayload(List<String> offered, List<String> target) {
        ObjectNode payload = nodeFactory.objectNode();
        ArrayNode offeredArray = nodeFactory.arrayNode();
        if (offered != null) {
            offered.forEach(offeredArray::add);
            payload.set("offeredResources", offeredArray);
        }
        ArrayNode targetArray = nodeFactory.arrayNode();
        if (target != null) {
            target.forEach(targetArray::add);
            payload.set("targetResources", targetArray);
        }
        payload.put("target", "bank");
        return payload;
    }

    private MessageDTO createMessageDTO(ObjectNode payload) {
        MessageDTO message = new MessageDTO(MessageType.TRADE_WITH_BANK, payload);
        message.setPlayer(playerId);
        return message;
    }

    @Test
    void testCheckIfPlayerHasSufficientResourcesTrue() {
        Map<TileType, Integer> playerRes = new EnumMap<>(Map.of(TileType.WOOD, 5, TileType.CLAY, 3));
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.CLAY);
        assertTrue(tradingService.checkIfPlayerHasSufficientResources(playerRes, offered));
    }

    @Test
    void testCheckIfPlayerHasSufficientResourcesFalseOneType() {
        Map<TileType, Integer> playerRes = new EnumMap<>(Map.of(TileType.WOOD, 1, TileType.CLAY, 3));
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.CLAY);
        assertFalse(tradingService.checkIfPlayerHasSufficientResources(playerRes, offered));
    }

    @Test
    void testCheckIfPlayerHasSufficientResourcesFalseMultipleTypes() {
        Map<TileType, Integer> playerRes = new EnumMap<>(Map.of(TileType.WOOD, 1, TileType.CLAY, 0));
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.CLAY);
        assertFalse(tradingService.checkIfPlayerHasSufficientResources(playerRes, offered));
    }

    @Test
    void testCheckIfPlayerHasSufficientResourcesOfferedEmpty() {
        Map<TileType, Integer> playerRes = new EnumMap<>(Map.of(TileType.WOOD, 5));
        List<TileType> offered = List.of();
        assertTrue(tradingService.checkIfPlayerHasSufficientResources(playerRes, offered));
    }

    @Test
    void testCheckIfPlayerHasSufficientResourcesPlayerHasNoneOfOffered() {
        Map<TileType, Integer> playerRes = new EnumMap<>(Map.of(TileType.SHEEP, 5));
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD);
        assertFalse(tradingService.checkIfPlayerHasSufficientResources(playerRes, offered));
    }

    @Test
    void testCheckIfCanTradeWithBankValid4for1() {
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> target = List.of(TileType.CLAY);
        assertTrue(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBankValid8for2() {
        List<TileType> offered = List.of(
                TileType.WOOD, TileType.WOOD, TileType.SHEEP, TileType.SHEEP,
                TileType.WOOD, TileType.WOOD, TileType.SHEEP, TileType.SHEEP
        );
        List<TileType> target = List.of(TileType.CLAY, TileType.ORE);
        assertTrue(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBankInvalidRatioNotMultiple() {
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD); // 3 offered
        List<TileType> target = List.of(TileType.CLAY); // 1 target
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBankInvalidRatioWrongOutputCount() {
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD); // 4 offered
        List<TileType> target = List.of(TileType.CLAY, TileType.ORE); // 2 target
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBankInvalidSelfTrade() {
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> target = List.of(TileType.WOOD); // Trading wood for wood
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBankInvalidMixedBundle() {
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.CLAY, TileType.CLAY); // Mixed bundle
        List<TileType> target = List.of(TileType.SHEEP);
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBankInvalidMixedBundleForSecondTarget() {
        List<TileType> offered = List.of(
                TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD, // Valid bundle 1
                TileType.SHEEP, TileType.SHEEP, TileType.CLAY, TileType.CLAY  // Invalid bundle 2
        );
        List<TileType> target = List.of(TileType.ORE, TileType.WHEAT);
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBankOfferedEmpty() {
        List<TileType> offered = List.of();
        List<TileType> target = List.of(TileType.CLAY);
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }

    @Test
    void testCheckIfCanTradeWithBankTargetEmpty() {
        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> target = List.of();
        assertFalse(tradingService.checkIfCanTradeWithBank(offered, target));
    }


    @Test
    void testTradeResourcesSuccessful() throws GameException {
        mockPlayer.getResources().put(TileType.WOOD, 4);
        mockPlayer.getResources().put(TileType.CLAY, 0);

        List<TileType> offered = List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD);
        List<TileType> target = List.of(TileType.CLAY);

        tradingService.tradeResources(mockPlayer, offered, target);

        verify(mockPlayer, times(4)).removeResource(TileType.WOOD, 1);
        verify(mockPlayer, times(1)).receiveResource(TileType.CLAY, 1);

    }

    @Test
    void testHandleBankTradeRequestSuccessfulBankTrade() throws GameException {
        mockPlayer.getResources().put(TileType.WOOD, 4);
        mockPlayer.getResources().put(TileType.CLAY, 0);
        TradeRequest tradeRequest = new TradeRequest(
                List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD),
                List.of(TileType.CLAY)
        );

        assertDoesNotThrow(() -> tradingService.handleBankTradeRequest(mockPlayer.getUniqueId(), tradeRequest));

        verify(mockPlayer, times(4)).removeResource(TileType.WOOD, 1);
        verify(mockPlayer, times(1)).receiveResource(TileType.CLAY, 1);
    }

    @Test
    void testHandleBankTradeRequestSuccessfulGeneralPortTrade() throws GameException {
        mockPlayer.getResources().put(TileType.WOOD, 3);
        mockPlayer.getResources().put(TileType.CLAY, 0);

        GeneralPort generalPort = Mockito.spy(new GeneralPort()); // 3:1 port
        Set<Port> ports = new HashSet<>();
        ports.add(generalPort);
        Mockito.doReturn(ports).when(mockPlayer).getAccessiblePorts();

        TradeRequest tradeRequest = new TradeRequest(
                List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD),
                List.of(TileType.CLAY)
        );

        assertDoesNotThrow(() -> tradingService.handleBankTradeRequest(mockPlayer.getUniqueId(), tradeRequest));
        verify(mockPlayer, times(3)).removeResource(TileType.WOOD, 1);
        verify(mockPlayer, times(1)).receiveResource(TileType.CLAY, 1);
    }

    @Test
    void testHandleBankTradeRequestSuccessfulSpecificPortTrade() throws GameException {
        mockPlayer.getResources().put(TileType.WOOD, 2);
        mockPlayer.getResources().put(TileType.CLAY, 0);

        SpecificResourcePort woodPort = Mockito.spy(new SpecificResourcePort(TileType.WOOD)); // 2:1 Wood port
        Set<Port> ports = new HashSet<>();
        ports.add(woodPort);
        Mockito.doReturn(ports).when(mockPlayer).getAccessiblePorts();


        TradeRequest tradeRequest = new TradeRequest(
                List.of(TileType.WOOD, TileType.WOOD),
                List.of(TileType.CLAY)
        );

        assertDoesNotThrow(() -> tradingService.handleBankTradeRequest(mockPlayer.getUniqueId(), tradeRequest));

        verify(mockPlayer, times(2)).removeResource(TileType.WOOD, 1);
        verify(mockPlayer, times(1)).receiveResource(TileType.CLAY, 1);
    }

    @Test
    void testHandleBankTradeRequestInsufficientPlayerResources() throws GameException {
        mockPlayer.getResources().put(TileType.WOOD, 1);
        TradeRequest tradeRequest = new TradeRequest(
                List.of(TileType.WOOD, TileType.WOOD, TileType.WOOD, TileType.WOOD),
                List.of(TileType.CLAY)
        );

        GameException exception = assertThrows(GameException.class,
                () ->tradingService.handleBankTradeRequest(mockPlayer.getUniqueId(), tradeRequest));
        assertEquals("Insufficient Resources of Player", exception.getMessage());
        verify(mockPlayer, never()).removeResource(any(TileType.class), any(Integer.class));
    }


    @Test
    void testHandleBankTradeRequestTradeRatioInvalidNeitherPortNorBank() throws GameException {
        mockPlayer.getResources().put(TileType.WOOD, 5);

        SpecificResourcePort woodPort = Mockito.spy(new SpecificResourcePort(TileType.WOOD)); // 2:1 Wood port
        Set<Port> ports = new HashSet<>();
        ports.add(woodPort);
        Mockito.doReturn(ports).when(mockPlayer).getAccessiblePorts();


        TradeRequest tradeRequest = new TradeRequest(
                List.of(TileType.WOOD),
                List.of(TileType.CLAY)
        );

        GameException exception = assertThrows(GameException.class,
                () ->tradingService.handleBankTradeRequest(mockPlayer.getUniqueId(), tradeRequest));
        assertEquals("Trade Ration is invalid", exception.getMessage());
        verify(mockPlayer, never()).removeResource(any(TileType.class), any(Integer.class));
    }
}