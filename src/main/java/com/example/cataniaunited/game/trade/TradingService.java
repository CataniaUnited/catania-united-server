package com.example.cataniaunited.game.trade;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerService;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class TradingService {

    private final int STANDARD_TRADE_RATIO = 4;

    @Inject
    PlayerService playerService;
    private static final Logger logger = Logger.getLogger(TradingService.class);
    public Uni<MessageDTO> handleBankTradeRequest(MessageDTO message) throws GameException{
        checkTradeRequestJson(message);
        Player player = playerService.getPlayerById(message.getPlayer());

        // Extract offeredResources List and desiredResources List (List<TileType>)
        List<TileType> offeredResources = extractListOutOfArrayNode(message.getMessage(), "offeredResources");
        List<TileType> targetResources = extractListOutOfArrayNode(message.getMessage(), "targetResources");

        // Check if player has sufficient resources as in offeredResources
        if (!checkIfPlayerHasSufficientResources(player.getResources(), offeredResources)) {
            logger.errorf("Player has insufficient Resources for Trade %s, %s", player.getResources(), offeredResources.toString());
            throw new GameException("Insufficient Resources of Player");
        }

        // Check If Any Port approves this trade request
        boolean canTrade = false;
        Set<Port> ports = player.getAccessiblePorts();
        for (Port port: ports){
            if (port.canTrade(offeredResources, targetResources)){
                // Port Accepts Trade
                canTrade = true;
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

        return null;
    }

    void checkTradeRequestJson(MessageDTO message) throws GameException {
        String playerId = message.getPlayer();
        Player player = playerService.getPlayerById(playerId);

        if (player == null){
            logger.errorf("Player requesting Trade wit PlayerId %s not found", playerId);
            throw new GameException("Player not found");
        }
        ObjectNode requestMessage = message.getMessage();

        JsonNode target = requestMessage.get("target");
        JsonNode offeredResources = requestMessage.get("offeredResources");
        JsonNode targetResources = requestMessage.get("targetResources");

        if (offeredResources == null || targetResources == null){
            logger.errorf("JSON of Trade Request is not complete. target: %s; offeredResources: %s; targetResources: %s", target, offeredResources, targetResources);
            throw new GameException("Invalid trade Request");
        }
    }

     List<TileType> extractListOutOfArrayNode(ObjectNode jsonMessage, String nameOfArrayNode) throws GameException {
        JsonNode node = jsonMessage.get(nameOfArrayNode);

        if (!node.isArray()) {
            logger.errorf("JSON of Trade Request is not correct. %s is not an arrayNode but a %s", nameOfArrayNode, node.getNodeType());
            throw new GameException("Invalid trade Request");
        }

         List<TileType> extractedTileTypeList = new ArrayList<>();
         ArrayNode arrayNode = (ArrayNode) node;

         for (JsonNode elementNode : arrayNode) {
             String tileTypeName = elementNode.asText();
             try {
                 // TileType enum constants are in UPPERCASE.
                 TileType tileType = TileType.valueOf(tileTypeName.toUpperCase());
                 extractedTileTypeList.add(tileType);
             } catch (IllegalArgumentException e) {
                 logger.errorf("Invalid resource type string '%s' in array '%s'.", tileTypeName, nameOfArrayNode);
                 throw new GameException("Invalid trade request: Unknown resource type '%s'.", tileTypeName);
             }
         }
         return extractedTileTypeList;
     }

     boolean checkIfPlayerHasSufficientResources(Map<TileType, Integer> playerResources, List<TileType> offeredResources){
         // 1. Count the occurrences of each TileType in the offeredResources list
         Map<TileType, Integer> offeredResourceMap = new HashMap<>();
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
             logger.errorf("offered or target resources is empty (Offered: %s; Target: %s)", offeredResources, targetResources);
             return false;
         }

         if (offeredResources.size() % STANDARD_TRADE_RATIO != 0 || (offeredResources.size() / STANDARD_TRADE_RATIO) != targetResources.size()) {
             logger.errorf("Trade Ratio is Invalid (Offered: %s; count %d, ratio: %d)", offeredResources, offeredResources.size(), STANDARD_TRADE_RATIO);
             return false;
         }

         for (TileType desired : targetResources) {
             if (offeredResources.contains(desired)) {
                 logger.errorf("Cant trade for Resource that's offered (Offered: %s; Target: %s)", offeredResources, targetResources);
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
