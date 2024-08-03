package com.createfuture.flink.utils;

public class EnvironmentUtils {
    public static String getOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    public static int getOrDefault(String key, int defaultValue) {
        String value = System.getenv(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public static long getOrDefault(String key, long defaultValue) {
        String value = System.getenv(key);
        return value != null ? Long.parseLong(value) : defaultValue;
    }
}
