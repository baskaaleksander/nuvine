package com.baskaaleksander.nuvine.application.util;

public class MaskingUtil {

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "invalid";
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) {
            return "*@" + parts[1];
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
    }
}
