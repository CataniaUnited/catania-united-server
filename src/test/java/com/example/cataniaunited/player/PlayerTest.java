package com.example.cataniaunited.player;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTest {

    @Test
    void testDefaultConstructor() {
        Player player = new Player();

        Assertions.assertTrue(player.getUsername().startsWith("RandomPlayer_"));
        Assertions.assertNotNull(player.getUniqueId(), "uniqueId should not be null");
        Assertions.assertFalse(player.getUniqueId().isEmpty(), "uniqueId should not be empty");
    }

    @Test
    void testCustomConstructor() {
        String customUsername = "Alice1";
        Player player = new Player(customUsername);
        assertEquals(customUsername, player.getUsername());
        Assertions.assertNotNull(player.getUniqueId(), "uniqueId should not be null");
    }

    @Test
    void testSetUsername() {
        Player player = new Player();
        String newUsername = "Bob";
        player.setUsername(newUsername);
        assertEquals(newUsername, player.getUsername());
    }

    @Test
    void testUniqueIdIsDifferentForEachPlayer() {
        Player player1 = new Player();
        Player player2 = new Player();
        Assertions.assertNotEquals(player1.getUniqueId(), player2.getUniqueId(),
                "Each Player should have a unique ID");
    }

    @Test
    void testToJsonIncludesUsernameAndVictoryPoints() {
        Player player = new Player("TestUser");
        player.addVictoryPoints(3);

        ObjectNode json = player.toJson();

        assertNotNull(json);
        assertEquals("TestUser", json.get("username").asText());
        assertEquals(3, json.get("victoryPoints").asInt());
    }
}
