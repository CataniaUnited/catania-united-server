package com.example.cataniaunited.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageDTOTest {

    @Test
    void testMessageDTOCreation() {
        MessageDTO message = new MessageDTO(MessageType.CREATE_LOBBY, "Player 1", "1");

        assertEquals(MessageType.CREATE_LOBBY, message.getType());
        assertEquals("Player 1", message.getPlayer());
        assertEquals("1", message.getLobbyId());
    }

    @Test
    void testMessageDTOSetters() {
        MessageDTO message = new MessageDTO();
        message.setType(MessageType.ERROR);
        message.setPlayer("Player 2");
        message.setLobbyId("2");

        assertEquals(MessageType.ERROR, message.getType());
        assertEquals("Player 2", message.getPlayer());
        assertEquals("2", message.getLobbyId());
    }
}