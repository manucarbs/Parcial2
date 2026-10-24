package edu.sv.ues.dam235.parcial2.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import edu.sv.ues.dam235.parcial2.data.model.Reserva;
import java.util.List;

@Dao
public interface ReservaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(Reserva reserva);

    @Query("SELECT * FROM reservas WHERE usuarioId = :uid AND eliminado = 0 "
            + "ORDER BY substr(fecha,7,4) || substr(fecha,4,2) || substr(fecha,1,2), hora")
    List<Reserva> obtenerPorUsuario(String uid);

    @Query("SELECT * FROM reservas WHERE usuarioId = :uid")
    List<Reserva> obtenerTodasPorUsuario(String uid);

    @Query("SELECT * FROM reservas WHERE usuarioId = :uid AND pendiente = 1")
    List<Reserva> obtenerPendientes(String uid);

    @Query("SELECT * FROM reservas WHERE id = :id LIMIT 1")
    Reserva buscarPorId(String id);

    @Delete
    void eliminar(Reserva reserva);
}