package edu.sv.ues.dam235.parcial2.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import edu.sv.ues.dam235.parcial2.R;
import edu.sv.ues.dam235.parcial2.data.local.AppDatabase;
import edu.sv.ues.dam235.parcial2.data.model.Usuario;
import edu.sv.ues.dam235.parcial2.util.RedUtil;
import edu.sv.ues.dam235.parcial2.util.SeguridadUtil;
import edu.sv.ues.dam235.parcial2.util.SesionManager;
import edu.sv.ues.dam235.parcial2.util.Validador;
import edu.sv.ues.dam235.parcial2.data.repository.ReservaRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerfilActivity extends AppCompatActivity {
    private TextInputEditText etCorreo, etNombre, etActual, etNueva, etConfirmar;
    private TextView tvEstado;
    private Button btnReintentar, btnGuardar;
    private SesionManager sesion;
    private String uid;
    private ReservaRepository repoReservas;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        sesion = new SesionManager(this);
        uid = sesion.getUsuarioActivo();

        repoReservas = new ReservaRepository(this, uid);
        etCorreo = findViewById(R.id.etCorreo);
        etNombre = findViewById(R.id.etNombre);
        etActual = findViewById(R.id.etActual);
        etNueva = findViewById(R.id.etNueva);
        etConfirmar = findViewById(R.id.etConfirmar);
        tvEstado = findViewById(R.id.tvEstado);
        btnReintentar = findViewById(R.id.btnReintentar);
        btnGuardar = findViewById(R.id.btnGuardar);

        btnGuardar.setOnClickListener(v -> guardar());
        btnReintentar.setOnClickListener(v -> sincronizar());

        cargarPerfil();
    }

    private void cargarPerfil() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Usuario u = db.usuarioDao().buscarPorId(uid);
            boolean hayPend = (u != null && u.pendiente)
                    || !db.reservaDao().obtenerPendientes(uid).isEmpty();
            runOnUiThread(() -> {
                if (u == null) { finish(); return; }
                etCorreo.setText(u.correo);
                etNombre.setText(u.nombre);
                refrescarEstado();
                if (hayPend) sincronizar();   // reintento automático al abrir Perfil
            });
        });
    }

    private void refrescarEstado() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Usuario u = db.usuarioDao().buscarPorId(uid);
            int citas = db.reservaDao().obtenerPendientes(uid).size();
            boolean perfil = u != null && u.pendiente;
            runOnUiThread(() -> {
                boolean hay = perfil || citas > 0;
                StringBuilder sb = new StringBuilder();
                if (hay) {
                    sb.append("⏳ Pendiente de sincronizar:");
                    if (perfil) sb.append("\n• Cambios de perfil");
                    if (citas > 0) sb.append("\n• ").append(citas)
                            .append(citas == 1 ? " cita" : " citas");
                } else {
                    sb.append("✅ Todo sincronizado con la nube");
                }
                tvEstado.setText(sb.toString());
                btnReintentar.setVisibility(hay ? View.VISIBLE : View.GONE);
            });
        });
    }

    // ---------- Guardar (validaciones + confirmación) ----------
    private void guardar() {
        String nombre = etNombre.getText().toString().trim();
        String actual = etActual.getText().toString();
        String nueva = etNueva.getText().toString();
        String conf = etConfirmar.getText().toString();
        boolean cambiaPass = !actual.isEmpty() || !nueva.isEmpty() || !conf.isEmpty();

        if (!Validador.nombreValido(nombre)) {
            etNombre.setError("Mínimo 7 caracteres");
            return;
        }
        if (cambiaPass) {
            if (actual.isEmpty()) { etActual.setError("Escribe tu contraseña actual"); return; }
            if (!Validador.passwordValida(nueva)) {
                etNueva.setError("Mínimo 8 caracteres, solo letras y números");
                return;
            }
            if (!nueva.equals(conf)) { etConfirmar.setError("Las contraseñas no coinciden"); return; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar cambios")
                .setMessage("¿Deseas guardar los cambios de tu perfil?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d, w) -> aplicar(nombre, actual, nueva, cambiaPass))
                .show();
    }

    // ---------- Paso 1: guardar SIEMPRE en local y marcar pendiente ----------
    private void aplicar(String nombre, String actual, String nueva, boolean cambiaPass) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Usuario u = db.usuarioDao().buscarPorId(uid);
            if (u == null) return;

            if (cambiaPass && !SeguridadUtil.hash(actual, u.salt).equals(u.passwordHash)) {
                runOnUiThread(() -> etActual.setError("Contraseña actual incorrecta"));
                return;
            }
            if (!cambiaPass && nombre.equals(u.nombre)) {
                runOnUiThread(() -> Toast.makeText(this, "No hay cambios que guardar",
                        Toast.LENGTH_SHORT).show());
                return;
            }

            u.nombre = nombre;
            if (cambiaPass) {
                u.salt = SeguridadUtil.generarSalt();
                u.passwordHash = SeguridadUtil.hash(nueva, u.salt);
                sesion.guardarPassPendiente(uid, actual, nueva);
                if (sesion.recordado()) sesion.recordar(u.correo, nueva);
            }
            u.pendiente = true;
            db.usuarioDao().guardar(u);

            runOnUiThread(() -> {
                etActual.setText("");
                etNueva.setText("");
                etConfirmar.setText("");
                refrescarEstado();
                Toast.makeText(this, "Guardado en el dispositivo", Toast.LENGTH_SHORT).show();
                sincronizar();
            });
        });
    }

    // ---------- Paso 2: subir a la nube si hay internet ----------
    private void sincronizar() {
        if (!RedUtil.hayInternet(this)) {
            Toast.makeText(this, "Sin internet: los cambios siguen pendientes",
                    Toast.LENGTH_LONG).show();
            refrescarEstado();
            return;
        }
        btnReintentar.setEnabled(false);

        executor.execute(() -> {
            Usuario u = AppDatabase.getInstance(this).usuarioDao().buscarPorId(uid);
            if (u == null || !u.pendiente) {
                runOnUiThread(this::sincronizarReservas);   // perfil al día: seguir con las citas
                return;
            }

            String nueva = sesion.getPassNueva(uid);
            String anterior = sesion.getPassAnterior(uid);

            if (nueva != null && anterior != null) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(u.correo, anterior)
                        .addOnSuccessListener(r -> r.getUser().updatePassword(nueva)
                                .addOnSuccessListener(x -> {
                                    sesion.limpiarPassPendiente(uid);
                                    subirNombre();
                                })
                                .addOnFailureListener(this::falloSync))
                        .addOnFailureListener(this::falloSync);
            } else {
                subirNombre();
            }
        });
    }

    private void subirNombre() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Usuario u = db.usuarioDao().buscarPorId(uid);
            Map<String, Object> datos = new HashMap<>();
            datos.put("nombre", u.nombre);

            FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                    .set(datos, SetOptions.merge())
                    .addOnSuccessListener(x -> executor.execute(() -> {
                        Usuario f = db.usuarioDao().buscarPorId(uid);
                        f.pendiente = false;
                        db.usuarioDao().guardar(f);
                        runOnUiThread(this::sincronizarReservas);   // ahora las citas
                    }))
                    .addOnFailureListener(this::falloSync);
        });
    }

    private void sincronizarReservas() {
        repoReservas.sincronizar(ok -> {
            btnReintentar.setEnabled(true);
            refrescarEstado();
            Toast.makeText(this, ok
                            ? "Todo sincronizado con la nube"
                            : "Algunos cambios siguen pendientes",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void falloSync(Exception e) {
        runOnUiThread(() -> {
            btnReintentar.setEnabled(true);
            Toast.makeText(this, "No se pudo sincronizar: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }
}