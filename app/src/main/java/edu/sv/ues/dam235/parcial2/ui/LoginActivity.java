package edu.sv.ues.dam235.parcial2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import edu.sv.ues.dam235.parcial2.R;
import edu.sv.ues.dam235.parcial2.data.local.AppDatabase;
import edu.sv.ues.dam235.parcial2.data.model.Usuario;
import edu.sv.ues.dam235.parcial2.util.RedUtil;
import edu.sv.ues.dam235.parcial2.util.SeguridadUtil;
import edu.sv.ues.dam235.parcial2.util.SesionManager;
import edu.sv.ues.dam235.parcial2.util.Validador;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etCorreo, etPassword;
    private Chip chipRecordar;
    private View btnLogin;
    private SesionManager sesion;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sesion = new SesionManager(this);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        chipRecordar = findViewById(R.id.chipRecordar);
        btnLogin = findViewById(R.id.btnLogin);

        if (sesion.recordado()) {
            etCorreo.setText(sesion.getCorreo());
            etPassword.setText(sesion.getPass());
            chipRecordar.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> iniciarSesion());
        findViewById(R.id.btnRegistrarme).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void iniciarSesion() {
        String correo = etCorreo.getText().toString().trim();
        String pass = etPassword.getText().toString();

        if (!Validador.correoValido(correo)) {
            etCorreo.setError("Correo inválido");
            return;
        }
        if (!Validador.passwordValida(pass)) {
            etPassword.setError("Mínimo 8 caracteres, solo letras y números");
            return;
        }

        btnLogin.setEnabled(false);
        executor.execute(() -> {
            // 1) Intentar contra la base local (funciona sin internet)
            Usuario local = AppDatabase.getInstance(this).usuarioDao().buscarPorCorreo(correo);
            if (local != null && SeguridadUtil.hash(pass, local.salt).equals(local.passwordHash)) {
                runOnUiThread(() -> entrar(local.id, correo, pass));
                return;
            }
            // 2) Si no está o no coincide, intentar contra Firebase
            if (RedUtil.hayInternet(this)) {
                loginRemoto(correo, pass);
            } else {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Credenciales incorrectas (sin internet)",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Usuario que existe en la nube pero aún no en este dispositivo
    private void loginRemoto(String correo, String pass) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, pass)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();
                    FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
                            .addOnSuccessListener(doc -> executor.execute(() -> {
                                Usuario u = new Usuario();
                                u.id = uid;
                                u.correo = correo;
                                u.nombre = doc.getString("nombre");
                                u.salt = SeguridadUtil.generarSalt();
                                u.passwordHash = SeguridadUtil.hash(pass, u.salt);
                                AppDatabase.getInstance(this).usuarioDao().guardar(u);
                                runOnUiThread(() -> entrar(uid, correo, pass));
                            }))
                            .addOnFailureListener(e -> runOnUiThread(() -> {
                                btnLogin.setEnabled(true);
                                Toast.makeText(this, "No se pudo leer el perfil: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }));
                })
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Correo o contraseña incorrectos",
                            Toast.LENGTH_LONG).show();
                }));
    }

    private void entrar(String uid, String correo, String pass) {
        if (chipRecordar.isChecked()) sesion.recordar(correo, pass);
        else sesion.olvidar();
        sesion.setUsuarioActivo(uid);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}