package com.example.cataniaunited.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevelopmentCardDeck {

    private final List<DevelopmentCardType> deck = new ArrayList<>();

    public DevelopmentCardDeck() {
        // Adds 14 KNIGHT cards
        for (int i = 0; i < 14; i++) {
            deck.add(DevelopmentCardType.KNIGHT);
        }
        // Adds 2 ROAD_BUILDING cards
        for (int i = 0; i < 2; i++) {
            deck.add(DevelopmentCardType.ROAD_BUILDING);
        }
        // Adds 2 YEAR_OF_PLENTY cards
        for (int i = 0; i < 2; i++) {
            deck.add(DevelopmentCardType.YEAR_OF_PLENTY);
        }
        // Adds 2 MONOPOLY cards
        for (int i = 0; i < 2; i++) {
            deck.add(DevelopmentCardType.MONOPOLY);
        }
        // Adds 5 VICTORY_POINT cards
        for (int i = 0; i < 5; i++) {
            deck.add(DevelopmentCardType.VICTORY_POINT);
        }

        // Shuffle the deck
        Collections.shuffle(deck);
    }

    public DevelopmentCardType drawCard() {
        if (deck.isEmpty()) {
            return null; // The deck is empty
        }
        return deck.remove(0);
    }

    public int remainingCards() {
        return deck.size();
    }
}
