package com.example.cataniaunited.game;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import io.quarkus.test.InjectMock;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class VictoryPointServiceTest {

    @Inject
    VictoryPointService victoryPointService;

    @InjectMock
    GameService gameService;

    @Mock
    GameBoard mockGameBoard;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCalculateVictoryPoints_noSettlements() throws GameException {
        when(gameService.getGameboardByLobbyId("lobby1")).thenReturn(mockGameBoard);
        when(mockGameBoard.getSettlementPositionGraph()).thenReturn(List.of());

        int points = victoryPointService.calculateVictoryPoints("lobby1", "player1");

        assertEquals(0, points);
        verify(gameService).getGameboardByLobbyId("lobby1");
    }



}
