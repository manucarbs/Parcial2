package edu.sv.ues.dam235.parcial2.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.UUID;

@Entity(tableName = "citas")
public class Cita {
    @PrimaryKey @NonNull
    public String id = UUID.randomUUID().toString();
    public String usuarioId;
    public String titulo;
    public String descripcion;
    public String fecha;          // dd/MM/yyyy
    public String hora;           // HH:mm
    public boolean pendiente;     // true = falta subir a Firestore
    public boolean eliminado;     // true = borrado local, falta borrar en la nube
}