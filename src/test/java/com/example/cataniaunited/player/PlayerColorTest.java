package com.example.cataniaunited.player;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class PlayerColorTest {
    @Test
    void testHexCodesNotNullOrEmpty(){
        for (PlayerColor color: PlayerColor.values()) {
            assertNotNull(color.getHexCode());
            assertFalse(color.getHexCode().isEmpty());
        }
    }

    @Test
    void testUniqueHexCodes(){
        Set<String> hexSet = new HashSet<>();
        for (PlayerColor color : PlayerColor.values()){
            assertTrue(hexSet.add(color.getHexCode()), "Duplicate hex code: " + color.getHexCode());
        }
    }
}
