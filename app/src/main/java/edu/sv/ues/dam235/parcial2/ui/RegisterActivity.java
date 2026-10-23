package edu.sv.ues.dam235.parcial2.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import edu.sv.ues.dam235.parcial2.R;
import edu.sv.ues.dam235.parcial2.data.local.AppDatabase;
import edu.sv.ues.dam235.parcial2.data.model.Usuario;
import edu.sv.ues.dam235.parcial2.util.RedUtil;
import edu.sv.ues.dam235.parcial2.util.SeguridadUtil;
import edu.sv.ues.dam235.parcial2.util.Validador;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etCorreo, etNombre, etPassword, etConfirmar;
    private View btnCrear;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etCorreo = findViewById(R.id.etCorreo);
        etNombre = findViewById(R.id.etNombre);
        etPassword = findViewById(R.id.etPassword);
        etConfirmar = findViewById(R.id.etConfirmar);
        btnCrear = findViewById(R.id.btnCrear);
        btnCrear.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        String correo = etCorreo.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String conf = etConfirmar.getText().toString();

        if (!Validador.correoValido(correo)) {
            etCorreo.setError("Correo inválido");
            return;
        }
        if (!Validador.nombreValido(nombre)) {
            etNombre.setError("Mínimo 7 caracteres");
            return;
        }
        if (!Validador.passwordValida(pass)) {
            etPassword.setError("Mínimo 8 caracteres, solo letras y números");
            return;
        }
        if (!pass.equals(conf)) {
            etConfirmar.setError("Las contraseñas no coinciden");
            return;
        }
        if (!RedUtil.hayInternet(this)) {
            Toast.makeText(this, "Necesitas internet para registrarte", Toast.LENGTH_LONG).show();
            return;
        }

        btnCrear.setEnabled(false);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(correo, pass)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();

                    // La contraseña NUNCA se guarda en Firestore
                    Map<String, Object> datos = new HashMap<>();
                    datos.put("correo", correo);
                    datos.put("nombre", nombre);

                    FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                            .set(datos)
                            .addOnSuccessListener(x -> executor.execute(() -> {
                                Usuario u = new Usuario();
                                u.id = uid;
                                u.correo = correo;
                                u.nombre = nombre;
                                u.salt = SeguridadUtil.generarSalt();
                                u.passwordHash = SeguridadUtil.hash(pass, u.salt);
                                AppDatabase.getInstance(this).usuarioDao().guardar(u);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Cuenta creada. Inicia sesión.",
                                            Toast.LENGTH_SHORT).show();
                                    finish(); // vuelve al Login
                                });
                            }))
                            .addOnFailureListener(e -> {
                                btnCrear.setEnabled(true);
                                Toast.makeText(this, "Error en Firestore: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnCrear.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}