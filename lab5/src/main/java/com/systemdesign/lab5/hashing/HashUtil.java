package com.systemdesign.lab5.hashing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

    private static final String ALGORITHM = "SHA-256";

    private HashUtil() {
    }

    public static long sha256Hash(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            long hash = 0L;
            for (int i = 0; i < Long.BYTES; i++) {
                hash = (hash << 8) | (hashBytes[i] & 0xFFL);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " algorithm not available", e);
        }
    }
}
