package edu.sv.ues.dam235.parcial2.util;

import android.util.Patterns;

public class Validador {
    public static boolean correoValido(String c) {
        return c != null && Patterns.EMAIL_ADDRESS.matcher(c).matches();
    }
    // Mínimo 8 caracteres, alfanumérica
    public static boolean passwordValida(String p) {
        return p != null && p.matches("^[A-Za-z0-9]{8,}$");
    }
    // Mínimo 7 caracteres
    public static boolean nombreValido(String n) {
        return n != null && n.trim().length() >= 7;
    }
}