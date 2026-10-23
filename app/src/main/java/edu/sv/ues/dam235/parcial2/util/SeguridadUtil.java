package edu.sv.ues.dam235.parcial2.util;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class SeguridadUtil {
    public static String generarSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return Base64.encodeToString(b, Base64.NO_WRAP);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(d, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}