package com.example.cataniaunited.exception;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class IntersectionOccupiedExceptionTest {

    @Test
    void testDefaultConstructor() {
        IntersectionOccupiedException exception = new IntersectionOccupiedException();
        assertNotNull(exception);
        assertEquals("Intersection occupied!", exception.getMessage());
        assertTrue(exception instanceof GameException, "Should be a subclass of GameException");
    }
}