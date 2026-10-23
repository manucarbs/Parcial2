package edu.sv.ues.dam235.parcial2.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey @NonNull
    public String id = "";        // uid de Firebase Auth
    public String correo;
    public String nombre;
    public String passwordHash;   // SHA-256 con salt, nunca texto plano
    public String salt;
    public boolean pendiente;     // cambios de perfil sin subir a la nube
}