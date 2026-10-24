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
import edu.sv.ues.dam235.parcial2.data.model.Reserva;
import edu.sv.ues.dam235.parcial2.util.RedUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReservaRepository {

    public interface Resultado<T> { void listo(T dato); }

    private static final int TIMEOUT_SEG = 10;

    private final Context ctx;
    private final AppDatabase db;
    private final String uid;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public ReservaRepository(Context ctx, String uid) {
        this.ctx = ctx.getApplicationContext();
        this.db = AppDatabase.getInstance(ctx);
        this.uid = uid;
    }

    private CollectionReference remota() {
        return FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid).collection("reservas");
    }

    // ---------- READ (siempre desde Room) ----------
    public void listar(Resultado<List<Reserva>> cb) {
        executor.execute(() -> {
            List<Reserva> lista = db.reservaDao().obtenerPorUsuario(uid);
            main.post(() -> cb.listo(lista));
        });
    }

    // ---------- CREATE / UPDATE ----------
    // Resultado: true si todo quedó sincronizado con la nube
    public void guardar(Reserva r, Resultado<Boolean> cb) {
        executor.execute(() -> {
            r.usuarioId = uid;
            r.pendiente = true;
            r.eliminado = false;
            db.reservaDao().guardar(r);   // 1) local primero
            sincronizarInterno();         // 2) nube si hay internet
            boolean ok = sinPendientes();
            main.post(() -> cb.listo(ok));
        });
    }

    // ---------- DELETE (borrado lógico hasta confirmar en la nube) ----------
    public void eliminar(Reserva r, Resultado<Boolean> cb) {
        executor.execute(() -> {
            r.eliminado = true;
            r.pendiente = true;
            db.reservaDao().guardar(r);
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
        return db.reservaDao().obtenerPendientes(uid).isEmpty();
    }

    // Debe ejecutarse SIEMPRE en el hilo del executor
    private void sincronizarInterno() {
        if (!RedUtil.hayInternet(ctx)) return;
        try {
            // 1) Subir pendientes
            for (Reserva r : db.reservaDao().obtenerPendientes(uid)) {
                DocumentReference ref = remota().document(r.id);
                if (r.eliminado) {
                    Tasks.await(ref.delete(), TIMEOUT_SEG, TimeUnit.SECONDS);
                    db.reservaDao().eliminar(r);
                } else {
                    Tasks.await(ref.set(aMapa(r)), TIMEOUT_SEG, TimeUnit.SECONDS);
                    r.pendiente = false;
                    db.reservaDao().guardar(r);
                }
            }

            // 2) Bajar lo que hay en la nube (p. ej. reservas hechas en otro dispositivo)
            QuerySnapshot snap = Tasks.await(remota().get(Source.SERVER),
                    TIMEOUT_SEG, TimeUnit.SECONDS);
            Set<String> idsRemotos = new HashSet<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                idsRemotos.add(d.getId());
                Reserva local = db.reservaDao().buscarPorId(d.getId());
                if (local != null && local.pendiente) continue; // lo local pendiente manda
                Reserva r = new Reserva();
                r.id = d.getId();
                r.usuarioId = uid;
                r.restaurante = d.getString("restaurante");
                r.notas = d.getString("notas");
                r.fecha = d.getString("fecha");
                r.hora = d.getString("hora");
                Long p = d.getLong("personas");
                r.personas = (p == null) ? 1 : p.intValue();
                r.pendiente = false;
                r.eliminado = false;
                db.reservaDao().guardar(r);
            }

            // 3) Lo que ya no existe en la nube y no está pendiente, se quita del local
            for (Reserva l : db.reservaDao().obtenerTodasPorUsuario(uid)) {
                if (!l.pendiente && !idsRemotos.contains(l.id)) db.reservaDao().eliminar(l);
            }
        } catch (Exception e) {
            // Sin conexión real o tiempo agotado: lo pendiente se reintenta después
        }
    }

    private Map<String, Object> aMapa(Reserva r) {
        Map<String, Object> m = new HashMap<>();
        m.put("restaurante", r.restaurante);
        m.put("notas", r.notas);
        m.put("fecha", r.fecha);
        m.put("hora", r.hora);
        m.put("personas", r.personas);
        return m;
    }
}