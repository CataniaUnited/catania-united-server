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

        Map<TileType, Integer> offeredResources = tradeRequest.offeredResources();
        Map<TileType, Integer> targetResources = tradeRequest.targetResources();

        // Check if player has sufficient resources for the trade.
        if (!checkIfPlayerHasSufficientResources(player.getResources(), offeredResources)) {
            logger.errorf("Player %s has insufficient Resources for Trade. Has: %s, Offered: %s",
                    playerId, player.getResources(), offeredResources);
            throw new GameException("Insufficient Resources of Player");
        }

        // Check If Any Port approves this trade request
        Set<Port> ports = player.getAccessiblePorts();
        boolean canTrade = ports.stream().anyMatch(p -> p.canTrade(offeredResources, targetResources));

        // If no port trade was possible, check for standard 4:1 bank trade
        if (!canTrade) {
            canTrade = checkIfCanTradeWithBank(offeredResources, targetResources);
        }

        // If a valid trade path was found, execute it. Otherwise, throw an exception.
        if (!canTrade) {
            throw new GameException("Trade ratio is invalid");
        }

        tradeResources(player, offeredResources, targetResources);
    }

    /**
     * Checks if the player has the required resources based on a map of offered quantities.
     */
    boolean checkIfPlayerHasSufficientResources(Map<TileType, Integer> playerResources, Map<TileType, Integer> offeredResources) {
        if (Util.isEmpty(offeredResources)) {
            return false; // Cannot offer nothing.
        }

        for (Map.Entry<TileType, Integer> offer : offeredResources.entrySet()) {
            TileType resourceToOffer = offer.getKey();
            int quantityOffered = offer.getValue();
            int playerHasQuantity = playerResources.getOrDefault(resourceToOffer, 0);

            if (playerHasQuantity < quantityOffered) {
                logger.infof("Insufficient resources for %s. Player has: %d, Offered: %d", resourceToOffer, playerHasQuantity, quantityOffered);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a standard 4:1 bank trade is valid using maps.
     */
    boolean checkIfCanTradeWithBank(Map<TileType, Integer> offeredResources, Map<TileType, Integer> targetResources) {
        if (Util.isEmpty(offeredResources) || Util.isEmpty(targetResources)) {
            logger.infof("Offered or target resources is empty.");
            return false;
        }

        // Ensure the player is not trying to trade for a resource they are also offering.
        if (!Collections.disjoint(offeredResources.keySet(), targetResources.keySet())) {
            logger.infof("Cannot trade for a resource that is also being offered.");
            return false;
        }

        // For a standard bank trade, a player must offer a single type of resource.
        if (offeredResources.size() != 1) {
            logger.info("Bank trade bundle must be of a single resource type.");
            return false;
        }

        // Get the single offered resource type and its amount.
        Map.Entry<TileType, Integer> offer = offeredResources.entrySet().iterator().next();
        int offeredAmount = offer.getValue();
        int targetAmount = targetResources.values().stream().mapToInt(Integer::intValue).sum();

        // Check if the amounts conform to the 4:1 ratio.
        if (offeredAmount % STANDARD_TRADE_RATIO != 0 || (offeredAmount / STANDARD_TRADE_RATIO) != targetAmount) {
            logger.infof("Trade Ratio is Invalid. Offered: %d, Target: %d, Ratio: %d:1", offeredAmount, targetAmount, STANDARD_TRADE_RATIO);
            return false;
        }

        return true;
    }

    /**
     * Executes the trade by removing/adding resources based on maps.
     */
    void tradeResources(Player player, Map<TileType, Integer> offeredResources, Map<TileType, Integer> targetResources) throws GameException {
        // Add all target resources
        for (Map.Entry<TileType, Integer> entry : targetResources.entrySet()) {
            player.receiveResource(entry.getKey(), entry.getValue());
        }

        // Remove all offered resources
        for (Map.Entry<TileType, Integer> entry : offeredResources.entrySet()) {
            player.removeResource(entry.getKey(), entry.getValue());
        }
    }
}