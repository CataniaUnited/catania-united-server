package com.example.cataniaunited.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    @Test
    void testIsEmptyWithNullString() {
        assertTrue(Util.isEmpty(null), "isEmpty should return true for null string.");
    }

    @Test
    void testIsEmptyWithEmptyString() {
        assertTrue(Util.isEmpty(""), "isEmpty should return true for an empty string.");
    }

    @Test
    void testIsEmptyWithNonEmptyString() {
        assertFalse(Util.isEmpty("hello"), "isEmpty should return false for a non-empty string.");
    }

    @Test
    void testIsEmptyWithStringContainingOnlyWhitespace() {
        assertTrue(Util.isEmpty(" "), "isEmpty should return true for a string with a single space.");
        assertTrue(Util.isEmpty("   "), "isEmpty should return true for a string with multiple spaces.");
        assertTrue(Util.isEmpty("\t"), "isEmpty should return true for a string with a tab character.");
        assertTrue(Util.isEmpty("\n"), "isEmpty should return true for a string with a newline character.");
    }

    @Test
    void testIsEmptyWithStringContainingLeadingAndTrailingWhitespace() {
        assertFalse(Util.isEmpty(" hello "), "isEmpty should return false for a string with content and surrounding whitespace.");
    }

    @Test
    void testPrivateConstructorThrowsException() {
        Exception exception = assertThrows(InvocationTargetException.class, () -> {
            Constructor<Util> constructor = Util.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });

        assertNotNull(exception.getCause(), "InvocationTargetException should have a cause.");
        assertEquals(IllegalStateException.class, exception.getCause().getClass(), "The cause should be an IllegalStateException.");
        assertEquals("Utility class", exception.getCause().getMessage(), "The exception message should match.");
    }
}