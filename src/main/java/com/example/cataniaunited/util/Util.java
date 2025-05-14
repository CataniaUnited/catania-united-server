package com.example.cataniaunited.util;

public class Util {

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws IllegalStateException if an attempt is made to instantiate the class.
     */
    private Util() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Checks if a string is null, empty, or consists only of whitespace.
     *
     * @param str The string to check.
     * @return true if the string is null, empty, or whitespace-only; false otherwise.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}