package com.example.cataniaunited.player;
import io.quarkus.websockets.next.WebSocketConnection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void testDefaultConstructor() {
        Player player = new Player();

        Assertions.assertEquals("RandomPlayer_4", player.getUsername());
        Assertions.assertNotNull(player.getUniqueId(), "uniqueId should not be null");
        Assertions.assertFalse(player.getUniqueId().isEmpty(), "uniqueId should not be empty");
    }

    @Test
    void testCustomConstructor() {
        String customUsername = "Alice1";
        Player player = new Player(customUsername);
        Assertions.assertEquals(customUsername, player.getUsername());
        Assertions.assertNotNull(player.getUniqueId(), "uniqueId should not be null");
    }

    @Test
    void testSetUsername() {
        Player player = new Player();
        String newUsername = "Bob";
        player.setUsername(newUsername);
        Assertions.assertEquals(newUsername, player.getUsername());
    }

    @Test
    void testUniqueIdIsDifferentForEachPlayer() {
        Player player1 = new Player();
        Player player2 = new Player();
        Assertions.assertNotEquals(player1.getUniqueId(), player2.getUniqueId(),
                "Each Player should have a unique ID");
    }
}
