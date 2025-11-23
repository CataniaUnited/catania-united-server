package com.example.cataniaunited.game.trade;

public record PlayerTradeRequest(
        String targetPlayerId,
        String sourcePlayerId,
        TradeRequest trade
) {
}
