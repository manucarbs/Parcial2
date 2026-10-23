package edu.sv.ues.dam235.parcial2.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import edu.sv.ues.dam235.parcial2.data.local.AppDatabase;
import edu.sv.ues.dam235.parcial2.data.model.Cita;
import edu.sv.ues.dam235.parcial2.util.RedUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CitaRepository {

    public interface Resultado<T> { void listo(T dato); }

    private static final int TIMEOUT_SEG = 10;

    private final Context ctx;
    private final AppDatabase db;
    private final String uid;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public CitaRepository(Context ctx, String uid) {
        this.ctx = ctx.getApplicationContext();
        this.db = AppDatabase.getInstance(ctx);
        this.uid = uid;
    }

    private CollectionReference remota() {
        return FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid).collection("citas");
    }

    // ---------- READ (siempre desde Room) ----------
    public void listar(Resultado<List<Cita>> cb) {
        executor.execute(() -> {
            List<Cita> lista = db.citaDao().obtenerPorUsuario(uid);
            main.post(() -> cb.listo(lista));
        });
    }

    // ---------- CREATE / UPDATE ----------
    // Resultado: true si todo quedó sincronizado con la nube
    public void guardar(Cita c, Resultado<Boolean> cb) {
        executor.execute(() -> {
            c.usuarioId = uid;
            c.pendiente = true;
            c.eliminado = false;
            db.citaDao().guardar(c);      // 1) local primero
            sincronizarInterno();         // 2) nube si hay internet
            boolean ok = sinPendientes();
            main.post(() -> cb.listo(ok));
        });
    }

    // ---------- DELETE (borrado lógico hasta confirmar en la nube) ----------
    public void eliminar(Cita c, Resultado<Boolean> cb) {
        executor.execute(() -> {
            c.eliminado = true;
            c.pendiente = true;
            db.citaDao().guardar(c);
            sincronizarInterno();
            boolean ok = sinPendientes();
            main.post(() -> cb.listo(ok));
        });
    }

    // ---------- SINCRONIZAR ----------
    public void sincronizar(Resultado<Boolean> cb) {
        executor.execute(() -> {
            sincronizarInterno();
            boolean ok = sinPendientes();
            main.post(() -> cb.listo(ok));
        });
    }

    private boolean sinPendientes() {
        return db.citaDao().obtenerPendientes(uid).isEmpty();
    }

    // Debe ejecutarse SIEMPRE en el hilo del executor
    private void sincronizarInterno() {
        if (!RedUtil.hayInternet(ctx)) return;
        try {
            // 1) Subir pendientes
            for (Cita c : db.citaDao().obtenerPendientes(uid)) {
                DocumentReference ref = remota().document(c.id);
                if (c.eliminado) {
                    Tasks.await(ref.delete(), TIMEOUT_SEG, TimeUnit.SECONDS);
                    db.citaDao().eliminar(c);
                } else {
                    Tasks.await(ref.set(aMapa(c)), TIMEOUT_SEG, TimeUnit.SECONDS);
                    c.pendiente = false;
                    db.citaDao().guardar(c);
                }
            }

            // 2) Bajar lo que hay en la nube (p. ej. citas creadas en otro dispositivo)
            QuerySnapshot snap = Tasks.await(remota().get(Source.SERVER),
                    TIMEOUT_SEG, TimeUnit.SECONDS);
            Set<String> idsRemotos = new HashSet<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                idsRemotos.add(d.getId());
                Cita local = db.citaDao().buscarPorId(d.getId());
                if (local != null && local.pendiente) continue; // lo local pendiente manda
                Cita c = new Cita();
                c.id = d.getId();
                c.usuarioId = uid;
                c.titulo = d.getString("titulo");
                c.descripcion = d.getString("descripcion");
                c.fecha = d.getString("fecha");
                c.hora = d.getString("hora");
                c.pendiente = false;
                c.eliminado = false;
                db.citaDao().guardar(c);
            }

            // 3) Lo que ya no existe en la nube y no está pendiente, se quita del local
            for (Cita l : db.citaDao().obtenerTodasPorUsuario(uid)) {
                if (!l.pendiente && !idsRemotos.contains(l.id)) db.citaDao().eliminar(l);
            }
        } catch (Exception e) {
            // Sin conexión real o tiempo agotado: lo pendiente se reintenta después
        }
    }

    private Map<String, Object> aMapa(Cita c) {
        Map<String, Object> m = new HashMap<>();
        m.put("titulo", c.titulo);
        m.put("descripcion", c.descripcion);
        m.put("fecha", c.fecha);
        m.put("hora", c.hora);
        return m;
    }
}