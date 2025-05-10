package com.example.cataniaunited.exception;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GameExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String errorMessage = "A simple game error occurred.";
        GameException exception = new GameException(errorMessage);

        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndArgs() {
        String messageFormat = "Error for player %s in lobby %s.";
        String player = "Player1";
        String lobby = "Lobby123";
        String expectedMessage = String.format(messageFormat, player, lobby);

        GameException exception = new GameException(messageFormat, player, lobby);

        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndArgs_TooManyArgsProvided() {
        String messageFormat = "Simple error.";
        String expectedMessage = "Simple error.";

        GameException exception = new GameException(messageFormat, "extraArg1", 123);

        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testGameExceptionIsAnException() {
        GameException exception = new GameException("Test");
        assertTrue(exception instanceof Exception, "GameException should be an instance of Exception");
        assertTrue(exception instanceof Throwable, "GameException should be an instance of Throwable");
    }
}