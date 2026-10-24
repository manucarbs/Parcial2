package edu.sv.ues.dam235.parcial2.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.UUID;

@Entity(tableName = "reservas")
public class Reserva {
    @PrimaryKey @NonNull
    public String id = UUID.randomUUID().toString();
    public String usuarioId;
    public String restaurante;
    public int personas = 1;
    public String fecha;          // dd/MM/yyyy
    public String hora;           // HH:mm
    public String notas;
    public boolean pendiente;     // true = falta subir a Firestore
    public boolean eliminado;     // true = borrado local, falta borrar en la nube
}