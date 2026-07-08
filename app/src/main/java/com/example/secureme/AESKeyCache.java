package com.example.secureme;

import javax.crypto.SecretKey;

public class AESKeyCache {

    private static SecretKey cachedKey;

    public static void setKey(SecretKey key) {
        cachedKey = key;
    }

    public static SecretKey getKey() {
        return cachedKey;
    }

    public static void clearKey() {
        cachedKey = null;
    }

    public static boolean isKeyCached() {
        return cachedKey != null;
    }
}
