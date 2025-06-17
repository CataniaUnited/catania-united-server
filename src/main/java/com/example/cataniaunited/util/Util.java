package com.example.cataniaunited.util;

import java.util.Collection;
import java.util.Map;

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

    /**
     * Checks if a List is null or empty
     *
     * @param l The list to check.
     * @return true if the object is null or empty; false otherwise.
     */
    public static boolean isEmpty(Collection<?> l) {
        return l == null || l.isEmpty();
    }


    /**
     * Checks if a Map is null or empty
     *
     * @param m The map to check.
     * @return true if the object is null or empty; false otherwise.
     */
    public static boolean isEmpty(Map<?, ?> m){return m == null || m.isEmpty();}
}