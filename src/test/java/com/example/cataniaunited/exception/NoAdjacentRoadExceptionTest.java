package com.example.cataniaunited.exception;

import com.example.cataniaunited.exception.ui.NoAdjacentRoadException;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class NoAdjacentRoadExceptionTest {

    @Test
    void testDefaultConstructor() {
        NoAdjacentRoadException exception = new NoAdjacentRoadException();
        assertNotNull(exception);
        assertEquals("No adjacent roads found", exception.getMessage());
        assertTrue(exception instanceof GameException, "Should be a subclass of GameException");
    }
}