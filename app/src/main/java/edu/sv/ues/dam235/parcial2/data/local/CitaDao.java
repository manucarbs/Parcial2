package edu.sv.ues.dam235.parcial2.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import edu.sv.ues.dam235.parcial2.data.model.Cita;
import java.util.List;

@Dao
public interface CitaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(Cita cita);

    @Query("SELECT * FROM citas WHERE usuarioId = :uid AND eliminado = 0 "
            + "ORDER BY substr(fecha,7,4) || substr(fecha,4,2) || substr(fecha,1,2), hora")
    List<Cita> obtenerPorUsuario(String uid);

    @Query("SELECT * FROM citas WHERE usuarioId = :uid")
    List<Cita> obtenerTodasPorUsuario(String uid);

    @Query("SELECT * FROM citas WHERE usuarioId = :uid AND pendiente = 1")
    List<Cita> obtenerPendientes(String uid);

    @Query("SELECT * FROM citas WHERE id = :id LIMIT 1")
    Cita buscarPorId(String id);

    @Delete
    void eliminar(Cita cita);
}