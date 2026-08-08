package com.systemdesign.lab5.util;

public final class HashFormatter {

    private HashFormatter() {
    }

    public static String toHex(long hash) {
        return String.format("%016x", hash);
    }
}
