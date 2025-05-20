package com.example.cataniaunited.exception;

import com.example.cataniaunited.exception.ui.SpacingRuleViolationException;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpacingRuleViolationExceptionTest {

    @Test
    void testDefaultConstructor() {
        SpacingRuleViolationException exception = new SpacingRuleViolationException();
        assertNotNull(exception);
        assertEquals("Too close to another settlement or city", exception.getMessage());
        assertTrue(exception instanceof GameException, "Should be a subclass of GameException");
    }
}