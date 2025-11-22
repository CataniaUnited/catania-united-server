package com.example.cataniaunited.game.trade;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.cataniaunited.util.Util.isEmpty;

@ApplicationScoped
public class TradingService {

    private static final int STANDARD_TRADE_RATIO = 4;

    @Inject
    PlayerService playerService;

    private ConcurrentHashMap<String, PlayerTradeRequest> openTradeRequests = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(TradingService.class);

    /**
     * Handles a bank trade request using a clean TradeRequest object.
     *
     * @param playerId     The ID of the player making the trade.
     * @param tradeRequest The TradeRequest object containing offered and target resources.
     * @throws GameException if the player is not found, has insufficient resources, or the trade is invalid.
     */
    public void handleBankTradeRequest(String playerId, TradeRequest tradeRequest) throws GameException {
        Player player = getPlayerForTrade(playerId);

        Map<TileType, Integer> offeredResources = tradeRequest.offeredResources();
        Map<TileType, Integer> targetResources = tradeRequest.targetResources();

        // Check if player has sufficient resources for the trade.
        checkIfPlayerHasSufficientResources(player, offeredResources);

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

    public String createPlayerTradeRequest(String lobbyId, PlayerTradeRequest tradeRequest) throws GameException {
        logger.debugf("Creating player trade request: lobbyId = %s, tradeRequest = %s", lobbyId, tradeRequest);
        checkPlayerTradeRequest(tradeRequest);
        String tradeId = createTradeIdentifier(lobbyId);
        this.openTradeRequests.put(tradeId, tradeRequest);
        logger.debugf("Trade request created: tradeId = %s, tradeRequest = %s", tradeId, tradeRequest);
        return tradeId;
    }

    public boolean verifyPlayerTradeRequest(PlayerTradeRequest tradeRequest) {
        return tradeRequest != null
                && !isEmpty(tradeRequest.sourcePlayerId())
                && !isEmpty(tradeRequest.targetPlayerId())
                && tradeRequest.trade() != null;
    }

    public PlayerTradeRequest getPlayerTradeRequest(String tradeId) throws GameException {
        PlayerTradeRequest playerTradeRequest = this.openTradeRequests.get(tradeId);
        if (playerTradeRequest == null) {
            logger.errorf("Player trade request not found: tradeId = %s", tradeId);
            throw new GameException("Trade request not found!");
        }
        return playerTradeRequest;
    }

    public PlayerTradeRequest acceptPlayerTradeRequest(String playerId, String tradeId) throws GameException {
        PlayerTradeRequest playerTradeRequest = getPlayerTradeRequest(tradeId);

        if (!Objects.equals(playerId, playerTradeRequest.targetPlayerId())) {
            logger.errorf("Player trade request doesn't match target player: tradeId = %s, playerId = %s, tradeRequest = %s", tradeId, playerId, playerTradeRequest);
            throw new GameException("Not your trade request!");
        }

        checkPlayerTradeRequest(playerTradeRequest);

        Player sourcePlayer = getPlayerForTrade(playerTradeRequest.sourcePlayerId());
        Player targetPlayer = getPlayerForTrade(playerTradeRequest.targetPlayerId());
        TradeRequest tradeRequest = playerTradeRequest.trade();

        logger.debugf("Player %s accepted trade request, performing trade: tradeRequest = %s", playerId, playerTradeRequest);
        tradeResources(sourcePlayer, tradeRequest.offeredResources(), tradeRequest.targetResources());
        tradeResources(targetPlayer, tradeRequest.targetResources(), tradeRequest.offeredResources());
        removeTradeRequest(tradeId);
        return playerTradeRequest;
    }

    public PlayerTradeRequest rejectPlayerTradeRequest(String playerId, String tradeId) throws GameException {
        PlayerTradeRequest playerTradeRequest = getPlayerTradeRequest(tradeId);

        if (!Objects.equals(playerId, playerTradeRequest.targetPlayerId())) {
            logger.errorf("Player trade request doesn't match target player: tradeId = %s, playerId = %s, tradeRequest = %s", tradeId, playerId, playerTradeRequest);
            throw new GameException("Not your trade request!");
        }
        logger.debugf("Player %s rejected trade request, removing trade: tradeRequest = %s", playerId, playerTradeRequest);
        removeTradeRequest(tradeId);
        return playerTradeRequest;
    }

    String createTradeIdentifier(String lobbyId) {
        return "%s#%s".formatted(lobbyId, UUID.randomUUID());
    }

    void checkPlayerTradeRequest(PlayerTradeRequest request) throws GameException {
        Player sourcePlayer = getPlayerForTrade(request.sourcePlayerId());
        Player targetPlayer = getPlayerForTrade(request.targetPlayerId());

        TradeRequest tradeRequest = request.trade();
        Map<TileType, Integer> offeredResources = tradeRequest.offeredResources();
        Map<TileType, Integer> targetResources = tradeRequest.targetResources();

        checkIfPlayerHasSufficientResources(sourcePlayer, offeredResources);
        checkIfPlayerHasSufficientResources(targetPlayer, targetResources);
    }

    Player getPlayerForTrade(String playerId) throws GameException {
        Player player = playerService.getPlayerById(playerId);
        if (player == null) {
            logger.errorf("Player requesting trade with PlayerId %s not found", playerId);
            throw new GameException("Player not found, cannot process trade.");
        }
        return player;
    }

    void checkIfPlayerHasSufficientResources(Player player, Map<TileType, Integer> offeredResources) throws GameException {
        if (!hasPlayerSufficientResources(player.getResources(), offeredResources)) {
            logger.errorf("Player %s has insufficient Resources for Trade. Has: %s, Offered: %s",
                    player.getUniqueId(), player.getResources(), offeredResources);
            throw new GameException("Insufficient Resources of Player");
        }
    }

    /**
     * Checks if the player has the required resources based on a map of offered quantities.
     */
    boolean hasPlayerSufficientResources(Map<TileType, Integer> playerResources, Map<TileType, Integer> offeredResources) {
        if (isEmpty(offeredResources)) {
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
        if (isEmpty(offeredResources) || isEmpty(targetResources)) {
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

    void removeTradeRequest(String tradeId) {
        logger.debugf("Removing trade request: id = %s", tradeId);
        this.openTradeRequests.remove(tradeId);
    }

    public void removeAllOpenTradeRequestForLobbyId(String lobbyId) {
        logger.debugf("Removing all trade requests for lobby: lobbyId = %s", lobbyId);
        var keys = this.openTradeRequests.keySet();
        if (!isEmpty(keys)) {
            keys.stream()
                    .filter(tradeId -> tradeId.startsWith(lobbyId))
                    .forEach(this::removeTradeRequest);
        }
    }
}