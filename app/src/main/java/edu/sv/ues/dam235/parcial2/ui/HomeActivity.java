package edu.sv.ues.dam235.parcial2.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import edu.sv.ues.dam235.parcial2.R;
import edu.sv.ues.dam235.parcial2.data.local.AppDatabase;
import edu.sv.ues.dam235.parcial2.data.model.Cita;
import edu.sv.ues.dam235.parcial2.data.model.Usuario;
import edu.sv.ues.dam235.parcial2.data.repository.CitaRepository;
import edu.sv.ues.dam235.parcial2.util.SesionManager;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeActivity extends AppCompatActivity implements CitaAdapter.Acciones {
    private SesionManager sesion;
    private CitaRepository repo;
    private CitaAdapter adapter;
    private TextView tvBienvenida, tvVacio;
    private ConnectivityManager.NetworkCallback redCallback;
    private final AtomicBoolean sincronizando = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setSupportActionBar((MaterialToolbar) findViewById(R.id.toolbar));

        sesion = new SesionManager(this);
        repo = new CitaRepository(this, sesion.getUsuarioActivo());

        tvBienvenida = findViewById(R.id.tvBienvenida);
        tvVacio = findViewById(R.id.tvVacio);

        adapter = new CitaAdapter(this);
        RecyclerView rv = findViewById(R.id.rvCitas);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAgregar);
        fab.setOnClickListener(v -> mostrarFormulario(null));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Nombre actualizado (por si se cambió en Perfil)
        String uid = sesion.getUsuarioActivo();
        executor.execute(() -> {
            Usuario u = AppDatabase.getInstance(this).usuarioDao().buscarPorId(uid);
            if (u != null) runOnUiThread(() -> tvBienvenida.setText("Hola, " + u.nombre));
        });

        // Mostrar lo local de inmediato y luego sincronizar
        recargar();
        sincronizarAhora(false);

        // Sincronizar solo cuando vuelva el internet con la app abierta
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        redCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                // VALIDATED = la red ya tiene salida real a internet
                if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    sincronizarAhora(false);
                }
            }
        };
        cm.registerDefaultNetworkCallback(redCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (redCallback != null) {
            try {
                ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE))
                        .unregisterNetworkCallback(redCallback);
            } catch (Exception ignorada) { }
            redCallback = null;
        }
    }

    // El AtomicBoolean evita lanzar dos sincronizaciones al mismo tiempo
    private void sincronizarAhora(boolean avisar) {
        if (!sincronizando.compareAndSet(false, true)) return;
        repo.sincronizar(ok -> {
            sincronizando.set(false);
            recargar();
            if (avisar) {
                Toast.makeText(this, ok
                                ? "Todo sincronizado con la nube"
                                : "Sin conexión: los cambios siguen pendientes",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recargar() {
        repo.listar(this::mostrar);
    }

    private void mostrar(List<Cita> citas) {
        adapter.setDatos(citas);
        tvVacio.setVisibility(citas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------- Acciones del adapter ----------
    @Override
    public void editar(Cita c) {
        mostrarFormulario(c);
    }

    @Override
    public void eliminar(Cita c) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar cita")
                .setMessage("¿Eliminar \"" + c.titulo + "\"?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (d, w) ->
                        repo.eliminar(c, ok -> {
                            avisar(ok, "Cita eliminada");
                            recargar();
                        }))
                .show();
    }

    // ---------- Formulario (crear / editar) ----------
    private void mostrarFormulario(Cita existente) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_cita, null);
        TextInputEditText etTitulo = vista.findViewById(R.id.etTitulo);
        TextInputEditText etDescripcion = vista.findViewById(R.id.etDescripcion);
        TextInputEditText etFecha = vista.findViewById(R.id.etFecha);
        TextInputEditText etHora = vista.findViewById(R.id.etHora);

        if (existente != null) {
            etTitulo.setText(existente.titulo);
            etDescripcion.setText(existente.descripcion);
            etFecha.setText(existente.fecha);
            etHora.setText(existente.hora);
        }
        etFecha.setOnClickListener(v -> elegirFecha(etFecha));
        etHora.setOnClickListener(v -> elegirHora(etHora));

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(existente == null ? "Nueva cita" : "Editar cita")
                .setView(vista)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null) // se define abajo para no cerrar si hay errores
                .create();

        dialogo.setOnShowListener(d ->
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String fecha = etFecha.getText().toString().trim();
                    String hora = etHora.getText().toString().trim();

                    if (titulo.isEmpty()) { etTitulo.setError("Escribe un título"); return; }
                    if (fecha.isEmpty()) { etFecha.setError("Elige una fecha"); return; }
                    if (hora.isEmpty()) { etHora.setError("Elige una hora"); return; }

                    Cita c = (existente != null) ? existente : new Cita();
                    c.titulo = titulo;
                    c.descripcion = etDescripcion.getText().toString().trim();
                    c.fecha = fecha;
                    c.hora = hora;

                    dialogo.dismiss();
                    repo.guardar(c, ok -> {
                        avisar(ok, "Cita guardada");
                        recargar();
                    });
                }));
        dialogo.show();
    }

    private void elegirFecha(TextInputEditText et) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (picker, y, m, d) ->
                et.setText(String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void elegirHora(TextInputEditText et) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (picker, h, min) ->
                et.setText(String.format(Locale.US, "%02d:%02d", h, min)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true)
                .show();
    }

    private void avisar(boolean sincronizado, String mensaje) {
        Toast.makeText(this, sincronizado
                        ? mensaje + " y sincronizada con la nube"
                        : mensaje + " en el dispositivo (pendiente de sincronizar)",
                Toast.LENGTH_SHORT).show();
    }

    // ---------- Menú ----------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sync) {
            sincronizarAhora(true);
            return true;
        } else if (id == R.id.action_perfil) {
            startActivity(new Intent(this, PerfilActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            cerrarSesion();
            return true;
        }
        if (id == R.id.action_mapa) {
            startActivity(new Intent(this, MapaActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cerrarSesion() {
        sesion.cerrarSesion();
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}