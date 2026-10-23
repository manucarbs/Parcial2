package edu.sv.ues.dam235.parcial2.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import edu.sv.ues.dam235.parcial2.data.model.Usuario;
import java.util.List;

@Dao
public interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(Usuario u);

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    Usuario buscarPorCorreo(String correo);

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    Usuario buscarPorId(String id);

    @Query("SELECT * FROM usuarios WHERE pendiente = 1")
    List<Usuario> obtenerPendientes();
}