package edu.sv.ues.dam235.parcial2.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SesionManager {
    private final SharedPreferences prefs;

    public SesionManager(Context c) {
        try {
            MasterKey key = new MasterKey.Builder(c)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            prefs = EncryptedSharedPreferences.create(c, "sesion", key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // "Recordar usuario y contraseña"
    public void recordar(String correo, String pass) {
        prefs.edit().putBoolean("recordar", true)
                .putString("correo", correo).putString("pass", pass).apply();
    }
    public void olvidar() {
        prefs.edit().putBoolean("recordar", false).remove("correo").remove("pass").apply();
    }
    public boolean recordado() { return prefs.getBoolean("recordar", false); }
    public String getCorreo() { return prefs.getString("correo", ""); }
    public String getPass() { return prefs.getString("pass", ""); }

    // Sesión activa
    public void setUsuarioActivo(String uid) { prefs.edit().putString("uid", uid).apply(); }
    public String getUsuarioActivo() { return prefs.getString("uid", null); }
    public void cerrarSesion() { prefs.edit().remove("uid").apply(); }

    // Cambio de contraseña pendiente de subir a la nube (cifrado, se borra al sincronizar)
    public void guardarPassPendiente(String uid, String anterior, String nueva) {
        // Si ya había un cambio pendiente, se conserva la contraseña original de la nube
        String original = prefs.getString("passAnterior_" + uid, null);
        prefs.edit()
                .putString("passAnterior_" + uid, original != null ? original : anterior)
                .putString("passNueva_" + uid, nueva)
                .apply();
    }
    public String getPassAnterior(String uid) { return prefs.getString("passAnterior_" + uid, null); }
    public String getPassNueva(String uid) { return prefs.getString("passNueva_" + uid, null); }
    public void limpiarPassPendiente(String uid) {
        prefs.edit().remove("passAnterior_" + uid).remove("passNueva_" + uid).apply();
    }

}