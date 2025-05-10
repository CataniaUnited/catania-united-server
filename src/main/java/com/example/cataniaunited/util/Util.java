package com.example.cataniaunited.util;

public class Util {

    private Util() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
