package com.example.cataniaunited.game;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.GameBoard;
import com.example.cataniaunited.game.board.SettlementPosition;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import io.quarkus.websockets.next.WebSocketConnection;
import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import io.smallrye.mutiny.Uni;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class VictoryPointServiceTest {

    @Inject
    VictoryPointService victoryPointService;

    @InjectMock
    GameService gameService;

    @Mock
    GameBoard mockGameBoard;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp(){
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testCalculateVictoryPoints_noSettlements() throws GameException {
        when(gameService.getGameboardByLobbyId("lobby1")).thenReturn(mockGameBoard);
        when(mockGameBoard.getSettlementPositionGraph()).thenReturn(List.of());

        int points = victoryPointService.calculateVictoryPoints("lobby1", "player1");

        assertEquals(0, points);
        verify(gameService).getGameboardByLobbyId("lobby1");
    }

    @Test
    void testCalculateVictoryPoints_oneSettlement_returnsOnePoint() throws GameException {
        when(gameService.getGameboardByLobbyId("lobby1")).thenReturn(mockGameBoard);
        when(mockGameBoard.calculateVictoryPointsForPlayer("player1")).thenReturn(1);

        int points = victoryPointService.calculateVictoryPoints("lobby1", "player1");
        assertEquals(1, points);
        verify(gameService).getGameboardByLobbyId("lobby1");
    }

    @Test
    void testCalculateVictoryPoints_multipleSettlements() throws GameException {
        when(gameService.getGameboardByLobbyId("lobby1")).thenReturn(mockGameBoard);
        when(mockGameBoard.calculateVictoryPointsForPlayer("player1")).thenReturn(5);
        when(mockGameBoard.calculateVictoryPointsForPlayer("player2")).thenReturn(3);

        int points1 = victoryPointService.calculateVictoryPoints("lobby1", "player1");
        int points2 = victoryPointService.calculateVictoryPoints("lobby1", "player2");

        assertEquals(5, points1);
        assertEquals(3, points2);
        verify(gameService, times(2)).getGameboardByLobbyId("lobby1");
    }

    @Test
    void testCheckForWin_notEnoughPoints_returnsFalse() throws GameException {
        SettlementPosition mockedSettlementPosition = mock(SettlementPosition.class);

        when(mockedSettlementPosition.getBuildingOwner()).thenReturn("player1");
        when(mockGameBoard.getSettlementPositionGraph())
                .thenReturn(List.of(mockedSettlementPosition, mockedSettlementPosition, mockedSettlementPosition,
                        mockedSettlementPosition, mockedSettlementPosition, mockedSettlementPosition,mockedSettlementPosition));
        when(gameService.getGameboardByLobbyId("lobby1")).thenReturn(mockGameBoard);

        boolean playerWon = victoryPointService.checkForWin("lobby1", "player1");

        assertFalse(playerWon);
    }

    @Test
    void checkForWin_tenPoints_returnsTrue() throws GameException {
        when(gameService.getGameboardByLobbyId("lobby1")).thenReturn(mockGameBoard);
        when(mockGameBoard.calculateVictoryPointsForPlayer("player1")).thenReturn(10);

        boolean playerWon = victoryPointService.checkForWin("lobby1", "player1");

        assertTrue(playerWon);
        verify(mockGameBoard).calculateVictoryPointsForPlayer("player1");
        verify(gameService).getGameboardByLobbyId("lobby1");
    }


    @Test
    void checkForWin_moreThenTenPoints_returnsTrue() throws GameException {
        when(gameService.getGameboardByLobbyId("lobby1")).thenReturn(mockGameBoard);
        when(mockGameBoard.calculateVictoryPointsForPlayer("player1")).thenReturn(11);

        boolean playerWon = victoryPointService.checkForWin("lobby1", "player1");

        assertTrue(playerWon);
    }


    @Test
    void testBroadcastWin_sendsCorrectMessage(){
        WebSocketConnection mockConnection = mock(WebSocketConnection.class, RETURNS_DEEP_STUBS);

        when(mockConnection.broadcast().sendText(any(MessageDTO.class))).thenReturn(Uni.createFrom().voidItem());

        String lobbyId = "lobby1";
        String playerId = "player1";

        Uni<MessageDTO> resultUni = victoryPointService.broadcastWin(mockConnection, lobbyId, playerId);
        MessageDTO result = resultUni.await().indefinitely();

        assertEquals(MessageType.GAME_WON, result.getType());
        assertEquals(playerId, result.getPlayer());
        assertEquals(lobbyId, result.getLobbyId());
        assertEquals(playerId, result.getMessage().get("winner").asText());
        
        verify(mockConnection.broadcast()).sendText(any(MessageDTO.class));
    }




}
