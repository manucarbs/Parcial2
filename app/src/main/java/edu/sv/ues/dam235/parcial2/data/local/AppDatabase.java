package edu.sv.ues.dam235.parcial2.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import edu.sv.ues.dam235.parcial2.data.model.Cita;
import edu.sv.ues.dam235.parcial2.data.model.Usuario;

@Database(entities = {Usuario.class, Cita.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instancia;

    public abstract UsuarioDao usuarioDao();
    public abstract CitaDao citaDao();

    public static synchronized AppDatabase getInstance(Context ctx) {
        if (instancia == null) {
            instancia = Room.databaseBuilder(ctx.getApplicationContext(),
                    AppDatabase.class, "citas_db").build();
        }
        return instancia;
    }
}