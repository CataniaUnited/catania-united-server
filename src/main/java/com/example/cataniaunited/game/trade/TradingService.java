package com.example.cataniaunited.game.trade;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.example.cataniaunited.util.Util;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class TradingService {

    private static final int STANDARD_TRADE_RATIO = 4;

    @Inject
    PlayerService playerService;
    private static final Logger logger = Logger.getLogger(TradingService.class);
    /**
     * Handles a bank trade request using a clean TradeRequest object.
     *
     * @param playerId     The ID of the player making the trade.
     * @param tradeRequest The TradeRequest object containing offered and target resources.
     * @throws GameException if the player is not found, has insufficient resources, or the trade is invalid.
     */
    public void handleBankTradeRequest(String playerId, TradeRequest tradeRequest) throws GameException {
        Player player = playerService.getPlayerById(playerId);
        if (player == null) {
            logger.errorf("Player requesting trade with PlayerId %s not found", playerId);
            throw new GameException("Player not found, cannot process trade.");
        }

        List<TileType> offeredResources = tradeRequest.offeredResources();
        List<TileType> targetResources = tradeRequest.targetResources();

        // Check if player has sufficient resources as in offeredResources
        if (!checkIfPlayerHasSufficientResources(player.getResources(), offeredResources)) {
            logger.errorf("Player %s has insufficient Resources for Trade. Has: %s, Offered: %s",
                    playerId, player.getResources(), offeredResources.toString());
            throw new GameException("Insufficient Resources of Player");
        }

        // Check If Any Port approves this trade request
        boolean canTrade = false;
        Set<Port> ports = player.getAccessiblePorts();
        for (Port port: ports){
            if (port.canTrade(offeredResources, targetResources)){
                // Port Accepts Trade
                canTrade = true;
                break;
            }
        }

        // Else check if Normal Trade is possible
        if (!canTrade){
            canTrade = checkIfCanTradeWithBank(offeredResources, targetResources);
        }

        // If yes trade, else throw exception
        if (!canTrade){
            throw new GameException("Trade Ration is invalid");
        }

        tradeResources(player, offeredResources, targetResources);
    }

     boolean checkIfPlayerHasSufficientResources(Map<TileType, Integer> playerResources, List<TileType> offeredResources){
         // 1. Count the occurrences of each TileType in the offeredResources list
         EnumMap<TileType, Integer> offeredResourceMap = new EnumMap<>(TileType.class);
         for (TileType resource : offeredResources) {
             offeredResourceMap.put(resource, offeredResourceMap.getOrDefault(resource, 0) + 1);
         }

         // 2. Iterate through the "offered quantities" map
         for (Map.Entry<TileType, Integer> entry : offeredResourceMap.entrySet()) {
             TileType resourceToOffer = entry.getKey();
             int quantityOffered = entry.getValue();

             // 3. Check if the playerResources map contains at least that quantity
             int playerHasQuantity = playerResources.getOrDefault(resourceToOffer, 0);

             if (playerHasQuantity < quantityOffered) {
                 // Player does not have enough of this specific resource
                 logger.infof("Insufficient resources for %s. Player has: %d, Offered: %d", resourceToOffer, playerHasQuantity, quantityOffered);
                 return false;
             }
         }

         // 4. If all checks pass, the player has sufficient resources
         return true;
     }

     boolean checkIfCanTradeWithBank(List<TileType> offeredResources, List<TileType> targetResources){
         if (Util.isEmpty(offeredResources) || Util.isEmpty(targetResources)) {
             logger.infof("offered or target resources is empty (Offered: %s; Target: %s)", offeredResources, targetResources);
             return false;
         }

         if (offeredResources.size() % STANDARD_TRADE_RATIO != 0 || (offeredResources.size() / STANDARD_TRADE_RATIO) != targetResources.size()) {
             logger.infof("Trade Ratio is Invalid (Offered: %s; count %d, ratio: %d)", offeredResources, offeredResources.size(), STANDARD_TRADE_RATIO);
             return false;
         }

         for (TileType desired : targetResources) {
             if (offeredResources.contains(desired)) {
                 logger.infof("Cant trade for Resource that's offered (Offered: %s; Target: %s)", offeredResources, targetResources);
                 return false;
             }
         }

         Map<TileType, Long> offeredResourceMap = offeredResources.stream()
                 .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

         // For each type of resource offered, check if its count is a multiple of the port's inputResourceAmount
         for (Map.Entry<TileType, Long> entry : offeredResourceMap.entrySet()) {
             long countOfEntryType = entry.getValue();
             if (countOfEntryType % STANDARD_TRADE_RATIO != 0) {
                 logger.info("Bank trade bundle not uniform.");
                 return false;
             }
         }

         // can Trade with Bank
         return true;
     }

     void tradeResources(Player player, List<TileType> offeredResources, List<TileType> targetResources) throws GameException {
        for (TileType resource: targetResources){
            player.receiveResource(resource, 1);
        }

         for (TileType resource: offeredResources){
             player.removeResource(resource, 1);
         }
     }
}
